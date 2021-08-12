/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app


import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.features.domain.SchemaBase.Type

class FeatureSchemaFixtures {

    static FeatureSchema VALUE_ARRAY = new ImmutableFeatureSchema.Builder()
            .name("externalprovider")
            .sourcePath("/externalprovider")
            .type(Type.OBJECT)
    //TODO: only needed for old parser
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("externalprovidername", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=externalprovider_fk]externalprovider_externalprovidername/externalprovidername")
                    .type(Type.VALUE_ARRAY)
                    .valueType(Type.STRING))
            .build()

    static FeatureSchema OBJECT_ARRAY = new ImmutableFeatureSchema.Builder()
            .name("explorationsite")
            .sourcePath("/explorationsite")
            .type(Type.OBJECT)
    //TODO: only needed for old parser
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("task", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=explorationsite_fk]explorationsite_task/[task_fk=id]task")
                    .type(Type.OBJECT_ARRAY)
                    .objectType("Link")
                    .putProperties2("title", new ImmutableFeatureSchema.Builder()
                            .sourcePath("projectname")
                            .type(Type.STRING))
                    .putProperties2("href", new ImmutableFeatureSchema.Builder()
                            .sourcePath("id")
                            .type(Type.STRING)))
            .build()


    static FeatureSchema MERGE = new ImmutableFeatureSchema.Builder()
            .name("eignungsflaeche")
            .sourcePath("/eignungsflaeche")
            .type(Type.OBJECT)
    //TODO: only needed for old parser
            .putProperties2("bla", new ImmutableFeatureSchema.Builder()
                    .sourcePath("bla")
                    .type(Type.VALUE_ARRAY)
                    .valueType(Type.STRING))
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=id]osirisobjekt/id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("kennung", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=id]osirisobjekt/kennung")
                    .type(Type.STRING))
            .build()
}
