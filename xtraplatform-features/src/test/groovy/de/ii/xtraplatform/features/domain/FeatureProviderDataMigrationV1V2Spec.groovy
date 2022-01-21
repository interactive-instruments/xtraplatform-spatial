/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.crs.domain.EpsgCrs
import de.ii.xtraplatform.crs.domain.OgcCrs
import de.ii.xtraplatform.features.app.FeatureProviderDataMigrationV1V2
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import static de.ii.xtraplatform.features.domain.FeatureProperty.Role
import static de.ii.xtraplatform.features.domain.FeatureProperty.Type
import static de.ii.xtraplatform.features.domain.FeatureProviderDataFixtures.ONEO_V1
import static de.ii.xtraplatform.features.domain.FeatureProviderDataFixtures.ONEO_V2

@Ignore
//TODO
class FeatureProviderDataMigrationV1V2Spec extends Specification {

    @Shared
    ObjectMapper objectMapper

    def setupSpec() {
        def yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
                .disable(YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS)


        objectMapper = new ObjectMapper(yamlFactory)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .registerModules(new Jdk8Module(), new GuavaModule())
                .setDefaultMergeable(false)
    }

    def 'migrate'() {

        given:

        FeatureProviderDataV1 data = new ImmutableFeatureProviderDataV1.Builder()
                .id("geoval")
                .createdAt(1586271491161)
                .lastModified(1586271491161)
                .providerType("FEATURE")
                .featureProviderType("SQL")
                .connectionInfo(FeatureProviderDataFixtures.connectionInfo)
                .nativeCrs(OgcCrs.CRS84)
                .putTypes2("observationsubject", new ImmutableFeatureType.Builder()
                        .putProperties2("id", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/id")
                                .type(Type.STRING)
                                .role(Role.ID)
                        )
                        .putProperties2("type", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/_type")
                                .type(Type.STRING)
                        )
                        .putProperties2("geomLowerPoint", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/geomlowerpoint")
                                .type(Type.GEOMETRY)
                        )
                        .putProperties2("shortName", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/shortname")
                                .type(Type.STRING)
                        )
                        .putProperties2("explorationSite.title", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[explorationsite_fk=id]explorationsite/shortname")
                                .type(Type.STRING)
                                .additionalInfo(ImmutableMap.of("role", "LINKTITLE"))
                        )
                        .putProperties2("explorationSite.href", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[explorationsite_fk=id]explorationsite/id")
                                .type(Type.STRING)
                                .additionalInfo(ImmutableMap.of("role", "LINKHREF"))
                        )
                        .putProperties2("process[].title", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_process/[process_fk=id]process/category_fk")
                                .type(Type.STRING)
                                .additionalInfo(ImmutableMap.of("role", "LINKTITLE"))
                        )
                        .putProperties2("process[].href", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_process/[process_fk=id]process/id")
                                .type(Type.STRING)
                                .additionalInfo(ImmutableMap.of("role", "LINKHREF"))
                        )
                        .putProperties2("filterValues[observationsubject_filtervalues].property", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_filtervalues/filtervalueproperty_fk")
                                .type(Type.STRING)
                        )
                        .putProperties2("filterValues[observationsubject_filtervalues].measure", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_filtervalues/resultmeasure")
                                .type(Type.FLOAT)
                        )
                        .putProperties2("filterValues[observationsubject_filtervalues].classification[filtervalue_resultclassification]", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_filtervalues/[id=filtervalue_fk]filtervalue_resultclassification/classificationcode_fk")
                                .type(Type.STRING)
                        )
                )
                .build()

        when:

        FeatureProviderDataV2 migrated = new FeatureProviderDataMigrationV1V2().migrate(data)

        def dataAsYaml = objectMapper.writeValueAsString(migrated)

        then:

        def expected = new File("src/test/resources/migrationv2.yml").text

        dataAsYaml == expected

    }


    def 'migrate oneo'() {

        given:

        when:

        FeatureProviderDataV2 actual = new FeatureProviderDataMigrationV1V2().migrate(ONEO_V1)

        FeatureProviderDataV2 expected = ONEO_V2

        then:

        actual == expected

    }
}
