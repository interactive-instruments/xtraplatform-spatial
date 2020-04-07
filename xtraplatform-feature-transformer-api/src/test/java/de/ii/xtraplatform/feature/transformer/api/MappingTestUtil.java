/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import io.dropwizard.jackson.Jackson;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MappingTestUtil {

    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private static final TypeReference<HashMap<String, Object>> GENERIC_MAP = new TypeReference<HashMap<String, Object>>() {
    };

    enum TYPES {UNKNOWN, ID}

    public static Map<String, FeatureTypeMapping> readFeatureTypeMappings(Path serviceFile) throws IOException {
        Map<String, Object> mappings = readMappings(serviceFile);
        Map<String, List<String>> paths = readMappingPaths(mappings);

        return toFeatureTypeMappings(mappings);
    }

    private static int comparePathsByPriority(Map.Entry<String, Optional<Integer>> path1,
                                              Map.Entry<String, Optional<Integer>> path2) {
        return !path1.getValue()
                     .isPresent() ? 1 : !path2.getValue()
                                              .isPresent() ? -1 : path1.getValue()
                                                                       .get() - path2.getValue()
                                                                                     .get();
    }

    private static Map.Entry<String, Optional<Integer>> extractPathAndSortPriority(Map.Entry<String, Object> mapping) {
        Map<String, Object> s2 = (Map<String, Object>) mapping.getValue();
        Map<String, Object> s22 = (Map<String, Object>) s2.get("general");
        Optional<Integer> s222 = Optional.ofNullable(s22.get("sortPriority"))
                                         .map(o -> ((Integer) o));

        return new AbstractMap.SimpleImmutableEntry<>(mapping.getKey(), s222);
    }

    private static Map.Entry<String, List<String>> extractSortedPaths(Map.Entry<String, Object> featureType) {
        Map<String, Object> featureTypeMappings = (Map<String, Object>) featureType.getValue();

        List<String> sortedPaths = featureTypeMappings.entrySet()
                                                      .stream()
                                                      .map(MappingTestUtil::extractPathAndSortPriority)
                                                      .sorted(MappingTestUtil::comparePathsByPriority)
                                                      .map(Map.Entry::getKey)
                                                      .collect(Collectors.toList());

        return new AbstractMap.SimpleImmutableEntry<>(featureType.getKey(), sortedPaths);
    }

    private static Map<String, Object> readMappings(Path serviceFile) throws IOException {

        Map<String, Object> service = MAPPER.readValue(serviceFile.toFile(), GENERIC_MAP);
        Map<String, Object> featureProvider = (Map<String, Object>) service.get("featureProvider");
        Map<String, Object> allMappings = (Map<String, Object>) featureProvider.get("mappings");

        return allMappings;
    }

    private static Map<String, List<String>> readMappingPaths(Map<String, Object> mappings) throws IOException {

        return mappings.entrySet()
                       .stream()
                       .map(MappingTestUtil::extractSortedPaths)
                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, FeatureTypeMapping> toFeatureTypeMappings(Map<String, Object> mappings) {
        return mappings.entrySet()
                       .stream()
                       .map(featureTypePaths -> {
                           Map<String, SourcePathMapping> sourcePathMappings = toSourcePathMappings((Map<String, Object>) featureTypePaths.getValue());

                           FeatureTypeMapping featureTypeMapping = new ImmutableFeatureTypeMapping.Builder().putAllMappings(sourcePathMappings)
                                                                                                            .build();

                           return new AbstractMap.SimpleImmutableEntry<>(featureTypePaths.getKey(), featureTypeMapping);
                       })
                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, SourcePathMapping> toSourcePathMappings(Map<String, Object> mappings) {
        return mappings.entrySet()
                       .stream()
                       .map(stringObjectEntry -> {

                           Map<String, Object> value = (Map<String, Object>) stringObjectEntry.getValue();
                           Map<String, Object> general = (Map<String, Object>) value.get(TargetMapping.BASE_TYPE);

                           ImmutableSourcePathMapping sourcePathMapping = new ImmutableSourcePathMapping.Builder().putMappings(TargetMapping.BASE_TYPE, new TargetMapping() {


                               @Nullable
                               @Override
                               public String getName() {
                                   return null;
                               }

                               @Nullable
                               @Override
                               public Enum getType() {
                                   return Optional.ofNullable(general.get("type"))
                                                  .map(val -> Objects.equals(val, "ID"))
                                                  .orElse(false) ? TYPES.ID : TYPES.UNKNOWN;
                               }

                               @Nullable
                               @Override
                               public Boolean getEnabled() {
                                   return true;
                               }

                               @Nullable
                               @Override
                               public Integer getSortPriority() {
                                   return (Integer) general.get("sortPriority");
                               }

                               @Nullable
                               @Override
                               public String getFormat() {
                                   return null;
                               }

                               @Override
                               public boolean isSpatial() {
                                   return false;
                               }
                           })
                                                                                                                  .build();
                           return new AbstractMap.SimpleImmutableEntry<>(stringObjectEntry.getKey(), sourcePathMapping);
                       })
                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
