package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.app.FeatureProviderDataMigrationV1V2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FeatureSchemaNamePathSwapper {

    static final FeatureProviderDataMigrationV1V2 migration = new FeatureProviderDataMigrationV1V2();

    public static FeatureSchema migrate(FeatureType featureType) {

        return migration.migrateFeatureType(featureType);
    }

    public static FeatureSchema swap(FeatureType featureType,
                                     FeatureSchemaSwapperSql schemaSwapperSql) {

        FeatureSchema featureTypeV2 = migration.migrateFeatureType(featureType);

        SchemaSql swap = schemaSwapperSql.swap(featureTypeV2);

        return new ImmutableFeatureSchema.Builder().from(featureTypeV2)
                                                   .propertyMap(swapProperties(featureTypeV2.getProperties()))
                                                   .build();
    }

    static Map<String, FeatureSchema> swapProperties(List<FeatureSchema> properties) {
        return properties.stream()
                         .flatMap(originalProperty -> {

                             FeatureSchema swappedProperty = new ImmutableFeatureSchema.Builder().from(originalProperty)
                                                                                                     .name(originalProperty.getSourcePath().orElse(""))
                                                                                                     .sourcePath(originalProperty.getName())
                                                                                                     .propertyMap(swapProperties(originalProperty.getProperties()))
                                                                                                     .build();

                             return Stream.of(new AbstractMap.SimpleImmutableEntry<>(originalProperty.getName(), swappedProperty));

                         })
                         .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
