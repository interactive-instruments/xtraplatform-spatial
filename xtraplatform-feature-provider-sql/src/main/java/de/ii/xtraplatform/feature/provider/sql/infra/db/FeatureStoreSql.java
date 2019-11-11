package de.ii.xtraplatform.feature.provider.sql.infra.db;

import akka.Done;
import akka.stream.ActorMaterializer;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureReaderSql;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStore;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreMultiplicityTracker;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow.SqlColumn;
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

public abstract class FeatureStoreSql implements FeatureStore {

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
        //TODO: same? merge?
        boolean isMetaRow();
        boolean isStarted();

        boolean isFeatureStarted();

        @Nullable
        String getCurrentId();

        boolean isAtLeastOneFeatureWritten();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStoreSql.class);

    private final FeatureReaderSql featureReader;
    private final ActorMaterializer materializer;

    protected FeatureStoreSql(FeatureReaderSql featureReader, ActorMaterializer materializer) {
        this.featureReader = featureReader;
        this.materializer = materializer;
    }

    public CompletionStage<Done> streamFeatures(FeatureQuery query, FeatureConsumer consumer,
                                                FeatureStoreTypeInfo typeInfo) {

        ReadContext readContext = ImmutableReadContext.builder()
                                                      .featureConsumer(consumer)
                                                      .mainTablePath(typeInfo.getInstanceContainers()
                                                                                              .get(0)
                                                                                              .getPath())
                                                      .multiTables(getMultiTables(typeInfo.getInstanceContainers()
                                                                                          .get(0)))
                                                      .multiplicityTracker(getMultiplicityTracker(typeInfo))
                                                      .isIdFilter(isIdFilter(query.getFilter()))
                                                      .readState(ModifiableReadState.create())
                                                      .build();

        return featureReader.getRowStream(query, typeInfo)
                            .runForeach(sqlRow -> handleRow(sqlRow, readContext), materializer)
                            .exceptionally(throwable -> handleException(throwable, readContext))
                            .whenComplete((done, throwable) -> handleCompletion(readContext));
    }

    private void handleRow(SqlRow sqlRow, ReadContext readContext) throws Exception {
        FeatureConsumer consumer = readContext.getFeatureConsumer();
        List<String> mainTablePath = readContext.getMainTablePath();
        List<String> multiTables = readContext.getMultiTables();
        FeatureStoreMultiplicityTracker multiplicityTracker = readContext.getMultiplicityTracker();
        ModifiableReadState readState = readContext.getReadState();

        if (sqlRow.getPath()
                  .size() >= 3 && multiTables.contains(sqlRow.getName())) {
            multiplicityTracker.track(sqlRow.getPath(), sqlRow.getIds());
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("MULTI2 {}", multiplicityTracker.getMultiplicitiesForPath(sqlRow.getPath()));
        }

        //TODO: isn't that too early
        if (sqlRow instanceof SqlRowValues) {
            readState.setIsAtLeastOneFeatureWritten(true);
        }

        if (sqlRow instanceof SqlRowMeta) {
            handleMetaRow(sqlRow, consumer, readState);
        } else if (readState.isStarted() && readState.isFeatureStarted() && !Objects.equals(sqlRow.getIds()
                                                                    .get(0), readState.getCurrentId())) {
            consumer.onFeatureEnd(mainTablePath);
            readState.setIsFeatureStarted(false);
            multiplicityTracker.reset();
        } else if (!readState.isStarted()) {
            readState.setIsStarted(true);
        }

        //TODO: do these ever differ?
        LOGGER.debug("STARTED: {} META: {}", readState.isStarted(), readState.isMetaRow());

        if (!Objects.equals(sqlRow.getIds()
                                  .get(0), readState.getCurrentId())) {
            consumer.onFeatureStart(mainTablePath);
            readState.setIsFeatureStarted(true);
        }

        handleColumns(sqlRow, consumer, multiplicityTracker.getMultiplicitiesForPath(sqlRow.getPath()));

        readState.setCurrentId(sqlRow.getIds()
                             .get(0));
    }

    private void handleMetaRow(SqlRow sqlRow, FeatureConsumer consumer, ModifiableReadState readState) throws Exception {
        Optional<String> count = sqlRow.next()
                                       .map(SqlColumn::getValue);

        OptionalLong numberReturned = count.map(s -> OptionalLong.of(Long.parseLong(s)))
                                           .orElseGet(OptionalLong::empty);

        Optional<String> count2 = sqlRow.next()
                                        .map(SqlColumn::getValue);
        OptionalLong numberMatched = count2.isPresent() && !count2.get()
                                                                  .equals("-1") ? OptionalLong.of(Long.parseLong(count2.get())) : OptionalLong.empty();

        consumer.onStart(numberReturned, numberMatched);

        readState.setIsMetaRow(true);
    }

    private void handleColumns(SqlRow sqlRow, FeatureConsumer consumer, List<Integer> multiplicities) throws Exception {
        Optional<SqlColumn> column = sqlRow.next();

        while (column.isPresent()) {
            if (Objects.nonNull(column.get().getValue())) {
                consumer.onPropertyStart(column.get().getPath(), multiplicities);
                consumer.onPropertyText(column.get().getValue());
                consumer.onPropertyEnd(column.get().getPath());
            }
            column = sqlRow.next();
        }
    }

    private Done handleException(Throwable throwable, ReadContext readContext) {
        if (throwable instanceof WebApplicationException) {
            throw (WebApplicationException) throwable;
        }
        LOGGER.error(throwable.getMessage());
        LOGGER.debug("STREAM FAILURE", throwable);

        if (readContext.isIdFilter()) {
            throw new InternalServerErrorException();
        }

        try {
            if (!readContext.getReadState().isMetaRow()) {
                readContext.getFeatureConsumer().onStart(OptionalLong.of(0), OptionalLong.empty());
            }
        } catch (Exception e) {
            //ignore
        }
        return Done.getInstance();
    }

    private void handleCompletion(ReadContext readContext) {
        if (readContext.isIdFilter() && !readContext.getReadState().isAtLeastOneFeatureWritten()) {
            throw new NotFoundException();
        }

        FeatureConsumer consumer = readContext.getFeatureConsumer();

        try {
            if (readContext.getReadState().isFeatureStarted()) {
                consumer.onFeatureEnd(readContext.getMainTablePath());
            }
            consumer.onEnd();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: [zcodebt, inspirebiotopehabitate, habitattypecovertype, pflanzengesellschaft, ortsangaben, foto, raumreferenz_2_fachreferenz, erfassung, pflanzebt, erhaltungszustandsbewertung, biotoptyp, schichtbt, raumreferenz, ortsangaben_flurstueckskennzeichen, biotop_2_btkomplex]
    private List<String> getMultiTables(FeatureStoreInstanceContainer instanceContainer) {

        return ImmutableList.of();
    }

    //TODO: to FeatureQuery?
    private boolean isIdFilter(String filter) {
        return Strings.nullToEmpty(filter)
                      .startsWith("IN ('");// TODO: matcher
    }
}
