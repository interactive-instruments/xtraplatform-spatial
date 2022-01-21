/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSchemaSql
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlRelation
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRelation
import de.ii.xtraplatform.features.domain.FeatureStoreRelation
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreRelation
import de.ii.xtraplatform.features.domain.SchemaBase.Type

class MutationSchemaFixtures {

    static List<SchemaSql> VALUE_ARRAY = [
            new ImmutableSchemaSql.Builder()
                    .name("externalprovider")
                    .sourcePath("externalprovider")
                    .type(Type.OBJECT)
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("externalprovider_externalprovidername")
                            .type(Type.OBJECT_ARRAY)
                            .parentPath(["externalprovider"])
                            .addRelation(ImmutableSqlRelation.builder()
                                    .cardinality(SqlRelation.CARDINALITY.ONE_2_N)
                                    .sourceContainer("externalprovider")
                                    .sourceField("id")
                                    .targetContainer("externalprovider_externalprovidername")
                                    .targetField("externalprovider_fk")
                                    .build())
                            .addProperties(new ImmutableSchemaSql.Builder()
                                    .name("externalprovidername")
                                    .type(Type.STRING)
                                    .sourcePath("externalprovidername")
                                    .parentPath(["externalprovider", "[id=externalprovider_fk]externalprovider_externalprovidername"])
                                    .build())
                            .build())
                    .build()
    ]


    static List<SchemaSql> OBJECT_ARRAY = [
            new ImmutableSchemaSql.Builder()
                    .name("explorationsite")
                    .sourcePath("explorationsite")
                    .type(Type.OBJECT)
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("explorationsite_task")
                            .type(Type.OBJECT_ARRAY)
                            .parentPath(["explorationsite"])
                            .addRelation(ImmutableSqlRelation.builder()
                                    .cardinality(SqlRelation.CARDINALITY.ONE_2_N)
                                    .sourceContainer("explorationsite")
                                    .sourceField("id")
                                    .targetContainer("explorationsite_task")
                                    .targetField("explorationsite_fk")
                                    .build())
                            .addProperties(new ImmutableSchemaSql.Builder()
                                    .name("task")
                                    .sourcePath("task")
                                    .type(Type.OBJECT)
                                    .parentPath(["explorationsite"])
                                    .addRelation(ImmutableSqlRelation.builder()
                                            .cardinality(SqlRelation.CARDINALITY.ONE_2_ONE)
                                            .sourceContainer("explorationsite_task")
                                            .sourceField("task_fk")
                                            .sourceSortKey("id")
                                            .targetContainer("task")
                                            .targetField("id")
                                            .build())
                                    .addProperties(new ImmutableSchemaSql.Builder()
                                            .name("projectname")
                                            .type(Type.STRING)
                                            .sourcePath("title")
                                            .parentPath(["explorationsite", "[id=explorationsite_fk]explorationsite_task", "[task_fk=id]task"])
                                            .build())
                                    .addProperties(new ImmutableSchemaSql.Builder()
                                            .name("id")
                                            .type(Type.STRING)
                                            .sourcePath("href")
                                            .parentPath(["explorationsite", "[id=explorationsite_fk]explorationsite_task", "[task_fk=id]task"])
                                            .build())
                                    .build())
                            .build())
                    .build()
    ]
}
