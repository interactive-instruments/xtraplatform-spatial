/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources

class FeaturePathFixtures {

    private static final ObjectMapper YAML = YamlSerialization.createYamlMapper();

    public static Set<List<String>> fromYaml(String name) {
        def resource = Resources.getResource("feature-paths/" + name + ".yml");
        return YAML.readValue(Resources.toByteArray(resource), LinkedHashSet.class);
    }
}
