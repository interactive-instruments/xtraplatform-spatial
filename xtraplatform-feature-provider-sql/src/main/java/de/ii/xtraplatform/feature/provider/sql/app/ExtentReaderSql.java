/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.streams.domain.LogContextStream;
import de.ii.xtraplatform.streams.domain.RunnableGraphWithMdc;
import org.threeten.extra.Interval;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

class ExtentReaderSql implements ExtentReader {

    private final SqlConnector sqlConnector;
    private final FeatureStoreQueryGeneratorSql queryGenerator;
    private final SqlDialect sqlDialect;
    private final EpsgCrs crs;

    ExtentReaderSql(SqlConnector sqlConnector, FeatureStoreQueryGeneratorSql queryGenerator,
                    SqlDialect sqlDialect, EpsgCrs crs) {
        this.sqlConnector = sqlConnector;
        this.queryGenerator = queryGenerator;
        this.sqlDialect = sqlDialect;
        this.crs = crs;
    }


    @Override
    public RunnableGraphWithMdc<CompletionStage<Optional<BoundingBox>>> getExtent(FeatureStoreTypeInfo typeInfo) {
        //TODO: multiple main tables
        FeatureStoreInstanceContainer instanceContainer = typeInfo.getInstanceContainers()
                                                                  .get(0);
        Optional<FeatureStoreAttributesContainer> spatialAttributesContainer = instanceContainer.getSpatialAttributesContainer();

        if (!spatialAttributesContainer.isPresent()) {
            throw new IllegalArgumentException("feature type has no geometry:" + typeInfo.getName());
        }

        String query = queryGenerator.getExtentQuery(spatialAttributesContainer.get());

        Source<SqlRow, NotUsed> sourceStream = sqlConnector.getSqlClient().getSourceStream(query, SqlQueryOptions.withColumnTypes(String.class));

        return LogContextStream.graphWithMdc(sourceStream.map(sqlRow -> sqlDialect.parseExtent((String) sqlRow.getValues()
                                                                                                              .get(0), crs)), Sink.head());
    }

    public RunnableGraphWithMdc<CompletionStage<Optional<Interval>>> getTemporalExtent(FeatureStoreTypeInfo typeInfo, String property) {
        FeatureStoreInstanceContainer instanceContainer = typeInfo.getInstanceContainers()
                .get(0);
        Optional<FeatureStoreAttributesContainer> temporalAttributesContainer = instanceContainer.getTemporalAttributesContainer(property);

        if (!temporalAttributesContainer.isPresent()) {
            throw new IllegalArgumentException("temporal property not found:" + property);
        }

        String query = queryGenerator.getTemporalExtentQuery(temporalAttributesContainer.get(), property);

        Source<SqlRow, NotUsed> sourceStream = sqlConnector.getSqlClient().getSourceStream(query, SqlQueryOptions.withColumnTypes(String.class, String.class));

        return LogContextStream.graphWithMdc(sourceStream.map(sqlRow -> sqlDialect.parseTemporalExtent((String) sqlRow.getValues().get(0), (String) sqlRow.getValues().get(1))), Sink.head());
    }

    public RunnableGraphWithMdc<CompletionStage<Optional<Interval>>> getTemporalExtent(FeatureStoreTypeInfo typeInfo, String startProperty, String endProperty) {
        FeatureStoreInstanceContainer instanceContainer = typeInfo.getInstanceContainers()
                .get(0);
        Optional<FeatureStoreAttributesContainer> startAttributesContainer = instanceContainer.getTemporalAttributesContainer(startProperty);
        if (!startAttributesContainer.isPresent()) {
            throw new IllegalArgumentException("temporal property not found:" + startProperty);
        }
        Optional<FeatureStoreAttributesContainer> endAttributesContainer = instanceContainer.getTemporalAttributesContainer(endProperty);
        if (!endAttributesContainer.isPresent()) {
            throw new IllegalArgumentException("temporal property not found:" + endProperty);
        }

        String query = queryGenerator.getTemporalExtentQuery(startAttributesContainer.get(), endAttributesContainer.get(), startProperty, endProperty);

        Source<SqlRow, NotUsed> sourceStream = sqlConnector.getSqlClient().getSourceStream(query, SqlQueryOptions.withColumnTypes(String.class, String.class));

        return LogContextStream.graphWithMdc(sourceStream.map(sqlRow -> sqlDialect.parseTemporalExtent((String) sqlRow.getValues().get(0), (String) sqlRow.getValues().get(1))), Sink.head());
    }
}
