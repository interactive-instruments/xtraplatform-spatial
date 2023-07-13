/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain

import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry

import static de.ii.xtraplatform.features.domain.SchemaBase.Role
import static de.ii.xtraplatform.features.domain.SchemaBase.Type

class FeatureTokenFixtures {

    public static final FeatureSchema SCHEMA = new ImmutableFeatureSchema.Builder()
            .name("biotop")
            .type(Type.OBJECT)
            .sourcePath("/biotop")
            .putProperties2("id",
                    new ImmutableFeatureSchema.Builder()
                            .type(Type.STRING)
                            .role(Role.ID)
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

    public static final SchemaMapping MAPPING = new ImmutableSchemaMapping.Builder()
            .targetSchema(SCHEMA)
            .useTargetPaths(false)
            .sourcePathTransformer((path, isValue) -> path)
            .build()

    public static final SchemaMapping MAPPING_OLD = SchemaMapping.withTargetPaths(MAPPING)

    public static final List<Object> SINGLE_FEATURE = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "24",
            Type.STRING,
            FeatureTokenType.VALUE,
            ["kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_SQL = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            ["biotop"],
            FeatureTokenType.VALUE,
            ["biotop", "id"],
            "24",
            Type.STRING,
            FeatureTokenType.VALUE,
            ["biotop", "kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_POINT = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "24",
            Type.STRING,
            FeatureTokenType.OBJECT,
            ["geometry"],
            SimpleFeatureGeometry.POINT,
            2,
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.VALUE,
            ["geometry"],
            "8.18523495507722",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "49.698295103021096",
            Type.FLOAT,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_MULTI_POINT = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "20",
            Type.STRING,
            FeatureTokenType.OBJECT,
            ["geometry"],
            SimpleFeatureGeometry.MULTI_POINT,
            2,
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.VALUE,
            ["geometry"],
            "6.406233970262905",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "50.1501333536934",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "7.406233970262905",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "51.1501333536934",
            Type.FLOAT,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "580410003-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_MULTI_POLYGON = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "21",
            Type.STRING,
            FeatureTokenType.OBJECT,
            ["geometry"],
            SimpleFeatureGeometry.MULTI_POLYGON,
            2,
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.VALUE,
            ["geometry"],
            "8.18523495507722",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "49.698295103021096",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "8.185283687843047",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "49.69823291309017",
            Type.FLOAT,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.VALUE,
            ["geometry"],
            "8.185681115675656",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "49.698286680057166",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "8.185796151881165",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "49.69836248910692",
            Type.FLOAT,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.VALUE,
            ["geometry"],
            "8.186313615874417",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "49.698603368350874",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "8.18641074595947",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "49.69866280390489",
            Type.FLOAT,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "631510001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_POLYGON_INNER = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            ["bag:pand"],
            FeatureTokenType.VALUE,
            ["bag:pand", "gml:@id"],
            "pand.4bad2970-2214-486c-a475-a04300136d94",
            Type.STRING,
            FeatureTokenType.OBJECT,
            ["bag:pand", "bag:geom"],
            SimpleFeatureGeometry.POLYGON,
            FeatureTokenType.ARRAY,
            ["bag:pand", "bag:geom", "gml:Polygon"],
            FeatureTokenType.VALUE,
            ["bag:pand", "bag:geom", "gml:Polygon", "gml:exterior", "gml:LinearRing", "gml:posList"],
            "\n" +
                    "                206476.339000 475299.692000 206475.466000 475298.741000 206465.324000 475308.48000\n" +
                    "                206456.806000 475208.767000 206469.818000 475286.826000 206468.873000 475285.762000\n" +
                    "                206470.851000 475284.005000 206471.768000 475285.037000 206471.924000 475285.206000\n" +
                    "                206473.055000 475285.251000 206473.224000 475285.096000 206474.219000 475284.173000\n" +
                    "                206475.856000 475285.939000 206474.852000 475286.870000 206481.919000 475294.571000\n" +
                    "                206476.339000 475299.692000\n" +
                    "              ",
            Type.STRING,
            FeatureTokenType.VALUE,
            ["bag:pand", "bag:geom", "gml:Polygon", "gml:interior", "gml:LinearRing", "gml:posList"],
            "\n" +
                    "                206475.281000 475298.053000 206477.379000 475296.023000 206474.715000 475293.270000\n" +
                    "                206472.617000 475295.300000 206475.281000 475298.053000\n" +
                    "              ",
            Type.STRING,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.VALUE,
            ["bag:pand", "bag:identificatie"],
            "0150100000004952",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_NESTED_OBJECT = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "24",
            Type.STRING,
            FeatureTokenType.OBJECT,
            ["erfasser"],
            FeatureTokenType.VALUE,
            ["erfasser", "name"],
            "John Doe",
            Type.STRING,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_NESTED_OBJECT_EMPTY = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "24",
            Type.STRING,
            FeatureTokenType.OBJECT,
            ["erfasser"],
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_NESTED_OBJECT_EMPTY_REQUIRED = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "24",
            Type.STRING,
            FeatureTokenType.OBJECT,
            ["erfasser_required"],
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_VALUE_ARRAY = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "24",
            Type.STRING,
            FeatureTokenType.ARRAY,
            ["erfasser_array"],
            FeatureTokenType.VALUE,
            ["erfasser_array"],
            "John Doe",
            Type.STRING,
            FeatureTokenType.VALUE,
            ["erfasser_array"],
            "Jane Doe",
            Type.STRING,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_VALUE_ARRAY_EMPTY = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "24",
            Type.STRING,
            FeatureTokenType.ARRAY,
            ["erfasser_array"],
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_VALUE_ARRAY_EMPTY_REQUIRED = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "24",
            Type.STRING,
            FeatureTokenType.ARRAY,
            ["erfasser_array_required"],
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_VALUE_ARRAY_AT_END = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["biotop", "id"],
            "24",
            Type.STRING,
            FeatureTokenType.VALUE,
            ["biotop", "kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.ARRAY,
            ["biotop", "erfasser_array_join"],
            FeatureTokenType.VALUE,
            ["biotop", "erfasser_array_join"],
            "John Doe",
            Type.STRING,
            FeatureTokenType.VALUE,
            ["biotop", "erfasser_array_join"],
            "Jane Doe",
            Type.STRING,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_VALUE_ARRAY_IN_ORDER = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["biotop", "id"],
            "24",
            Type.STRING,
            FeatureTokenType.ARRAY,
            ["biotop", "erfasser_array_join"],
            FeatureTokenType.VALUE,
            ["biotop", "erfasser_array_join"],
            "John Doe",
            Type.STRING,
            FeatureTokenType.VALUE,
            ["biotop", "erfasser_array_join"],
            "Jane Doe",
            Type.STRING,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.VALUE,
            ["biotop", "kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> SINGLE_FEATURE_NESTED_OBJECT_ARRAYS = [
            FeatureTokenType.INPUT,
            true,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "24",
            Type.STRING,
            FeatureTokenType.ARRAY,
            ["raumreferenz"],
            FeatureTokenType.OBJECT,
            ["raumreferenz"],
            FeatureTokenType.ARRAY,
            ["raumreferenz", "ortsangabe"],
            FeatureTokenType.OBJECT,
            ["raumreferenz", "ortsangabe"],
            FeatureTokenType.VALUE,
            ["raumreferenz", "ortsangabe", "kreisschluessel"],
            "11",
            Type.INTEGER,
            FeatureTokenType.ARRAY,
            ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"],
            FeatureTokenType.VALUE,
            ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"],
            "34",
            Type.INTEGER,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.OBJECT,
            ["raumreferenz", "ortsangabe"],
            FeatureTokenType.ARRAY,
            ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"],
            FeatureTokenType.VALUE,
            ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"],
            "35",
            Type.INTEGER,
            FeatureTokenType.VALUE,
            ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"],
            "36",
            Type.INTEGER,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.OBJECT,
            ["raumreferenz", "ortsangabe"],
            FeatureTokenType.VALUE,
            ["raumreferenz", "ortsangabe", "kreisschluessel"],
            "12",
            Type.INTEGER,
            FeatureTokenType.ARRAY,
            ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"],
            FeatureTokenType.VALUE,
            ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"],
            "37",
            Type.INTEGER,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "611320001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]

    public static final List<Object> COLLECTION = [
            FeatureTokenType.INPUT,
            3L,
            12L,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "19",
            Type.STRING,
            FeatureTokenType.OBJECT,
            ["geometry"],
            SimpleFeatureGeometry.POINT,
            2,
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.VALUE,
            ["geometry"],
            "6.295202392345018",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "50.11336914792363",
            Type.FLOAT,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "580340001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "20",
            Type.STRING,
            FeatureTokenType.VALUE,
            ["kennung"],
            "580410003-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.FEATURE,
            FeatureTokenType.VALUE,
            ["id"],
            "21",
            Type.STRING,
            FeatureTokenType.OBJECT,
            ["geometry"],
            SimpleFeatureGeometry.MULTI_POINT,
            2,
            FeatureTokenType.ARRAY,
            ["geometry"],
            FeatureTokenType.VALUE,
            ["geometry"],
            "6.406233970262905",
            Type.FLOAT,
            FeatureTokenType.VALUE,
            ["geometry"],
            "50.1501333536934",
            Type.FLOAT,
            FeatureTokenType.ARRAY_END,
            FeatureTokenType.OBJECT_END,
            FeatureTokenType.VALUE,
            ["kennung"],
            "631510001-1",
            Type.STRING,
            FeatureTokenType.FEATURE_END,
            FeatureTokenType.INPUT_END
    ]
}
