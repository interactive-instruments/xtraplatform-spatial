/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

public class FeatureSchemaNamePathSwapper {
/*
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

 */
}
