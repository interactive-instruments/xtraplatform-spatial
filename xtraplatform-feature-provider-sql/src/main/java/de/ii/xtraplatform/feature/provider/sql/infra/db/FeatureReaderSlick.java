package de.ii.xtraplatform.feature.provider.sql.infra.db;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.slick.javadsl.Slick;
import akka.stream.alpakka.slick.javadsl.SlickRow;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Source;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureReaderSql;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableMetaQueryResult;
import de.ii.xtraplatform.feature.provider.sql.domain.MetaQueryResult;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRowValues;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.reflect.ClassTag$;
import slick.basic.DatabaseConfig;
import slick.basic.DatabaseConfig$;
import slick.jdbc.JdbcProfile;

import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

@Component
@Provides
public class FeatureReaderSlick implements FeatureReaderSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureReaderSlick.class);

    private SlickSession session;
    private final FeatureStoreQueryGeneratorSql queryGenerator;

    public FeatureReaderSlick(FeatureStoreQueryGeneratorSql queryGenerator) {
        this.queryGenerator = queryGenerator;


    }

    @Validate
    private void onStart(@Context BundleContext context) {
        try {
            // bundle class loader has to be passed to Slick for initialization
            ClassLoader classLoader = context.getBundle()
                                             .adapt(BundleWiring.class)
                                             .getClassLoader();
            Thread.currentThread()
                  .setContextClassLoader(classLoader);
            DatabaseConfig<JdbcProfile> databaseConfig = DatabaseConfig$.MODULE$.forConfig("", createSlickConfig((ConnectionInfoSql) data.getConnectionInfo()), classLoader, ClassTag$.MODULE$.apply(JdbcProfile.class));
            this.session = SlickSession.forConfig(databaseConfig);
        } catch (Throwable e) {
            //TODO
            LOGGER.error("CONNECTING TO DB FAILED", e);
        }
    }

    @Invalidate
    private void onStop() {
        if (Objects.nonNull(session)) {
            session.close();
        }
    }


    //TODO: reuse instances of SqlRow, SqlColumn
    @Override
    public Source<SqlRow, NotUsed> getRowStream(FeatureQuery featureQuery, FeatureStoreTypeInfo typeInfo,
                                                boolean computeNumberMatched) {
        //TODO
        FeatureStoreInstanceContainer mainTable = typeInfo.getInstanceContainers()
                                                          .get(0);
        List<FeatureStoreAttributesContainer> attributesContainers = mainTable.getAllAttributesContainers();

        Optional<String> metaQuery = isIdFilter(featureQuery.getFilter())
                ? Optional.empty()
                : Optional.of(queryGenerator.getMetaQuery(mainTable, featureQuery.getLimit(), featureQuery.getOffset(), featureQuery.getFilter(), computeNumberMatched));

        Source<SqlRow, NotUsed> sqlRowSource = getMetaRow(metaQuery).flatMapConcat(metaQueryResult -> {

            //TODO: exception when only one table???
            int[] i = {0};
            Source<SqlRow, NotUsed>[] sqlRows = queryGenerator.getInstanceQueries(mainTable, featureQuery.getFilter(), metaQueryResult.getMinKey(), metaQueryResult.getMaxKey())
                                                              .map(query -> Slick.source(session, query, toSlickRowInfo(attributesContainers.get(i[0]), i[0]++)))
                                                              .toArray((IntFunction<Source<SqlRow, NotUsed>[]>) Source[]::new);
            return mergeAndSort(sqlRows);
        });

        return sqlRowSource;
    }

    private Source<MetaQueryResult, NotUsed> getMetaRow(Optional<String> metaQuery) {
        if (!metaQuery.isPresent()) {
            return Source.single(getMetaQueryResult(0, 0, 0, 0));
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("META QUERY: {}", metaQuery.get());
        }

        return Slick.source(session, metaQuery.get(), slickRow -> getMetaQueryResult(slickRow.nextLong(), slickRow.nextLong(), slickRow.nextLong(), slickRow.nextLong()))
                    .recover(new PFBuilder<Throwable, MetaQueryResult>().matchAny(throwable -> getMetaQueryResult(0, 0, 0, 0))
                                                                        .build());
    }

    //TODO
    private MetaQueryResult getMetaQueryResult(long minKey, long maxKey, long numberReturned, long numberMatched) {
        return ImmutableMetaQueryResult.builder()
                                       .minKey(minKey)
                                       .maxKey(maxKey)
                                       .numberReturned(numberReturned)
                                       .numberMatched(numberMatched)
                                       .build();
    }

    private Function<SlickRow, SqlRow> toSlickRowInfo(FeatureStoreAttributesContainer attributesContainer,
                                                      int priority) {
        return slickRow -> new SqlRowValues(slickRow, attributesContainer, priority);
    }


    static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed>... sources) {
        return mergeAndSort(sources[0], sources[1], Arrays.asList(sources)
                                                          .subList(2, sources.length));
    }

    static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed> source1,
                                                                     Source<T, NotUsed> source2,
                                                                     Iterable<Source<T, NotUsed>> rest) {
        Comparator<T> comparator = Comparator.naturalOrder();
        Source<T, NotUsed> mergedAndSorted = source1.mergeSorted(source2, comparator);
        for (Source<T, NotUsed> source3 : rest) {
            mergedAndSorted = mergedAndSorted.mergeSorted(source3, comparator);
        }
        return mergedAndSorted;
    }

    //TODO: to FeatureQuery?
    private boolean isIdFilter(String filter) {
        return Strings.nullToEmpty(filter)
                      .startsWith("IN ('");// TODO: matcher
    }

    private static Config createSlickConfig(ConnectionInfoSql connectionInfo) {
        return ConfigFactory.parseMap(ImmutableMap.<String, Object>builder()
                .put("profile", getProfile(connectionInfo))
                .put("db", ImmutableMap.<String, Object>builder()
                        .put("user", connectionInfo.getUser())
                        .put("password", getPassword(connectionInfo))
                        .put("dataSourceClass", getDataSourceClass(connectionInfo))
                        .put("properties.serverName", connectionInfo.getHost())
                        .put("properties.databaseName", connectionInfo.getDatabase())
                        .put("numThreads", connectionInfo.getMaxThreads())
                        .put("initializationFailFast", true)
                        .build())
                .build());
    }

    private static String getPassword(ConnectionInfoSql connectionInfo) {
        String password = connectionInfo.getPassword();
        try {
            password = new String(Base64.getDecoder()
                                        .decode(password), Charsets.UTF_8);
        } catch (IllegalArgumentException e) {
            //ignore if not valid base64
        }

        return password;
    }

    private static String getProfile(ConnectionInfoSql connectionInfo) {
        String profile;

        switch (connectionInfo.getDialect()) {
            case PGIS:
            default:
                profile = "slick.jdbc.PostgresProfile$";
                break;
        }

        return profile;
    }

    private static String getDataSourceClass(ConnectionInfoSql connectionInfo) {
        String dataSourceClass;

        switch (connectionInfo.getDialect()) {
            case PGIS:
            default:
                dataSourceClass = "org.postgresql.ds.PGSimpleDataSource";
                break;
        }

        return dataSourceClass;
    }
}
