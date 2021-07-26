/*
 * Copyright 2021 interactive instruments GmbH
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
class FeatureTokenEmitterSpec extends Specification {

    FeatureEventHandler<FeatureEventHandler.ModifiableContext> eventHandler
    FeatureTokenReader tokenReader
    List<Object> tokens

    def setup() {
        tokens = []
        eventHandler = (FeatureTokenEmitter2<FeatureEventHandler.ModifiableContext>) (token -> tokens.add(token))
        tokenReader = new FeatureTokenReader(eventHandler, ModifiableGenericContext.create())
    }

    def 'single feature'() {

        given:

        when:

        FeatureTokenFixtures.SINGLE_FEATURE.forEach(token -> tokenReader.onToken(token))

        then:

        tokens == FeatureTokenFixtures.SINGLE_FEATURE

    }

    def 'collection with 3 features'() {

        given:

        when:

        FeatureTokenFixtures.COLLECTION.forEach(token -> tokenReader.onToken(token))

        then:

        tokens == FeatureTokenFixtures.COLLECTION

    }
}
