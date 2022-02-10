/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import java.util.Optional;
import java.util.function.Supplier;
import org.threeten.extra.Interval;

class ExtentReaderSql implements ExtentReader {

    private final Supplier<SqlClient> sqlClient;
    private final FeatureStoreQueryGeneratorSql queryGenerator;
    private final SqlDialect sqlDialect;
    private final EpsgCrs crs;

    ExtentReaderSql(Supplier<SqlClient> sqlClient, FeatureStoreQueryGeneratorSql queryGenerator,
                    SqlDialect sqlDialect, EpsgCrs crs) {
        this.sqlClient = sqlClient;
        this.queryGenerator = queryGenerator;
        this.sqlDialect = sqlDialect;
        this.crs = crs;
    }


    @Override
    public Stream<Optional<BoundingBox>> getExtent(FeatureStoreTypeInfo typeInfo) {
        //TODO: multiple main tables
        FeatureStoreInstanceContainer instanceContainer = typeInfo.getInstanceContainers()
                                                                  .get(0);
        Optional<FeatureStoreAttributesContainer> spatialAttributesContainer = instanceContainer.getSpatialAttributesContainer();

        if (!spatialAttributesContainer.isPresent()) {
            throw new IllegalArgumentException("feature type has no geometry:" + typeInfo.getName());
        }

        String query = queryGenerator.getExtentQuery(instanceContainer, spatialAttributesContainer.get());

        return sqlClient.get().getSourceStream(query, SqlQueryOptions.withColumnTypes(String.class))
            .via(Reactive.Transformer.map(sqlRow -> sqlDialect.parseExtent((String) sqlRow.getValues()
                .get(0), crs)))
            .to(Reactive.Sink.head());
    }

    @Override
    public Stream<Optional<Interval>> getTemporalExtent(FeatureStoreTypeInfo typeInfo,
        String property) {
        FeatureStoreInstanceContainer instanceContainer = typeInfo.getInstanceContainers()
                .get(0);
        Optional<FeatureStoreAttributesContainer> temporalAttributesContainer = instanceContainer.getTemporalAttributesContainer(property);

        if (!temporalAttributesContainer.isPresent()) {
            throw new IllegalArgumentException("temporal property not found:" + property);
        }

        String query = queryGenerator.getTemporalExtentQuery(instanceContainer, temporalAttributesContainer.get(), property);

        return sqlClient.get()
            .getSourceStream(query, SqlQueryOptions.withColumnTypes(String.class, String.class))
            .via(Reactive.Transformer.map(sqlRow -> sqlDialect.parseTemporalExtent((String) sqlRow.getValues().get(0), (String) sqlRow.getValues().get(1))))
            .to(Reactive.Sink.head());
    }

    @Override
    public Stream<Optional<Interval>> getTemporalExtent(FeatureStoreTypeInfo typeInfo,
        String startProperty, String endProperty) {
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

        String query = queryGenerator.getTemporalExtentQuery(instanceContainer, startAttributesContainer.get(), endAttributesContainer.get(), startProperty, endProperty);

        return sqlClient.get()
            .getSourceStream(query, SqlQueryOptions.withColumnTypes(String.class, String.class))
            .via(Reactive.Transformer.map(sqlRow -> sqlDialect.parseTemporalExtent((String) sqlRow.getValues().get(0), (String) sqlRow.getValues().get(1))))
            .to(Reactive.Sink.head());
    }
}
