package de.ii.xtraplatform.feature.provider.sql.app;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Function;
import akka.japi.function.Function2;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.feature.provider.api.Feature;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureNormalizer;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureStream2;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer2;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureType;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreMultiplicityTracker;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRowMeta;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletionStage;

public class FeatureNormalizerSql implements FeatureNormalizer<SqlRow> {

    private final Map<String, FeatureStoreTypeInfo> typeInfos;
    //private final Map<String, FeatureTypeMapping> mappings;
    private final Map<String, FeatureType> types;

    /*public FeatureNormalizerSql(
            Map<String, FeatureStoreTypeInfo> typeInfos,
            Map<String, FeatureTypeMapping> mappings) {
        this.typeInfos = typeInfos;
        this.mappings = mappings;
    }*/

    public FeatureNormalizerSql(Map<String, FeatureStoreTypeInfo> typeInfos, Map<String, FeatureType> types) {
        this.typeInfos = typeInfos;
        this.types = types;
    }

    @Override
    public Sink<SqlRow, CompletionStage<FeatureStream2.Result>> normalizeAndTransform(
            FeatureTransformer2 featureTransformer, FeatureQuery featureQuery) {

        FeatureStoreTypeInfo typeInfo = typeInfos.get(featureQuery.getType());
        //FeatureTypeMapping mapping = mappings.get(featureQuery.getType());
        FeatureType featureType = types.get(featureQuery.getType());

        //TODO: consumer to transformer is generic code, but this also contains WKT parser, factor out
        //FeatureTransformerFromSql consumer = new FeatureTransformerFromSql(mapping, featureTransformer, featureQuery.getFields());
        FeatureTransformerFromSql2 consumer = new FeatureTransformerFromSql2(featureType, featureTransformer, featureQuery.getFields());


        return consume(typeInfo, consumer, featureQuery);
    }

    @Override
    public Source<Feature, NotUsed> normalize(Source<SqlRow, NotUsed> sourceStream) {
        return null;
    }



    //TODO: query only needed for IdFilter exceptions, should happen further up
    private static Sink<SqlRow, CompletionStage<FeatureStream2.Result>> consume(FeatureStoreTypeInfo typeInfo,
                                                                               FeatureConsumer consumer,
                                                                               FeatureQuery query) {
        return consume(ImmutableList.of(typeInfo), consumer, query);
    }

    private static Sink<SqlRow, CompletionStage<FeatureStream2.Result>> consume(
            final List<FeatureStoreTypeInfo> typeInfos,
            final FeatureConsumer consumer, FeatureQuery query) {

        //TODO: support multiple typeInfos
        FeatureStoreTypeInfo typeInfo = typeInfos.get(0);
        //TODO: support multiple main tables
        FeatureStoreInstanceContainer instanceContainer = typeInfo.getInstanceContainers().get(0);
        List<String> multiTables = instanceContainer.getMultiContainerNames();

        FeatureStoreMultiplicityTracker multiplicityTracker = new SqlMultiplicityTracker(multiTables);//getMultiplicityTracker(typeInfo)

        ReadContext readContext = ImmutableReadContext.builder()
                                                                   .featureConsumer(consumer)
                                                                   .mainTablePath(typeInfo.getInstanceContainers()
                                                                                          .get(0)
                                                                                          .getPath())
                                                                   .multiTables(multiTables)
                                                                   .multiplicityTracker(multiplicityTracker)
                                                                   .isIdFilter(query.hasIdFilter())
                                                                   .readState(ModifiableReadState.create())
                                                                   .build();

        //TODO: cleanup
        Flow<SqlRow, NotUsed, CompletionStage<FeatureStream2.Result>> consumerFlow =
                Flow.fromFunction((Function<SqlRow, NotUsed>) sqlRow -> {
                    handleRow(sqlRow, readContext);
                    return NotUsed.getInstance();
                })
                    .watchTermination((Function2<NotUsed, CompletionStage<Done>, CompletionStage<FeatureStream2.Result>>) (notUsed, completionStage) -> completionStage.handle((done, throwable) -> {
                        boolean success = true;
                        if (Objects.nonNull(throwable)) {
                            handleException(throwable, readContext);
                            success = false;
                        }

                        handleCompletion(readContext);
                        boolean finalSuccess = success;
                        return () -> finalSuccess;
                    }));

        return consumerFlow.to(Sink.ignore());
    }

    /*public static Sink<ByteString, CompletionStage<Done>> transform(final QName featureType,
                                                                    final FeatureTypeMapping featureTypeMapping,
                                                                    final FeatureTransformer featureTransformer,
                                                                    List<String> fields) {
        return transform(featureType, featureTypeMapping, featureTransformer, fields, ImmutableMap.of());
    }

    public static Sink<ByteString, CompletionStage<Done>> transform(final QName featureType,
                                                                    final FeatureTypeMapping featureTypeMapping,
                                                                    final FeatureTransformer featureTransformer,
                                                                    List<String> fields,
                                                                    Map<QName, List<String>> resolvableTypes) {
        List<QName> featureTypes = resolvableTypes.isEmpty() ? ImmutableList.of(featureType) : ImmutableList.<QName>builder().add(featureType)
                                                                                                                             .addAll(resolvableTypes.keySet())
                                                                                                                             .build();
        return SqlRowStream.consume(featureTypes, new FeatureTransformerFromGml(featureTypeMapping, featureTransformer, fields, resolvableTypes));
    }*/

    @Value.Immutable
    interface ReadContext {
        FeatureConsumer getFeatureConsumer();

        List<String> getMainTablePath();

        List<String> getMultiTables();

        FeatureStoreMultiplicityTracker getMultiplicityTracker();

        boolean isIdFilter();

        ModifiableReadState getReadState();
    }

    @Value.Modifiable
    interface ReadState {
        @Value.Default
        default boolean isStarted() {
            return false;
        }

        @Value.Default
        default boolean isFeatureStarted() {
            return false;
        }

        @Nullable
        Long getCurrentId();

        boolean isAtLeastOneFeatureWritten();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureNormalizerSql.class);

    private static void handleRow(SqlRow sqlRow, ReadContext readContext) throws Exception {

        if (sqlRow instanceof SqlRowMeta) {
            handleMetaRow((SqlRowMeta)sqlRow, readContext);
            return;
        }

        //TODO: should't happen, test with exception in onStart
        if (!readContext.getReadState()
                        .isStarted()) {
            return;
        }

        //if (sqlRow instanceof SqlRowValues) {
        handleValueRow(sqlRow, readContext);
        //    return;
        //}

    }

    private static void handleMetaRow(SqlRowMeta sqlRow, ReadContext readContext) throws Exception {

        readContext.getFeatureConsumer()
                   .onStart(OptionalLong.of(sqlRow.getNumberReturned()), sqlRow.getNumberMatched(), ImmutableMap.of());

        readContext.getReadState()
                   .setIsStarted(true);
    }

    private static void handleValueRow(SqlRow sqlRow, ReadContext readContext) throws Exception {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sql row: {}", sqlRow);
        }

        FeatureConsumer consumer = readContext.getFeatureConsumer();
        ModifiableReadState readState = readContext.getReadState();
        FeatureStoreMultiplicityTracker multiplicityTracker = readContext.getMultiplicityTracker();
        Long featureId = sqlRow.getIds()
                               .get(0);

        //TODO: tracker knows multitables, move there
        //TODO: tables may occur more than once in path, so we need multiPaths instead of multiTables
        if (readContext.getMultiTables()
                       .contains(sqlRow.getName())) {
            multiplicityTracker.track(sqlRow.getPath(), sqlRow.getIds());
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Multiplicities {} {}", sqlRow.getPath(), multiplicityTracker.getMultiplicitiesForPath(sqlRow.getPath()));
        }

        //TODO: isn't that too early?
        if (!(sqlRow instanceof SqlRowMeta)) {
            readState.setIsAtLeastOneFeatureWritten(true);
        }

        if (!Objects.equals(readState.getCurrentId(), featureId)) {
            if (readState.isFeatureStarted()) {
                consumer.onFeatureEnd(readContext.getMainTablePath());
                readState.setIsFeatureStarted(false);
                multiplicityTracker.reset();
            }

            consumer.onFeatureStart(readContext.getMainTablePath(), ImmutableMap.of());
            readState.setIsFeatureStarted(true);
            readState.setCurrentId(featureId);
        }

        handleColumns(sqlRow, consumer, multiplicityTracker.getMultiplicitiesForPath(sqlRow.getPath()));
    }

    private static void handleColumns(SqlRow sqlRow, FeatureConsumer consumer,
                                      List<Integer> multiplicities) throws Exception {

        for (int i = 0; i < sqlRow.getValues().size() && i < sqlRow.getColumnPaths().size(); i++) {
            if (Objects.nonNull(sqlRow.getValues()
                                      .get(i))) {
                consumer.onPropertyStart(sqlRow.getColumnPaths()
                                               .get(i), multiplicities, ImmutableMap.of());
                consumer.onPropertyText((String) sqlRow.getValues()
                                                       .get(i));
                consumer.onPropertyEnd(sqlRow.getColumnPaths()
                                             .get(i));
            }
        }

        /*Optional<SqlRow.SqlColumn> column = sqlRow.next();

        while (column.isPresent()) {
            if (Objects.nonNull(column.get()
                                      .getValue())) {
                consumer.onPropertyStart(column.get()
                                               .getPath(), multiplicities);
                consumer.onPropertyText(column.get()
                                              .getValue());
                consumer.onPropertyEnd(column.get()
                                             .getPath());
            }
            column = sqlRow.next();
        }*/
    }

    private static Done handleException(Throwable throwable, ReadContext readContext) {
        if (throwable instanceof WebApplicationException) {
            throw (WebApplicationException) throwable;
        }
        LOGGER.error(throwable.getMessage());
        LOGGER.debug("STREAM FAILURE", throwable);

        //TODO: return featureFound in Result, handle in getItemResponse
        if (readContext.isIdFilter()) {
            throw new InternalServerErrorException();
        }

        try {
            if (!readContext.getReadState()
                            .isStarted()) {
                readContext.getFeatureConsumer()
                           .onStart(OptionalLong.of(0), OptionalLong.empty(), ImmutableMap.of());
            }
        } catch (Exception e) {
            //ignore
        }
        return Done.getInstance();
    }

    private static void handleCompletion(ReadContext readContext) {

        //TODO: return featureFound in Result, handle in getItemResponse
        if (readContext.isIdFilter() && !readContext.getReadState()
                                                    .isAtLeastOneFeatureWritten()) {
            throw new NotFoundException();
        }

        FeatureConsumer consumer = readContext.getFeatureConsumer();

        try {
            if (readContext.getReadState()
                           .isFeatureStarted()) {
                consumer.onFeatureEnd(readContext.getMainTablePath());
            }
            consumer.onEnd();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
