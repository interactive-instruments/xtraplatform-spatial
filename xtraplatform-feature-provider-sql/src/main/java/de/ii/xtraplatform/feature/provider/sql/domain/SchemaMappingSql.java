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
