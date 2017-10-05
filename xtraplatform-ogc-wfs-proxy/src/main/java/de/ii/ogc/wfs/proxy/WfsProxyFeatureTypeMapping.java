/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.ogc.wfs.proxy;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public class WfsProxyFeatureTypeMapping {
    // TODO: multiplicity
    private Map<String, Map<String, List<TargetMapping>>> mappings;

    public WfsProxyFeatureTypeMapping() {
        this.mappings = new LinkedHashMap<>();
    }

    public void addMapping(String path, String targetType, TargetMapping targetMapping) {
        if (!mappings.containsKey(path)) {
            mappings.put(path, new HashMap<String, List<TargetMapping>>());
        }
        if (!mappings.get(path).containsKey(targetType)) {
            mappings.get(path).put(targetType, new ArrayList<TargetMapping>());
        }
        mappings.get(path).get(targetType).add(targetMapping);
    }

    public List<TargetMapping> findMappings(String path, String targetType) {
        if (mappings.containsKey(path) && mappings.get(path).containsKey(targetType)) {
               return mappings.get(path).get(targetType);
        }
        return ImmutableList.<TargetMapping>of();
    }

    public Map<String, List<TargetMapping>> findMappings(String targetType) {
        Map<String, List<TargetMapping>> mappings = new HashMap<>();

        for (String path: getMappings().keySet()) {
            if (getMappings().get(path).containsKey(targetType)) {
                mappings.put(path, getMappings().get(path).get(targetType));
            }
        }

        return mappings;
    }

    public Map<String, Map<String, List<TargetMapping>>> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, Map<String, List<TargetMapping>>> mappings) {
        this.mappings = mappings;
    }
}
