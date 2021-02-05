/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.immutables.value.Value;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public interface SchemaMapping<T extends SchemaBase<T>> {

    T getTargetSchema();

    @Value.Derived
    @Value.Auxiliary
    default Map<List<String>, List<T>> getTargetSchemasByPath() {
        return getTargetSchema().accept(new SchemaToMappingVisitor<>())
                                .asMap()
                                .entrySet()
                                .stream()
                                //TODO: removal of first path element only makes sense for geojson, so change in parser
                                .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey().subList(1, entry.getKey().size()), Lists.newArrayList(entry.getValue())))
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    default List<T> getTargetSchemas(List<String> path) {
        return getTargetSchemasByPath().getOrDefault(path, ImmutableList.of());
    }

    T schemaWithGeometryType(T schema, SimpleFeatureGeometry geometryType);
}
