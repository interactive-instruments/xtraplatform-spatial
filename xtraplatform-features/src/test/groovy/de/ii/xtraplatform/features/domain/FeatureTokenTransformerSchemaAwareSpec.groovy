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
class FeatureTokenTransformerSchemaAwareSpec extends Specification {

    FeatureTokenTransformerMapping<FeatureSchema> tokenTransformer
    FeatureTokenReader tokenReader
    SchemaMapping mapping

    def setup() {
        mapping = new ImmutableSchemaMapping.Builder()
                .targetSchema(new ImmutableFeatureSchema.Builder()
                        .name("biotop")
                        .sourcePath("/biotop")
                        .putProperties2("kennung", new ImmutableFeatureSchema.Builder()
                            .sourcePath("kennung")
                            .type(SchemaBase.Type.INTEGER))
                        .build())
                .build()

        tokenTransformer = Mock({
            createContext() >> { ModifiableSchemaAwareContext.<FeatureSchema>create().setMapping(mapping)}
        })

        tokenReader = new FeatureTokenReader(tokenTransformer, tokenTransformer.createContext())
    }

    def 'single feature'() {

        given:

        when:

        FeatureTokenFixtures.SINGLE_FEATURE_SQL.forEach(token -> tokenReader.onToken(token))

        then:
        1 * tokenTransformer.onStart({ FeatureTokenTransformerMapping.SchemaAwareContext<FeatureSchema> context ->
            context.metadata().isSingleFeature()
            context.currentSchema() == mapping.getTargetSchema()
        })

        then:
        1 * tokenTransformer.onFeatureStart(_)

        then:
        1 * tokenTransformer.onValue({ FeatureTokenTransformerMapping.SchemaAwareContext<FeatureSchema> context ->
            context.path() == ["biotop", "id"]
            context.value() == "24"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * tokenTransformer.onValue({ FeatureTokenTransformerMapping.SchemaAwareContext<FeatureSchema> context ->
            context.path() == ["biotop", "kennung"]
            context.value() == "611320001-1"
            context.valueType() == SchemaBase.Type.STRING
            context.currentSchema().getType() == SchemaBase.Type.INTEGER
        })

        then:
        1 * tokenTransformer.onFeatureEnd(_)

        then:
        1 * tokenTransformer.onEnd(_)

    }
}
