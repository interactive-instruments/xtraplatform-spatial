/*
 * Copyright 2021 interactive instruments GmbH
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
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import spock.lang.Shared
import spock.lang.Specification

class FeatureSchemaToTypeVisitorSpec extends Specification {

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

        FeatureProviderDataV2 data = new ImmutableFeatureProviderCommonData.Builder()
                .id("geoval")
                .createdAt(1586271491161)
                .lastModified(1586271491161)
                .providerType("FEATURE")
                .featureProviderType("SQL")
                .connectionInfo(FeatureProviderDataFixtures.connectionInfo)
                .nativeCrs(OgcCrs.CRS84)
                .defaultLanguage("de")
                .putTypes2("observationsubject", new ImmutableFeatureSchema.Builder()
                        .sourcePath("/observationsubject")
                        .label("Untersuchungsobjekt")
                        .description("Ein Untersuchungsobjekt ist entweder eine Probe für Untersuchungen im Labor oder eine In-Situ-Messung")
                        .putProperties2("id", new ImmutableFeatureSchema.Builder()
                                .sourcePath("id")
                                .role(SchemaBase.Role.ID)
                                .label("Objekt-Id")
                                .description("Eindeutige Id des Objekts")
                                .constraints(new ImmutableSchemaConstraints.Builder()
                                        .regex("[a-zA-Z0-9_]{3,}")
                                        .build())
                        )
                        .putProperties2("type", new ImmutableFeatureSchema.Builder()
                                .sourcePath("_type")
                                .label("Art")
                                .constraints(new ImmutableSchemaConstraints.Builder()
                                        .codelist("geoval_type")
                                        .enumValues(ImmutableList.of("Borehole", "TrialPit"))
                                        .build())
                        )
                        .putProperties2("shortName", new ImmutableFeatureSchema.Builder()
                                .sourcePath("shortname")
                                .transformations(new ImmutablePropertyTransformation.Builder().codelist("nullValues").build())
                        )
                        .putProperties2("geomLowerPoint", new ImmutableFeatureSchema.Builder()
                                .sourcePath("geomlowerpoint")
                                .type(SchemaBase.Type.GEOMETRY)
                                .geometryType(SimpleFeatureGeometry.POINT)
                        )
                        .putProperties2("explorationSite", new ImmutableFeatureSchema.Builder()
                                .sourcePath("[explorationsite_fk=id]explorationsite")
                                .type(SchemaBase.Type.OBJECT)
                                .objectType("Link")
                                .label("Aufschlußpunkt")
                                .putProperties2("title", new ImmutableFeatureSchema.Builder()
                                        .sourcePath("shortname")
                                )
                                .putProperties2("href", new ImmutableFeatureSchema.Builder()
                                        .sourcePath("id")
                                        .constraints(new ImmutableSchemaConstraints.Builder()
                                                .required(true)
                                                .build()))
                        )
                        .putProperties2("process", new ImmutableFeatureSchema.Builder()
                                .sourcePath("[id=observationsubject_fk]observationsubject_process/[process_fk=id]process")
                                .type(SchemaBase.Type.OBJECT_ARRAY)
                                .objectType("Link")
                                .label("Versuche")
                                .constraints(new ImmutableSchemaConstraints.Builder()
                                        .minOccurrence(1)
                                        .maxOccurrence(3)
                                        .build())
                                .putProperties2("title", new ImmutableFeatureSchema.Builder()
                                        .sourcePath("category_fk")
                                )
                                .putProperties2("href", new ImmutableFeatureSchema.Builder()
                                        .sourcePath("id")
                                        .constraints(new ImmutableSchemaConstraints.Builder()
                                                .required(true)
                                                .build())
                                )
                        )
                        .putProperties2("filterValues", new ImmutableFeatureSchema.Builder()
                                .sourcePath("[id=observationsubject_fk]observationsubject_filtervalues")
                                .type(SchemaBase.Type.OBJECT_ARRAY)
                                .objectType("FilterValue")
                                .label("Leitkennwerte")
                                .putProperties2("property", new ImmutableFeatureSchema.Builder()
                                        .sourcePath("filtervalueproperty_fk")
                                        .label("Eigenschaft")
                                )
                                .putProperties2("property", new ImmutableFeatureSchema.Builder()
                                        .sourcePath("filtervalueproperty_fk")
                                        .label("Eigenschaft")
                                )
                                .putProperties2("measure", new ImmutableFeatureSchema.Builder()
                                        .sourcePath("resultmeasure")
                                        .type(SchemaBase.Type.FLOAT)
                                        .label("Messwert")
                                        .constraints(new ImmutableSchemaConstraints.Builder()
                                                .min(0.0)
                                                .max(100.0)
                                                .build())
                                )
                                .putProperties2("classification", new ImmutableFeatureSchema.Builder()
                                        .sourcePath("[id=filtervalue_fk]filtervalue_resultclassification/classificationcode_fk")
                                        .type(SchemaBase.Type.VALUE_ARRAY)
                                        .valueType(SchemaBase.Type.STRING)
                                        .label("Klassifikationen")
                                )
                        )
                )
                .build()

        when:

        FeatureType type = data.getTypes().get("observationsubject").accept(new FeatureSchemaToTypeVisitor("observationsubject"))

        FeatureProviderDataV1 migrated = new ImmutableFeatureProviderDataV1.Builder()
                .id("geoval")
                .createdAt(1586271491161)
                .lastModified(1586271491161)
                .providerType("FEATURE")
                .featureProviderType("SQL")
                .connectionInfo(FeatureProviderDataFixtures.connectionInfo)
                .nativeCrs(OgcCrs.CRS84)
                .putTypes("observationsubject", type)
                .build()

        then:

        def expected = new ImmutableFeatureProviderDataV1.Builder()
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
                                .type(FeatureProperty.Type.STRING)
                                .role(FeatureProperty.Role.ID)
                        )
                        .putProperties2("type", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/_type")
                                .type(FeatureProperty.Type.STRING)
                        )
                        .putProperties2("shortName", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/shortname")
                                .type(FeatureProperty.Type.STRING)
                        )
                        .putProperties2("geomLowerPoint", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/geomlowerpoint")
                                .type(FeatureProperty.Type.GEOMETRY)
                        )
                        .putProperties2("explorationSite.title", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[explorationsite_fk=id]explorationsite/shortname")
                                .type(FeatureProperty.Type.STRING)
                                .additionalInfo(ImmutableMap.of("role", "LINKTITLE"))
                        )
                        .putProperties2("explorationSite.href", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[explorationsite_fk=id]explorationsite/id")
                                .type(FeatureProperty.Type.STRING)
                                .additionalInfo(ImmutableMap.of("role", "LINKHREF"))
                        )
                        .putProperties2("process[].title", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_process/[process_fk=id]process/category_fk")
                                .type(FeatureProperty.Type.STRING)
                                .additionalInfo(ImmutableMap.of("role", "LINKTITLE"))
                        )
                        .putProperties2("process[].href", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_process/[process_fk=id]process/id")
                                .type(FeatureProperty.Type.STRING)
                                .additionalInfo(ImmutableMap.of("role", "LINKHREF"))
                        )
                        .putProperties2("filterValues[].property", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_filtervalues/filtervalueproperty_fk")
                                .type(FeatureProperty.Type.STRING)
                        )
                        .putProperties2("filterValues[].measure", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_filtervalues/resultmeasure")
                                .type(FeatureProperty.Type.FLOAT)
                        )
                        .putProperties2("filterValues[].classification[]", new ImmutableFeatureProperty.Builder()
                                .path("/observationsubject/[id=observationsubject_fk]observationsubject_filtervalues/[id=filtervalue_fk]filtervalue_resultclassification/classificationcode_fk")
                                .type(FeatureProperty.Type.STRING)
                        )
                )
                .build()

        migrated == expected

    }

}
