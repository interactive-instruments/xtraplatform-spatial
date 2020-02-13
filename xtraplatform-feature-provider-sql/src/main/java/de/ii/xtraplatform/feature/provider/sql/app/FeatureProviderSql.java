package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

//@Component
//@Provides(properties = {@StaticServiceProperty(name = "providerType", type = "java.lang.String", value = FeatureProviderSql.PROVIDER_TYPE)})
@EntityComponent
@Entity(entityType = FeatureProvider2.class, dataType = FeatureProviderDataV1.class, type = "providers", subType = "feature/sql")
public class FeatureProviderSql extends AbstractFeatureProvider<SqlRow, SqlQueries, SqlQueryOptions> implements FeatureProvider2, FeatureQueries, FeatureExtents, FeatureCrs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSql.class);

    private static final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .build());

    //private final ActorSystem system;
    //private final ActorMaterializer materializer;
    private final CrsTransformerFactory crsTransformerFactory;
    private final SqlConnector connector;
    private FeatureStorePathParser pathParser;
    private FeatureStoreQueryGeneratorSql queryGeneratorSql;
    private FeatureQueryTransformerSql queryTransformer;
    private FeatureNormalizerSql featureNormalizer;
    private ExtentReader extentReader;

    public FeatureProviderSql(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Property(name = "data") FeatureProviderDataV1 data,
                              @Property(name = ".connector") SqlConnector sqlConnector) {
        //TODO: starts akka for every instance, move to singleton
        super(context, actorSystemProvider);
        //this.system = actorSystemProvider.getActorSystem(context, config);
        //this.materializer = ActorMaterializer.create(system);
        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = sqlConnector;


    }

    @Override
    public FeatureProviderDataV1 getData() {
        return super.getData();
    }

    @Override
    protected void onStart() {
        if (!getConnector().isConnected()) {
            Optional<Throwable> connectionError = getConnector().getConnectionError();
            String message = connectionError.map(Throwable::getMessage)
                                            .orElse("unknown reason");
            LOGGER.error("Feature provider with id '{}' could not be started: {}", getId(), message);
            if (connectionError.isPresent() && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", connectionError.get());
            }
        } else {
            SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                         .options(((ConnectionInfoSql) getData().getConnectionInfo()).getPathSyntax())
                                                         .build();
            //TODO: merge

            this.pathParser = new FeatureStorePathParserSql(syntax);

            //this.typeInfos = getTypeInfos2(data.getTypes(), ((ConnectionInfoSql)data.getConnectionInfo()).getPathSyntax());
            //TODO: from config
            SqlDialect sqlDialect = new SqlDialectPostGis();
            this.queryGeneratorSql = new FeatureStoreQueryGeneratorSql(new FilterEncoderSqlNewImpl(getData().getNativeCrs()), sqlDialect);
            this.queryTransformer = new FeatureQueryTransformerSql(getTypeInfos(), queryGeneratorSql, false/*TODO data.computeNumberMatched()*/);
            this.featureNormalizer = new FeatureNormalizerSql(getTypeInfos(), getData().getTypes());
            this.extentReader = new ExtentReaderSql(connector, queryGeneratorSql, sqlDialect, getData().getNativeCrs());
        }
    }

    @Override
    protected FeatureStorePathParser getPathParser() {
        return pathParser;
    }

    @Override
    protected FeatureQueryTransformer<SqlQueries> getQueryTransformer() {
        return queryTransformer;
    }

    @Override
    protected FeatureProviderConnector<SqlRow, SqlQueries, SqlQueryOptions> getConnector() {
        return connector;
    }

    @Override
    protected FeatureNormalizer<SqlRow> getNormalizer() {
        return featureNormalizer;
    }

    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        return getData().getNativeCrs()
                        .equals(crs) || crsTransformerFactory.isCrsSupported(crs);
    }

    @Override
    public boolean is3dSupported() {
        return crsTransformerFactory.isCrs3d(getData().getNativeCrs());
    }

    //TODO: from data.getTypes()
    //TODO: move to derived in data?
    /*private Map<String, FeatureStoreTypeInfo> getTypeInfos(Map<String, FeatureTypeMapping> mappings) {
        //TODO: options from data
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .build();
        SqlMappingParser mappingParser = new SqlMappingParser(syntax);
        FeatureStorePathParser pathParser = new FeatureStorePathParserSql(syntax);

        return mappings.entrySet()
                       .stream()
                       .map(entry -> {
                           List<String> paths = mappingParser.parse(entry.getValue()
                                                                         .getMappings());
                           List<FeatureStoreInstanceContainer> instanceContainers = pathParser.parse(paths);
                           FeatureStoreTypeInfo typeInfo = ImmutableFeatureStoreTypeInfo.builder()
                                                                                        .name(entry.getKey())
                                                                                        .instanceContainers(instanceContainers)
                                                                                        .build();

                           return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), typeInfo);
                       })
                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }*/

    /*private Map<String, FeatureStoreTypeInfo> getTypeInfos2(Map<String, FeatureType> featureTypes, SqlPathSyntax.Options syntaxOptions) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(syntaxOptions)
                                                     .build();
        SqlFeatureTypeParser mappingParser = new SqlFeatureTypeParser(syntax);
        FeatureStorePathParser pathParser = new FeatureStorePathParserSql(syntax);

        return featureTypes.entrySet()
                           .stream()
                           .map(entry -> {
                               List<String> paths = mappingParser.parse(entry.getValue());
                               List<FeatureStoreInstanceContainer> instanceContainers = pathParser.parse(paths);
                               FeatureStoreTypeInfo typeInfo = ImmutableFeatureStoreTypeInfo.builder()
                                                                                            .name(entry.getKey())
                                                                                            .instanceContainers(instanceContainers)
                                                                                            .build();

                               return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), typeInfo);
                           })
                           .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }*/

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName) {
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

        if (!typeInfo.isPresent()) {
            return Optional.empty();
        }

        return extentReader.getExtent(typeInfo.get())
                           .run(getMaterializer())
                           .exceptionally(throwable -> Optional.empty())
                           .toCompletableFuture()
                           .join();
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs) {
        return getSpatialExtent(typeName).flatMap(boundingBox -> crsTransformerFactory.getTransformer(getData().getNativeCrs(), crs)
                                                                                      .flatMap(crsTransformer -> {
                                                                                          try {
                                                                                              return Optional.of(crsTransformer.transformBoundingBox(boundingBox));
                                                                                          } catch (CrsTransformationException e) {
                                                                                              return Optional.empty();
                                                                                          }
                                                                                      }));
    }
}
