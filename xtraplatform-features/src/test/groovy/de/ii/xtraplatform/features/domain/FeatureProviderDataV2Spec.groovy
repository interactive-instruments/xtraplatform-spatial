/*
 * Copyright 2020 interactive instruments GmbH
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
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.crs.domain.OgcCrs
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import spock.lang.Shared
import spock.lang.Specification

class FeatureProviderDataV2Spec extends Specification {

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

    def 'serialize'() {

        given:

        FeatureProviderDataV2 data = new ImmutableFeatureProviderDataV2.Builder()
                .id("geoval")
                .createdAt(1586271491161)
                .lastModified(1586271491161)
                .providerType("FEATURE")
                .featureProviderType("SQL")
                .nativeCrs(OgcCrs.CRS84)
                .defaultLanguage("de")
                .putTypes2("observationsubject", new ImmutableFeatureTypeV2.Builder()
                        .path("/observationsubject")
                        .label("Untersuchungsobjekt")
                        .description("Ein Untersuchungsobjekt ist entweder eine Probe für Untersuchungen im Labor oder eine In-Situ-Messung")
                        .putProperties2("id", new ImmutableFeaturePropertyV2.Builder()
                                .path("id")
                                .role(FeaturePropertyV2.Role.ID)
                                .label("Objekt-Id")
                                .description("Eindeutige Id des Objekts")
                                .constraints(ImmutableMap.of("regex", ImmutableList.of("[a-zA-Z0-9_]{3,}")))
                        )
                        .putProperties2("type", new ImmutableFeaturePropertyV2.Builder()
                                .path("_type")
                                .label("Art")
                                .constraints(ImmutableMap.of("codelist", ImmutableList.of("geoval_type"),
                                        "enum", ImmutableList.of("Borehole", "TrialPit")))
                        )
                        .putProperties2("shortName", new ImmutableFeaturePropertyV2.Builder()
                                .path("shortname")
                                .transformers(ImmutableMap.of("codelist", "nullValues"))
                        )
                        .putProperties2("geomLowerPoint", new ImmutableFeaturePropertyV2.Builder()
                                .path("geomlowerpoint")
                                .type(FeaturePropertyV2.Type.GEOMETRY)
                                .geometryType(SimpleFeatureGeometry.POINT)
                        )
                        .putProperties2("explorationSite", new ImmutableFeaturePropertyV2.Builder()
                                .path("[explorationsite_fk=id]explorationsite")
                                .type(FeaturePropertyV2.Type.OBJECT)
                                .objectType("Link")
                                .label("Aufschlußpunkt")
                                .putProperties2("title", new ImmutableFeaturePropertyV2.Builder()
                                    .path("shortname")
                                )
                                .putProperties2("href", new ImmutableFeaturePropertyV2.Builder()
                                        .path("id")
                                        .constraints(ImmutableMap.of("required", ImmutableList.of("true")))
                                )
                        )
                        .putProperties2("process", new ImmutableFeaturePropertyV2.Builder()
                                .path("[id=observationsubject_fk]observationsubject_process/[process_fk=id]process")
                                .type(FeaturePropertyV2.Type.OBJECT_ARRAY)
                                .objectType("Link")
                                .label("Versuche")
                                .constraints(ImmutableMap.of("minOccurrence", ImmutableList.of("1"), "maxOccurrence", ImmutableList.of("3")))
                                .putProperties2("title", new ImmutableFeaturePropertyV2.Builder()
                                        .path("category_fk")
                                )
                                .putProperties2("href", new ImmutableFeaturePropertyV2.Builder()
                                        .path("id")
                                        .constraints(ImmutableMap.of("required", ImmutableList.of("true")))
                                )
                        )
                        .putProperties2("filterValues", new ImmutableFeaturePropertyV2.Builder()
                                .path("[id=observationsubject_fk]observationsubject_filtervalues")
                                .type(FeaturePropertyV2.Type.OBJECT_ARRAY)
                                .objectType("FilterValue")
                                .label("Leitkennwerte")
                                .putProperties2("property", new ImmutableFeaturePropertyV2.Builder()
                                        .path("filtervalueproperty_fk")
                                        .label("Eigenschaft")
                                )
                                .putProperties2("property", new ImmutableFeaturePropertyV2.Builder()
                                        .path("filtervalueproperty_fk")
                                        .label("Eigenschaft")
                                )
                                .putProperties2("measure", new ImmutableFeaturePropertyV2.Builder()
                                        .path("resultmeasure")
                                        .type(FeaturePropertyV2.Type.FLOAT)
                                        .label("Messwert")
                                        .constraints(ImmutableMap.of("min", ImmutableList.of("0.0"), "max", ImmutableList.of("100.0")))
                                )
                                .putProperties2("classification", new ImmutableFeaturePropertyV2.Builder()
                                        .path("[id=filtervalue_fk]filtervalue_resultclassification/classificationcode_fk")
                                        .type(FeaturePropertyV2.Type.VALUE_ARRAY)
                                        .valueType(FeaturePropertyV2.Type.STRING)
                                        .label("Klassifikationen")
                                )
                        )
                )
                .putCodelists("nullValues", ImmutableMap.of("No Information", "null", "-1", "null"))
                .build()

        when:

        def dataAsYaml = objectMapper.writeValueAsString(data)

        then:

        def expected = new File("src/test/resources/geoval.yml").text

        dataAsYaml == expected

    }
}
