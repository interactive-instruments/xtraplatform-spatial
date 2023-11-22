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

    def 'structure wrap: #casename'() {

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

        casename           | transformations                            | schema                               | tokens                               | sourceOnlyIf | targetOnlyIf
        "value array"      | wrap("hatGenerAttribut.wert", VALUE_ARRAY) | "bp_anpflanzungbindungerhaltung-min" | "bp_anpflanzungbindungerhaltung-min" | null         | "wrapped"
        "value array noop" | wrap("hatGenerAttribut.wert", VALUE_ARRAY) | "bp_anpflanzungbindungerhaltung-min" | "bp_anpflanzungbindungerhaltung-min" | "wrapped"    | "wrapped"
        "object"           | wrap("gehoertZuPlan", OBJECT)              | "bp_bereich-min"                     | "bp_bereich"                         | null         | "wrapped"
        "object noop"      | wrap("gehoertZuPlan", OBJECT)              | "bp_bereich-min"                     | "bp_bereich"                         | "wrapped"    | "wrapped"

    }

    static List<Object> complete(List<Object> tokens) {
        return [FeatureTokenType.INPUT, FeatureTokenType.FEATURE] + tokens + [FeatureTokenType.FEATURE_END, FeatureTokenType.INPUT_END]
    }

    static Map<String, List<PropertyTransformation>> wrap(String path, Type type) {
        return [(path): [new ImmutablePropertyTransformation.Builder().wrap(type).build()]]
    }

}
