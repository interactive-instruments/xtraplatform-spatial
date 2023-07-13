/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain


import spock.lang.Specification

/**
 * @author zahnen
 */
class FeatureTokenTransformerSortingSpec extends Specification {

    FeatureTokenReader tokenReader
    List<Object> tokens

    def setup() {
        FeatureTokenTransformerSorting mapper = new FeatureTokenTransformerSorting()
        FeatureQuery query = ImmutableFeatureQuery.builder().type("test").build()
        FeatureEventHandler.ModifiableContext context = mapper.createContext()
                .setQuery(query)
                .setMappings([test: FeatureTokenFixtures.MAPPING])
                .setType('test')

        tokenReader = new FeatureTokenReader(mapper, context)
        tokens = []
        mapper.init(token -> tokens.add(token))
    }

    def 'single feature join before column value'() {

        given:

        when:

        FeatureTokenFixtures.SINGLE_FEATURE_VALUE_ARRAY_AT_END.forEach(token -> tokenReader.onToken(token))

        then:

        tokens == FeatureTokenFixtures.SINGLE_FEATURE_VALUE_ARRAY_IN_ORDER

    }

}
