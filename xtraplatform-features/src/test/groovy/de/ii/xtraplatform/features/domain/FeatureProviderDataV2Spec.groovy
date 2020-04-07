package de.ii.xtraplatform.features.domain

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import de.ii.xtraplatform.crs.domain.OgcCrs
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

        objectMapper = new ObjectMapper(yamlFactory)
                .enable(SerializationFeature.INDENT_OUTPUT)
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
                .putTypes2("observationsubject", new ImmutableFeatureTypeV2.Builder()
                        .putProperties2("id", new ImmutableFeaturePropertyV2.Builder()
                                .path("id")
                                .role(FeaturePropertyV2.Role.ID)
                        )
                        .putProperties2("explorationSite", new ImmutableFeaturePropertyV2.Builder()
                                .path("[explorationsite_fk=id]explorationsite")
                                .putProperties2("title", new ImmutableFeaturePropertyV2.Builder()
                                    .path("shortname")
                                )
                        )
                )
                .build()

        when:

        def dataAsYaml = objectMapper.writeValueAsString(data)

        then:

        def expected = new File("src/test/resources/geoval.yml").text

        dataAsYaml == expected

    }
}
