package de.ii.xtraplatform.features.sql.app

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import de.ii.xtraplatform.features.domain.YamlSerialization

class SqlQueryFixtures {
    private static final ObjectMapper YAML = YamlSerialization.createYamlMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<List<String>>() {
    };

    public static List<String> fromYaml(String name) {
        def resource = Resources.getResource("sql-queries/" + name + ".yml");
        return YAML.readValue(Resources.toByteArray(resource), STRING_LIST);
    }
}
