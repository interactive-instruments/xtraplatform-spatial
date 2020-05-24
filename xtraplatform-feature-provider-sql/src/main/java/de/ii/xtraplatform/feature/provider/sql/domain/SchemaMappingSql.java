/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
public interface SchemaMappingSql extends SchemaMapping<SchemaSql> {

    @Override
    default SchemaSql schemaWithGeometryType(SchemaSql schema, SimpleFeatureGeometry geometryType) {
        return new ImmutableSchemaSql.Builder().from(schema)
                                               .geometryType(geometryType)
                                               .build();
    }
}
