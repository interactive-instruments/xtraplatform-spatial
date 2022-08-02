/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain

import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import spock.lang.Specification

/**
 * @author zahnen
 */
class FeatureTokenReaderSpec extends Specification {

    FeatureEventHandlerGeneric eventHandler
    FeatureTokenReader tokenReader

    def setup() {
        eventHandler = Mock()

        FeatureEventHandler.ModifiableContext context = ModifiableGenericContext.create();
        SchemaMapping mapping = Mock()
        mapping.getPathSeparator() >> Optional.empty()
        context.setMappings([ft: mapping])
        context.setType('ft')
        tokenReader = new FeatureTokenReader(eventHandler, context)
    }

    def 'single feature'() {

        given:

        when:

        FeatureTokenFixtures.SINGLE_FEATURE.forEach(token -> tokenReader.onToken(token))

        then:
        1 * eventHandler.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().isSingleFeature()
        })

        then:
        1 * eventHandler.onFeatureStart(_)

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "24"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "611320001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventHandler.onFeatureEnd(_)

        then:
        1 * eventHandler.onEnd(_)

    }

    def 'collection with 3 features'() {

        given:

        when:

        FeatureTokenFixtures.COLLECTION.forEach(token -> tokenReader.onToken(token))

        then:
        1 * eventHandler.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().getNumberReturned() == OptionalLong.of(3)
            context.metadata().getNumberMatched() == OptionalLong.of(12)
        })

        then:
        1 * eventHandler.onFeatureStart(_)

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "19"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventHandler.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.geometryType() == Optional.of(SimpleFeatureGeometry.POINT)
        })

        then:
        1 * eventHandler.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "6.295202392345018"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "50.11336914792363"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventHandler.onArrayEnd(_)

        then:
        1 * eventHandler.onObjectEnd(_)

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "580340001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventHandler.onFeatureEnd(_)

        then:
        1 * eventHandler.onFeatureStart(_)

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "20"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "580410003-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventHandler.onFeatureEnd(_)

        then:
        1 * eventHandler.onFeatureStart(_)

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "21"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventHandler.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.geometryType() == Optional.of(SimpleFeatureGeometry.MULTI_POINT)
        })

        then:
        1 * eventHandler.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "6.406233970262905"
            context.valueType() == SchemaBase.Type.FLOAT
            context.indexes() == [1]
        })

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "50.1501333536934"
            context.valueType() == SchemaBase.Type.FLOAT
            context.indexes() == [2]
        })

        then:
        1 * eventHandler.onArrayEnd(_)

        then:
        1 * eventHandler.onObjectEnd(_)

        then:
        1 * eventHandler.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "631510001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventHandler.onFeatureEnd(_)

        then:
        1 * eventHandler.onEnd(_)

    }
}
