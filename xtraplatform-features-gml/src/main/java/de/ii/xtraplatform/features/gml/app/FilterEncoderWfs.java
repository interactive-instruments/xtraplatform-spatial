/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.app;

import com.google.common.base.Splitter;
import com.google.common.primitives.Doubles;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterEncoderWfs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEncoderWfs.class);

    static final String ROW_NUMBER = "row_number";
    static final Splitter ARRAY_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

    private final EpsgCrs nativeCrs;
    private final CrsTransformerFactory crsTransformerFactory;
    private final Cql cql;
    BiFunction<List<Double>, Optional<EpsgCrs>, List<Double>> coordinatesTransformer;

    public FilterEncoderWfs(
            EpsgCrs nativeCrs,
            CrsTransformerFactory crsTransformerFactory,
            Cql cql) {
        this.nativeCrs = nativeCrs;
        this.crsTransformerFactory = crsTransformerFactory;
        this.cql = cql;
        this.coordinatesTransformer = this::transformCoordinatesIfNecessary;
    }

    public CqlFilter encode(CqlFilter cqlFilter, FeatureSchema schema) {
        return cqlFilter;//cqlFilter.accept(new CqlToSql(schema));
    }

    private List<Double> transformCoordinatesIfNecessary(List<Double> coordinates, Optional<EpsgCrs> sourceCrs) {

        if (sourceCrs.isPresent() && !Objects.equals(sourceCrs.get(), nativeCrs)) {
            Optional<CrsTransformer> transformer = crsTransformerFactory.getTransformer(sourceCrs.get(), nativeCrs, true);
            if (transformer.isPresent()) {
                double[] transformed = transformer.get()
                                                  .transform(Doubles.toArray(coordinates), coordinates.size() / 2,
                                                      2);
                if (Objects.isNull(transformed)) {
                    throw new IllegalArgumentException(String.format("Filter is invalid. Coordinates cannot be transformed: %s", coordinates));
                }

                return Doubles.asList(transformed);
            }
        }
        return coordinates;
    }
}
