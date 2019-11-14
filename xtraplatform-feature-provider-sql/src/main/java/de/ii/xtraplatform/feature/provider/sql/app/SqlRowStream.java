/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Function;
import akka.japi.function.Function2;
import akka.japi.function.Procedure;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureStream2;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreMultiplicityTracker;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreRelation;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRowMeta;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRowValues;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletionStage;

/**
 * @author zahnen
 */
public class SqlRowStream {

    //TODO: query only needed for IdFilter exceptions, should happen further up
    public static Sink<SqlRow, CompletionStage<FeatureStream2.Result>> consume(FeatureStoreTypeInfo typeInfo,
                                                                               FeatureConsumer consumer,
                                                                               FeatureQuery query) {
        return consume(ImmutableList.of(typeInfo), consumer, query);
    }

    public static Sink<SqlRow, CompletionStage<FeatureStream2.Result>> consume(
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
        String getCurrentId();

        boolean isAtLeastOneFeatureWritten();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlRowStream.class);

    private static void handleRow(SqlRow sqlRow, ReadContext readContext) throws Exception {

        if (sqlRow instanceof SqlRowMeta) {
            handleMetaRow(sqlRow, readContext);
            return;
        }

        //TODO: should't happen, test with exception in onStart
        if (!readContext.getReadState()
                        .isStarted()) {
            return;
        }

        handleValueRow(sqlRow, readContext);
    }

    private static void handleMetaRow(SqlRow sqlRow, ReadContext readContext) throws Exception {
        Optional<String> count = sqlRow.next()
                                       .map(SqlRow.SqlColumn::getValue);

        OptionalLong numberReturned = count.map(s -> OptionalLong.of(Long.parseLong(s)))
                                           .orElseGet(OptionalLong::empty);

        Optional<String> count2 = sqlRow.next()
                                        .map(SqlRow.SqlColumn::getValue);
        OptionalLong numberMatched = count2.isPresent() && !count2.get()
                                                                  .equals("-1") ? OptionalLong.of(Long.parseLong(count2.get())) : OptionalLong.empty();

        readContext.getFeatureConsumer()
                   .onStart(numberReturned, numberMatched);

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
        String featureId = sqlRow.getIds()
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
        if (sqlRow instanceof SqlRowValues) {
            readState.setIsAtLeastOneFeatureWritten(true);
        }

        if (!Objects.equals(readState.getCurrentId(), featureId)) {
            if (readState.isFeatureStarted()) {
                consumer.onFeatureEnd(readContext.getMainTablePath());
                readState.setIsFeatureStarted(false);
                multiplicityTracker.reset();
            }

            consumer.onFeatureStart(readContext.getMainTablePath());
            readState.setIsFeatureStarted(true);
            readState.setCurrentId(featureId);
        }

        handleColumns(sqlRow, consumer, multiplicityTracker.getMultiplicitiesForPath(sqlRow.getPath()));
    }

    private static void handleColumns(SqlRow sqlRow, FeatureConsumer consumer,
                                      List<Integer> multiplicities) throws Exception {
        Optional<SqlRow.SqlColumn> column = sqlRow.next();

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
        }
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
                           .onStart(OptionalLong.of(0), OptionalLong.empty());
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
