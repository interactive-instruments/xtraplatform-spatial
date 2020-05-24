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
                                .role(FeatureSchema.Role.ID)
                                .label("Objekt-Id")
                                .description("Eindeutige Id des Objekts")
                                .constraints(new ImmutableConstraints.Builder()
                                        .regex("[a-zA-Z0-9_]{3,}")
                                        .build())
                        )
                        .putProperties2("type", new ImmutableFeaturePropertyV2.Builder()
                                .path("_type")
                                .label("Art")
                                .constraints(new ImmutableConstraints.Builder()
                                        .codelist("geoval_type")
                                        .enumValues(ImmutableList.of("Borehole", "TrialPit"))
                                        .build())
                        )
                        .putProperties2("shortName", new ImmutableFeaturePropertyV2.Builder()
                                .path("shortname")
                                .transformers(ImmutableMap.of("codelist", "nullValues"))
                        )
                        .putProperties2("geomLowerPoint", new ImmutableFeaturePropertyV2.Builder()
                                .path("geomlowerpoint")
                                .type(FeatureSchema.Type.GEOMETRY)
                                .geometryType(SimpleFeatureGeometry.POINT)
                        )
                        .putProperties2("explorationSite", new ImmutableFeaturePropertyV2.Builder()
                                .path("[explorationsite_fk=id]explorationsite")
                                .type(FeatureSchema.Type.OBJECT)
                                .objectType("Link")
                                .label("Aufschlußpunkt")
                                .putProperties2("title", new ImmutableFeaturePropertyV2.Builder()
                                        .path("shortname")
                                )
                                .putProperties2("href", new ImmutableFeaturePropertyV2.Builder()
                                        .path("id")
                                        .constraints(new ImmutableConstraints.Builder()
                                                .required(true)
                                                .build()))
                        )
                        .putProperties2("process", new ImmutableFeaturePropertyV2.Builder()
                                .path("[id=observationsubject_fk]observationsubject_process/[process_fk=id]process")
                                .type(FeatureSchema.Type.OBJECT_ARRAY)
                                .objectType("Link")
                                .label("Versuche")
                                .constraints(new ImmutableConstraints.Builder()
                                        .minOccurrence(1)
                                        .maxOccurrence(3)
                                        .build())
                                .putProperties2("title", new ImmutableFeaturePropertyV2.Builder()
                                        .path("category_fk")
                                )
                                .putProperties2("href", new ImmutableFeaturePropertyV2.Builder()
                                        .path("id")
                                        .constraints(new ImmutableConstraints.Builder()
                                                .required(true)
                                                .build())
                                )
                        )
                        .putProperties2("filterValues", new ImmutableFeaturePropertyV2.Builder()
                                .path("[id=observationsubject_fk]observationsubject_filtervalues")
                                .type(FeatureSchema.Type.OBJECT_ARRAY)
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
                                        .type(FeatureSchema.Type.FLOAT)
                                        .label("Messwert")
                                        .constraints(new ImmutableConstraints.Builder()
                                                .min(0.0)
                                                .max(100.0)
                                                .build())
                                )
                                .putProperties2("classification", new ImmutableFeaturePropertyV2.Builder()
                                        .path("[id=filtervalue_fk]filtervalue_resultclassification/classificationcode_fk")
                                        .type(FeatureSchema.Type.VALUE_ARRAY)
                                        .valueType(FeatureSchema.Type.STRING)
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
