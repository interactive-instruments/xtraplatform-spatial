package de.ii.xtraplatform.features.domain

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import de.ii.xtraplatform.base.domain.JacksonProvider

class YamlSerialization {

    //TODO: from jackson or store?
    static ObjectMapper createYamlMapper() {
        def jackson = new JacksonProvider(() -> Set.of())

        def yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
                .disable(YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS)

        def objectMapper = jackson.getNewObjectMapper(yamlFactory)
                .enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)

        return objectMapper;
    }
}
