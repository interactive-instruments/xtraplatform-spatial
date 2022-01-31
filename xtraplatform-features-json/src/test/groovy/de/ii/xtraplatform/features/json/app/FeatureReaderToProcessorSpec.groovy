/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.json.app

import de.ii.xtraplatform.features.domain.Feature
import de.ii.xtraplatform.features.domain.FeatureProcessor
import de.ii.xtraplatform.features.domain.FeatureReader
import de.ii.xtraplatform.features.domain.FeatureReaderGeneric
import de.ii.xtraplatform.features.domain.FeatureSchemaMapper
import de.ii.xtraplatform.features.domain.ModifiableFeature
import de.ii.xtraplatform.features.domain.ModifiableProperty
import de.ii.xtraplatform.features.domain.Property
import de.ii.xtraplatform.features.domain.PropertyBase
import de.ii.xtraplatform.features.domain.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author zahnen
 */
//@Ignore
class FeatureReaderToProcessorSpec extends Specification {
//TODO: FeatureObjectEncoderSpec
 /*   static final Logger LOGGER = LoggerFactory.getLogger(FeatureReaderToProcessorSpec.class)

    //TODO: test schema assignment
    def 'build feature'() {

        given:

        FeatureProcessor<Property, Feature, Schema> featureProcessor = Mock()
        FeatureReader<Schema, Schema> featureReader = new FeatureReaderToProcessor<Property, Feature, Schema>(featureProcessor)
        FeatureSchemaMapper<Schema> featureSchemaMapper = new FeatureSchemaMapper<>(null, featureReader)
        GeoJsonParser parser = new GeoJsonParser(featureSchemaMapper)

        def source = new File('src/test/resources/nested_array_object.json').text

        when:

        parser.parse(source)

        then:

        featureProcessor.createFeature() >> { args -> ModifiableFeature.create() }
        featureProcessor.createProperty() >> { args -> ModifiableProperty.create() }

        1 * featureProcessor.process(expected)

        where:

        expected = ModifiableFeature.create()
                                    .schema(Optional.empty())
                                    .addProperties(ModifiableProperty.create()
                                                                     .type(PropertyBase.Type.VALUE)
                                                                     .schema(Optional.empty())
                                                                     .value("24"))
                                    .addProperties(ModifiableProperty.create()
                                                                     .type(PropertyBase.Type.ARRAY)
                                                                     .schema(Optional.empty())
                                                                     .addNestedProperties(ModifiableProperty.create()
                                                                                                            .type(PropertyBase.Type.OBJECT)
                                                                                                            .schema(Optional.empty())
                                                                                                            .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                   .type(PropertyBase.Type.ARRAY)
                                                                                                                                                   .schema(Optional.empty())
                                                                                                                                                   .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                          .type(PropertyBase.Type.OBJECT)
                                                                                                                                                                                          .schema(Optional.empty())
                                                                                                                                                                                          .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                                                                 .type(PropertyBase.Type.VALUE)
                                                                                                                                                                                                                                 .schema(Optional.empty())
                                                                                                                                                                                                                                 .value("11"))
                                                                                                                                                                                          .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                                                                 .type(PropertyBase.Type.ARRAY)
                                                                                                                                                                                                                                 .schema(Optional.empty())
                                                                                                                                                                                                                                 .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                                                                                                        .type(PropertyBase.Type.VALUE)
                                                                                                                                                                                                                                                                        .schema(Optional.empty())
                                                                                                                                                                                                                                                                        .value("34"))))
                                                                                                                                                   .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                          .type(PropertyBase.Type.OBJECT)
                                                                                                                                                                                          .schema(Optional.empty())
                                                                                                                                                                                          .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                                                                 .type(PropertyBase.Type.ARRAY)
                                                                                                                                                                                                                                 .schema(Optional.empty())
                                                                                                                                                                                                                                 .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                                                                                                        .type(PropertyBase.Type.VALUE)
                                                                                                                                                                                                                                                                        .schema(Optional.empty())
                                                                                                                                                                                                                                                                        .value("35"))
                                                                                                                                                                                                                                 .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                                                                                                        .type(PropertyBase.Type.VALUE)
                                                                                                                                                                                                                                                                        .schema(Optional.empty())
                                                                                                                                                                                                                                                                        .value("36"))))
                                                                                                                                                   .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                          .type(PropertyBase.Type.OBJECT)
                                                                                                                                                                                          .schema(Optional.empty())
                                                                                                                                                                                          .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                                                                 .type(PropertyBase.Type.VALUE)
                                                                                                                                                                                                                                 .schema(Optional.empty())
                                                                                                                                                                                                                                 .value("12"))
                                                                                                                                                                                          .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                                                                 .type(PropertyBase.Type.ARRAY)
                                                                                                                                                                                                                                 .schema(Optional.empty())
                                                                                                                                                                                                                                 .addNestedProperties(ModifiableProperty.create()
                                                                                                                                                                                                                                                                        .type(PropertyBase.Type.VALUE)
                                                                                                                                                                                                                                                                        .schema(Optional.empty())
                                                                                                                                                                                                                                                                        .value("37")))))))
                                    .addProperties(ModifiableProperty.create()
                                                                     .type(PropertyBase.Type.VALUE)
                                                                     .schema(Optional.empty())
                                                                     .value("611320001-1"))

    }*/
}
