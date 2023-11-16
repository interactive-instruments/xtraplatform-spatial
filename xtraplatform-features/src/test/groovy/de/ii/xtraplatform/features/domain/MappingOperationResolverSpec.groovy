/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain


import spock.lang.Shared
import spock.lang.Specification

/**
 * @author zahnen
 */
class MappingOperationResolverSpec extends Specification {

    @Shared
    MappingOperationResolver mappingOperationResolver = new MappingOperationResolver()

    def 'mapping operation resolver: #casename'() {

        given:

        def types = ["test": source]

        when:

        def actual = mappingOperationResolver.resolve(types)

        then:

        actual.test == expected

        where:

        casename               | source                                     | expected
        "concat values"        | FeatureSchemaFixtures.CONCAT_VALUES_JOIN   | CONCAT_VALUES_JOIN_RESOLVED
        "concat object arrays" | FeatureSchemaFixtures.CONCAT_OBJECT_ARRAYS | CONCAT_OBJECT_ARRAYS_RESOLVED

    }


    static FeatureSchema CONCAT_VALUES_JOIN_RESOLVED = new ImmutableFeatureSchema.Builder()
            .from(FeatureSchemaFixtures.CONCAT_VALUES_JOIN)
            .putProperties2("component", new ImmutableFeatureSchema.Builder()
                    .type(SchemaBase.Type.VALUE_ARRAY)
                    .valueType(SchemaBase.Type.STRING)
                    .sourcePaths(["lan", "rbz", "krs", "[id7=id]o12007/nam", "gmd"])
                    .concatBuilders([
                            new ImmutableFeatureSchema.Builder().sourcePath("lan")
                                    .type(SchemaBase.Type.VALUE_ARRAY)
                                    .valueType(SchemaBase.Type.STRING),
                            new ImmutableFeatureSchema.Builder().sourcePath("rbz")
                                    .type(SchemaBase.Type.VALUE_ARRAY)
                                    .valueType(SchemaBase.Type.STRING),
                            new ImmutableFeatureSchema.Builder().sourcePath("krs")
                                    .type(SchemaBase.Type.VALUE_ARRAY)
                                    .valueType(SchemaBase.Type.STRING),
                            new ImmutableFeatureSchema.Builder().sourcePath("[id7=id]o12007/nam")
                                    .type(SchemaBase.Type.VALUE_ARRAY)
                                    .valueType(SchemaBase.Type.STRING),
                            new ImmutableFeatureSchema.Builder().sourcePath("gmd")
                                    .type(SchemaBase.Type.VALUE_ARRAY)
                                    .valueType(SchemaBase.Type.STRING),
                    ]))
            .build()

    static FeatureSchema CONCAT_OBJECT_ARRAYS_RESOLVED = new ImmutableFeatureSchema.Builder()
            .from(FeatureSchemaFixtures.CONCAT_OBJECT_ARRAYS)
            .putProperties2("hatObjekt", new ImmutableFeatureSchema.Builder()
                    .type(SchemaBase.Type.OBJECT_ARRAY)
                    .concat([
                            new ImmutableFeatureSchema.Builder()
                                    .type(SchemaBase.Type.OBJECT_ARRAY)
                                    .name("bst_abwasserleitung")
                                    .sourcePath("[_id=gehoertzuplan_pfs_plan_fk]bst_abwasserleitung")
                                    .putProperties2("id", new ImmutableFeatureSchema.Builder()
                                            .sourcePath("_id")
                                            .type(SchemaBase.Type.INTEGER))
                                    .putProperties2("title", new ImmutableFeatureSchema.Builder()
                                            .sourcePath("name")
                                            .type(SchemaBase.Type.STRING))
                                    .build(),
                            new ImmutableFeatureSchema.Builder()
                                    .type(SchemaBase.Type.OBJECT_ARRAY)
                                    .name("bst_erdgasleitung")
                                    .sourcePath("[_id=gehoertzuplan_pfs_plan_fk]bst_erdgasleitung")
                                    .putProperties2("id", new ImmutableFeatureSchema.Builder()
                                            .sourcePath("_id")
                                            .type(SchemaBase.Type.INTEGER))
                                    .putProperties2("title", new ImmutableFeatureSchema.Builder()
                                            .sourcePath("name")
                                            .type(SchemaBase.Type.STRING))
                                    .build(),
                    ])
                    .putPropertyMap("0_id", new ImmutableFeatureSchema.Builder()
                            .name("id")
                            .path(List.of("0_id"))
                            .sourcePath("[_id=gehoertzuplan_pfs_plan_fk]bst_abwasserleitung/_id")
                            .type(SchemaBase.Type.INTEGER))
                    .putPropertyMap("0_title", new ImmutableFeatureSchema.Builder()
                            .name("title")
                            .path(List.of("0_title"))
                            .sourcePath("[_id=gehoertzuplan_pfs_plan_fk]bst_abwasserleitung/name")
                            .type(SchemaBase.Type.STRING))
                    .putPropertyMap("1_id", new ImmutableFeatureSchema.Builder()
                            .name("id")
                            .path(List.of("1_id"))
                            .sourcePath("[_id=gehoertzuplan_pfs_plan_fk]bst_erdgasleitung/_id")
                            .type(SchemaBase.Type.INTEGER))
                    .putPropertyMap("1_title", new ImmutableFeatureSchema.Builder()
                            .name("title")
                            .path(List.of("1_title"))
                            .sourcePath("[_id=gehoertzuplan_pfs_plan_fk]bst_erdgasleitung/name")
                            .type(SchemaBase.Type.STRING))
            )
            .build()
}

