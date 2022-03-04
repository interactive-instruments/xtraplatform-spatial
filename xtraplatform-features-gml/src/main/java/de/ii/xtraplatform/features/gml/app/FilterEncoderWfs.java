/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.app;

import static de.ii.xtraplatform.cql.domain.In.ID_PLACEHOLDER;

import com.google.common.base.Splitter;
import com.google.common.primitives.Doubles;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.Geometry.Envelope;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.cql.domain.SpatialOperator;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.gml.infra.fes.FesBBox;
import de.ii.xtraplatform.features.gml.infra.fes.FesExpression;
import de.ii.xtraplatform.features.gml.infra.fes.FesResourceId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
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

    //TODO: CqlToFesVisitor
    public FesExpression encode(CqlFilter filter, FeatureSchema schema) {
        if (filter.getInOperator().isPresent()) {
            String ids = filter.getInOperator().get().getList()
                .stream()
                .filter(scalar -> scalar instanceof ScalarLiteral
                    && ((ScalarLiteral) scalar).getType() == String.class)
                .map(scalar -> (String)((ScalarLiteral)scalar).getValue())
                .collect(Collectors.joining(","));

            return new FesResourceId(ids);
        }
        if (filter.getSpatialOperation().isPresent()
            && filter.getSpatialOperation().get().getOperator() == SpatialOperator.S_INTERSECTS
            && filter.getSpatialOperation().get().getOperands().size() == 2
            && filter.getSpatialOperation().get().getOperands().get(0) instanceof Property
            && filter.getSpatialOperation().get().getOperands().get(1) instanceof SpatialLiteral
            && ((SpatialLiteral)filter.getSpatialOperation().get().getOperands().get(1)).getValue() instanceof Geometry.Envelope) {

            String property = ((Property)filter.getSpatialOperation().get().getOperands().get(0)).getName();
            Geometry.Envelope envelope = (Envelope) ((SpatialLiteral)filter.getSpatialOperation().get().getOperands().get(1)).getValue();

            List<Double> coordinates = transformCoordinatesIfNecessary(envelope.getCoordinates(),
                envelope.getCrs());
            BoundingBox boundingBox = BoundingBox.of(coordinates.get(0), coordinates.get(1),
                coordinates.get(2), coordinates.get(3), nativeCrs);

            return new FesBBox(boundingBox, "");
        }
        return null;//cqlFilter.accept(new CqlToSql(schema));
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

    private Optional<String> getPrefixedPropertyName(FeatureSchema schema, String property) {
        return schema.getProperties()
            .stream()
            //TODO: why lower case???
            .filter(featureProperty -> Objects.nonNull(featureProperty.getName()) && Objects.equals(featureProperty.getName()
                .toLowerCase(), property.replaceAll(ID_PLACEHOLDER, "id").toLowerCase()))
            .map(FeatureSchema::getFullPathAsString)
            .map(path -> path.substring(path.indexOf("/", 1)+1))
            .findFirst()
            //.map(namespaceNormalizer::getPrefixedPath)
            .map(prefixedPath -> {
                if (prefixedPath.contains("@")) {
                    return "@" + prefixedPath.replaceAll("@", "");
                }
                return prefixedPath;
            });
    }
}
