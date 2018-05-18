/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 */
package de.ii.xtraplatform.feature.query.api;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class WfsProxyFeatureTypeMapping {
    // TODO: multiplicity
    private final Map<String, Map<String, List<TargetMapping>>> mappings;

    private final Map<List<String>, Map<String, List<TargetMapping>>> mappings2;

    WfsProxyFeatureTypeMapping() {
        this.mappings = new LinkedHashMap<>();
        this.mappings2 = new LinkedHashMap<>();
    }

    public boolean isEmpty() {
        return mappings.isEmpty();
    }

    public WfsProxyFeatureTypeMapping(Map<String, Map<String, List<TargetMapping>>> mappings) {
        this.mappings = mappings;
    }

    public void addMapping(String path, String targetType, TargetMapping targetMapping) {
        if (!mappings.containsKey(path)) {
            mappings.put(path, new LinkedHashMap<String, List<TargetMapping>>());
        }
        if (!mappings.get(path).containsKey(targetType)) {
            mappings.get(path).put(targetType, new ArrayList<TargetMapping>());
        }
        mappings.get(path).get(targetType).add(targetMapping);
    }

    public List<TargetMapping> findMappings(String path, String targetType) {
        if (mappings.containsKey(path) && mappings.get(path).containsKey(targetType)) {
            List<TargetMapping> mappingList = mappings.get(path).get(targetType);

            //TODO
            if (mappings.get(path).containsKey(TargetMapping.BASE_TYPE)) {
                TargetMapping baseMapping = mappings.get(path).get(TargetMapping.BASE_TYPE).get(0);
                List<TargetMapping> mergedMappingList = new ArrayList<>();

                for (TargetMapping targetMapping : mappingList) {
                    mergedMappingList.add(targetMapping.mergeCopyWithBase(baseMapping));
                }

                mappingList = mergedMappingList;
            }

            return mappingList;
        }
        return ImmutableList.<TargetMapping>of();
    }
    public List<TargetMapping> findMappings2(List<String> path, String targetType) {
        if (mappings2.containsKey(path) && mappings2.get(path).containsKey(targetType)) {
            List<TargetMapping> mappingList = mappings2.get(path).get(targetType);

            //TODO
            if (mappings2.get(path).containsKey(TargetMapping.BASE_TYPE)) {
                TargetMapping baseMapping = mappings2.get(path).get(TargetMapping.BASE_TYPE).get(0);
                return mappingList.stream()
                        .map(mapping -> mapping.mergeCopyWithBase(baseMapping))
                        .collect(Collectors.toList());
            }

            return mappingList;
        }
        return ImmutableList.<TargetMapping>of();
    }

    public Map<String, List<TargetMapping>> findMappings(String targetType) {
        Map<String, List<TargetMapping>> mappings = new HashMap<>();

        for (String path : getMappings().keySet()) {
            if (getMappings().get(path).containsKey(targetType)) {
                List<TargetMapping> mappingList = getMappings().get(path).get(targetType);

                //TODO
                if (!targetType.equals(TargetMapping.BASE_TYPE) && getMappings().get(path).containsKey(TargetMapping.BASE_TYPE)) {
                    TargetMapping baseMapping = getMappings().get(path).get(TargetMapping.BASE_TYPE).get(0);
                    List<TargetMapping> mergedMappingList = new ArrayList<>();

                    for (TargetMapping targetMapping: mappingList) {
                        mergedMappingList.add(targetMapping.mergeCopyWithBase(baseMapping));
                    }

                    mappingList = mergedMappingList;
                }


                mappings.put(path, mappingList);
            }
        }

        return mappings;
    }

    private Map<String, Map<String, List<TargetMapping>>> getMappings() {
        return mappings;
    }

    void setMappings(Map<String, Map<String, List<TargetMapping>>> mappings) {
        this.mappings.putAll(mappings);

        mappings.keySet().forEach(key -> {
            // TODO
            List<String> pathAsList = Splitter.onPattern("\\/(?=http)").splitToList(key);
            mappings2.put(pathAsList, mappings.get(key));
        });
    }
}
