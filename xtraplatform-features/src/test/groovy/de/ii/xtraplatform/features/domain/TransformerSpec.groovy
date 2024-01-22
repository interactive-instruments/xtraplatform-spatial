/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain

import de.ii.xtraplatform.features.domain.SchemaBase.Type
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation
import spock.lang.Specification

import static de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext
import static de.ii.xtraplatform.features.domain.SchemaBase.Type.OBJECT
import static de.ii.xtraplatform.features.domain.SchemaBase.Type.VALUE_ARRAY

/**
 * @author zahnen
 */
class TransformerSpec extends Specification {

    def 'transformer: #casename'() {

        given:

        List<Object> actual = []
        FeatureSchema featureSchema = FeatureSchemaFixtures.fromYaml(schema)
        FeatureTokens featureTokens = FeatureTokenFixtures.fromYaml(tokens)

        FeatureTokenTransformerMappings mapper = Util.createMapper(actual, transformations)
        ModifiableContext<FeatureSchema, SchemaMapping> context = Util.createContext(mapper, featureSchema, false)
        FeatureTokenReader reader = Util.createReader(mapper, context)

        when:

        def src = featureTokens.asSource(sourceOnlyIf)
        complete(src).forEach(token -> reader.onToken(token))

        then:

        def expected = featureTokens.asTarget(targetOnlyIf)
        actual == complete(expected)

        where:

        casename                | transformations                                        | schema                               | tokens                               | sourceOnlyIf | targetOnlyIf
        "wrap value array"      | wrap("hatGenerAttribut.wert", VALUE_ARRAY)             | "bp_anpflanzungbindungerhaltung-min" | "bp_anpflanzungbindungerhaltung-min" | null         | "wrapped"
        "wrap value array noop" | wrap("hatGenerAttribut.wert", VALUE_ARRAY)             | "bp_anpflanzungbindungerhaltung-min" | "bp_anpflanzungbindungerhaltung-min" | "wrapped"    | "wrapped"
        "wrap object"           | wrap("gehoertZuPlan", OBJECT)                          | "bp_bereich-min"                     | "bp_bereich"                         | null         | "wrapped"
        "wrap object noop"      | wrap("gehoertZuPlan", OBJECT)                          | "bp_bereich-min"                     | "bp_bereich"                         | "wrapped"    | "wrapped"
        "object array concat"   | concat("hatObjekt", true)                              | "pfs_plan-hatObjekt"                 | "pfs_plan-hatObjekt"                 | "source"     | "concat"
        "object array format"   | mapFormat("hatObjekt", ["href": "id::{{id}}"], "id")   | "pfs_plan-hatObjekt"                 | "pfs_plan-hatObjekt"                 | "concat"     | "mapped"
        "object array reduce"   | reduceFormat("hatObjekt", "id::{{id}}", "id")          | "pfs_plan-hatObjekt"                 | "pfs_plan-hatObjekt"                 | "concat"     | "reduced"
        "object array select"   | reduceSelect("hatObjekt", "id")                        | "pfs_plan-hatObjekt"                 | "pfs_plan-hatObjekt"                 | "concat"     | "selected"
        "value array coalesce"  | coalesce("hatObjekt", false)                           | "pfs_plan-hatObjekt-coalesce"        | "pfs_plan-hatObjekt"                 | "source2"   | "coalesce"
        "concat values"         | concat("process.title", false)                         | "observation"                        | "observation"                        | "source"     | "concat"
        "wrap value array"      | wrap("process.title", VALUE_ARRAY)                     | "observation"                        | "observation"                        | "concat"     | "wrapped"
        "reduce value array"    | arrayReduceFormat("process.title", "{{0}} nach {{1}}") | "observation"                        | "observation"                        | "wrapped"    | "reduced"

    }

    static List<Object> complete(List<Object> tokens) {
        return [FeatureTokenType.INPUT, FeatureTokenType.FEATURE] + tokens + [FeatureTokenType.FEATURE_END, FeatureTokenType.INPUT_END]
    }

    static Map<String, List<PropertyTransformation>> wrap(String path, Type type) {
        return [(path): [new ImmutablePropertyTransformation.Builder().wrap(type).build()]]
    }

    static Map<String, List<PropertyTransformation>> mapFormat(String path, Map<String, String> map, String property) {
        return [(path): [new ImmutablePropertyTransformation.Builder().objectRemoveSelect(property).objectMapFormat(map).build()]]
    }

    static Map<String, List<PropertyTransformation>> reduceFormat(String path, String template, String property) {
        return [(path): [new ImmutablePropertyTransformation.Builder().objectRemoveSelect(property).objectReduceFormat(template).build()]]
    }

    static Map<String, List<PropertyTransformation>> reduceSelect(String path, String property) {
        return [(path): [new ImmutablePropertyTransformation.Builder().objectRemoveSelect(property).objectReduceSelect(property).build()]]
    }

    static Map<String, List<PropertyTransformation>> duplicate(String path, Map<String, String> map) {
        return [(path): [new ImmutablePropertyTransformation.Builder().objectMapDuplicate(map).build()]]
    }

    static Map<String, List<PropertyTransformation>> concat(String path, boolean isObject) {
        return [(path): [new ImmutablePropertyTransformation.Builder().concat(isObject).build()]]
    }

    static Map<String, List<PropertyTransformation>> coalesce(String path, boolean isObject) {
        return [(path): [new ImmutablePropertyTransformation.Builder().coalesce(isObject).build()]]
    }

    static Map<String, List<PropertyTransformation>> arrayReduceFormat(String path, String template) {
        return [(path): [new ImmutablePropertyTransformation.Builder().arrayReduceFormat(template).build()]]
    }

    static Map<String, List<PropertyTransformation>> noop() {
        return [:]
    }

}
