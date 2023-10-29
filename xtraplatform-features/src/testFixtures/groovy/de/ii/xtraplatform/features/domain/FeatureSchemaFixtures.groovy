/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain


import de.ii.xtraplatform.features.domain.SchemaBase.Type

class FeatureSchemaFixtures {

    static FeatureSchema VALUE_ARRAY = new ImmutableFeatureSchema.Builder()
            .name("externalprovider")
            .sourcePath("/externalprovider")
            .type(Type.OBJECT)
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
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("task", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=explorationsite_fk]explorationsite_task/[task_fk=id]task")
                    .type(Type.OBJECT_ARRAY)
                    .objectType("Link")
                    .putProperties2("id", new ImmutableFeatureSchema.Builder()
                            .sourcePath("id")
                            .type(Type.STRING))
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
            .putProperties2("programm", new ImmutableFeatureSchema.Builder()
                    .sourcePath("programm")
                    .type(Type.STRING))
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=id]osirisobjekt/id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("kennung", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=id]osirisobjekt/kennung")
                    .type(Type.STRING))
            .build()

    static FeatureSchema SELF_JOINS = new ImmutableFeatureSchema.Builder()
            .name("building")
            .sourcePath("/building")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("consistsOfBuildingPart", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=fk_buildingpart_parent]building")
                    .type(Type.OBJECT_ARRAY)
                    .putProperties2("href", new ImmutableFeatureSchema.Builder()
                            .sourcePath("id")
                            .type(Type.STRING)))
            .putProperties2("parent", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[fk_buildingpart_parent=id]building")
                    .type(Type.OBJECT)
                    .putProperties2("href", new ImmutableFeatureSchema.Builder()
                            .sourcePath("id")
                            .type(Type.STRING)))
            .build()

    static FeatureSchema SELF_JOINS_FILTER = new ImmutableFeatureSchema.Builder()
            .name("building")
            .sourcePath("/building{filter=id>1}")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("oid")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("consistsOfBuildingPart", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=fk_buildingpart_parent]building{filter=href>100}")
                    .type(Type.OBJECT_ARRAY)
                    .putProperties2("href", new ImmutableFeatureSchema.Builder()
                            .sourcePath("id")
                            .type(Type.STRING)))
            .putProperties2("parent", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[fk_buildingpart_parent=id]building{filter=href>1000}")
                    .type(Type.OBJECT)
                    .putProperties2("href", new ImmutableFeatureSchema.Builder()
                            .sourcePath("id")
                            .type(Type.STRING)))
            .build()

    static FeatureSchema SELF_JOIN_NESTED_DUPLICATE = new ImmutableFeatureSchema.Builder()
            .name("building")
            .sourcePath("/building")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("genericAttributesString", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=fk_feature]att_string_building")
                    .type(Type.OBJECT_ARRAY)
                    .putProperties2("name", new ImmutableFeatureSchema.Builder()
                            .sourcePath("name")
                            .type(Type.STRING)))
            .putProperties2("consistsOfBuildingPart", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=fk_buildingpart_parent]building")
                    .type(Type.OBJECT_ARRAY)
                    .putProperties2("genericAttributesString", new ImmutableFeatureSchema.Builder()
                            .sourcePath("[id=fk_feature]att_string_building")
                            .type(Type.OBJECT_ARRAY)
                            .putProperties2("name", new ImmutableFeatureSchema.Builder()
                                    .sourcePath("name")
                                    .type(Type.STRING))))
            .build()

    static FeatureSchema PROPERTY_WITH_MULTIPLE_SOURCE_PATHS = new ImmutableFeatureSchema.Builder()
            .name("address")
            .sourcePath("/o12006")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("objid")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("component", new ImmutableFeatureSchema.Builder()
                    .addSourcePaths("lan")
                    .addSourcePaths("rbz")
                    .addSourcePaths("krs")
                    .addSourcePaths("gmd")
                    .type(Type.STRING))
            .build()

    static FeatureSchema CONCAT_VALUES_JOIN = new ImmutableFeatureSchema.Builder()
            .name("address")
            .sourcePath("/o12006")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("objid")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("component", new ImmutableFeatureSchema.Builder()
                    .type(Type.VALUE_ARRAY)
                    .valueType(Type.STRING)
                    .concatBuilders([
                            new ImmutableFeatureSchema.Builder().sourcePath("lan"),
                            new ImmutableFeatureSchema.Builder().sourcePath("rbz"),
                            new ImmutableFeatureSchema.Builder().sourcePath("krs"),
                            new ImmutableFeatureSchema.Builder().sourcePath("[id7=id]o12007/nam"),
                            new ImmutableFeatureSchema.Builder().sourcePath("gmd"),
                    ]))
            .build()

    //TODO: nested
    static FeatureSchema NESTED_JOINS = new ImmutableFeatureSchema.Builder()
            .name("observationsubject")
            .sourcePath("/observationsubject")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("filterValues", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=observationsubjectid]observationsubject_filtervalues")
                    .type(Type.OBJECT_ARRAY)
                    .objectType("FilterValue")
                    .putProperties2("type", new ImmutableFeatureSchema.Builder()
                            .sourcePath("_type")
                            .type(Type.STRING))
                    .putProperties2("filterValueProperty", new ImmutableFeatureSchema.Builder()
                            .sourcePath("[filtervalueproperty_fk=code]observedproperty")
                            .type(Type.OBJECT)
                            .putProperties2("title", new ImmutableFeatureSchema.Builder()
                                    .sourcePath("symbol")
                                    .type(Type.STRING))
                            .putProperties2("href", new ImmutableFeatureSchema.Builder()
                                    .sourcePath("code")
                                    .type(Type.STRING))))
            .build()

    static FeatureSchema NESTED_VALUE_ARRAY = new ImmutableFeatureSchema.Builder()
            .name("strassenachse")
            .sourcePath("/o42003")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("strasse", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[id=rid]o42003__p0000103000/[p0000103000=objid]o42002")
                    .type(Type.OBJECT)
                    .putProperties2("name", new ImmutableFeatureSchema.Builder()
                            .sourcePath("nam")
                            .type(Type.STRING))
                    .putProperties2("bezeichnung", new ImmutableFeatureSchema.Builder()
                            .sourcePath("[id=rid]o42002__bez/bez")
                            .type(Type.VALUE_ARRAY)
                            .valueType(Type.STRING)))
            .build()

    static FeatureSchema OBJECT_WITHOUT_SOURCE_PATH = new ImmutableFeatureSchema.Builder()
            .name("explorationsite")
            .sourcePath("/explorationsite")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("legalAvailability", new ImmutableFeatureSchema.Builder()
                    .type(Type.OBJECT)
                    .objectType("Link")
                    .putProperties2("title", new ImmutableFeatureSchema.Builder()
                            .sourcePath("legalavailability_fk")
                            .type(Type.STRING))
                    .putProperties2("href", new ImmutableFeatureSchema.Builder()
                            .sourcePath("legalavailability_fk")
                            .type(Type.STRING)))
            .build()

    static FeatureSchema OBJECT_WITHOUT_SOURCE_PATH2 = new ImmutableFeatureSchema.Builder()
            .name("building")
            .sourcePath("/o31001")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("geometry2D", new ImmutableFeatureSchema.Builder()
                    .type(Type.OBJECT)
                    .objectType("BuildingGeometry2D")
                    .putProperties2("geometry", new ImmutableFeatureSchema.Builder()
                            .sourcePath("position")
                            .type(Type.STRING))
                    .putProperties2("horizontalGeometryReference", new ImmutableFeatureSchema.Builder()
                            .sourcePath("[id=id]bu2d_building__horizontalgeometryreference")
                            .type(Type.OBJECT)
                            .putProperties2("href", new ImmutableFeatureSchema.Builder()
                                    .sourcePath("classifier")
                                    .type(Type.STRING))))
            .build()

    static FeatureSchema CONNECTOR_SIMPLE = new ImmutableFeatureSchema.Builder()
            .name("eignungsflaeche")
            .sourcePath("/eignungsflaeche")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("kennung", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[JSON]osirisobjekt/kennung")
                    .type(Type.STRING))
            .build()

    static FeatureSchema CONNECTOR_MERGE = new ImmutableFeatureSchema.Builder()
            .name("eignungsflaeche")
            .sourcePath("/eignungsflaeche")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("programm", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[JSON]osirisobjekt/programm")
                    .type(Type.STRING))
            .putProperties2("kennung", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[JSON]osirisobjekt/kennung")
                    .type(Type.STRING))
            .build()


    static FeatureSchema CONNECTOR_OBJECT = new ImmutableFeatureSchema.Builder()
            .name("eignungsflaeche")
            .sourcePath("/eignungsflaeche")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("osirisobjekt", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[JSON]osirisobjekt")
                    .type(Type.OBJECT)
                    .putProperties2("kennung", new ImmutableFeatureSchema.Builder()
                            .sourcePath("kennung")
                            .type(Type.STRING)))
            .build()


    static FeatureSchema CONNECTOR_MERGE_OBJECT = new ImmutableFeatureSchema.Builder()
            .name("eignungsflaeche")
            .sourcePath("/eignungsflaeche")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.STRING)
                    .role(SchemaBase.Role.ID))
            .putProperties2("osirisobjekt", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[JSON]osirisobjekt")
                    .type(Type.OBJECT)
                    .putProperties2("kennung", new ImmutableFeatureSchema.Builder()
                            .sourcePath("kennung")
                            .type(Type.STRING)))
            .putProperties2("programm", new ImmutableFeatureSchema.Builder()
                    .sourcePath("[JSON]osirisobjekt/programm")
                    .type(Type.STRING))
            .build()


    public static final FeatureSchema BIOTOP = new ImmutableFeatureSchema.Builder()
            .name("biotop")
            .type(Type.OBJECT)
            .sourcePath("/biotop")
            .putProperties2("id",
                    new ImmutableFeatureSchema.Builder()
                            .type(Type.STRING)
                            .role(SchemaBase.Role.ID)
                            .sourcePath("id"))
            .putProperties2("erfasser_array_join",
                    new ImmutableFeatureSchema.Builder()
                            .type(Type.VALUE_ARRAY)
                            .valueType(Type.STRING)
                            .sourcePath("[eid=id]erfasser/name"))
            .putProperties2("kennung",
                    new ImmutableFeatureSchema.Builder()
                            .type(Type.STRING)
                            .sourcePath("kennung"))
            .putProperties2("erfasser",
                    new ImmutableFeatureSchema.Builder()
                            .type(Type.OBJECT)
                            .putProperties2("name",
                                    new ImmutableFeatureSchema.Builder()
                                            .type(Type.STRING)
                                            .sourcePath("name")))
            .putProperties2("erfasser_required",
                    new ImmutableFeatureSchema.Builder()
                            .type(Type.OBJECT)
                            .constraints(new ImmutableSchemaConstraints.Builder()
                                    .required(true)
                                    .build())
                            .putProperties2("name",
                                    new ImmutableFeatureSchema.Builder()
                                            .type(Type.STRING)
                                            .sourcePath("name")))
            .putProperties2("erfasser_array",
                    new ImmutableFeatureSchema.Builder()
                            .type(Type.VALUE_ARRAY)
                            .valueType(Type.STRING)
                            .sourcePath("name"))
            .putProperties2("erfasser_array_required",
                    new ImmutableFeatureSchema.Builder()
                            .type(Type.VALUE_ARRAY)
                            .valueType(Type.STRING)
                            .constraints(new ImmutableSchemaConstraints.Builder()
                                    .required(true)
                                    .build())
                            .sourcePath("name"))
            .build()

    public static final SchemaMapping BIOTOP_MAPPING = new ImmutableSchemaMapping.Builder()
            .targetSchema(BIOTOP)
            .sourcePathTransformer((path, isValue) -> path)
            .build()
}