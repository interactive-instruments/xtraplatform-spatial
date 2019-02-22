/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.geojson;

import akka.japi.Pair;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zahnen
 */
public class MappingSwapper {
    public static final String UNDERSCORE_REPLACEMENT = "xyz";

    public String getSourceFormat() {
        return "general";
    }

    public Map<String, FeatureTypeMapping> swapMappings(Map<String, FeatureTypeMapping> mappings, String targetFormat) {
        Map<String, FeatureTypeMapping> swappedMappings = new LinkedHashMap<>();

        mappings.forEach((featureType, ftMappings) -> {
            swappedMappings.put(featureType, swapMapping(ftMappings, targetFormat));
        });

        return swappedMappings;
    }

    public FeatureTypeMapping swapMapping(FeatureTypeMapping mappings, String targetFormat) {
        ImmutableFeatureTypeMapping.Builder builder = ImmutableFeatureTypeMapping.builder();

        mappings.getMappings()
                .forEach((path, sourceMappings) -> {

                    sourceMappings.getMappings()
                                  .entrySet()
                                  .stream()
                                  .filter(targetMappingEntry -> targetMappingEntry.getKey()
                                                                                  .equals(getSourceFormat()) && Objects.nonNull(targetMappingEntry.getValue().getName()))
                                  .forEach(targetMappingEntry -> {
                                      Pair<String, String> swappedPaths = swapMultiplicities(path, targetMappingEntry.getValue()
                                                                                                                     .getName(), targetMappingEntry.getValue().isSpatial());

                                      builder.putMappings(swappedPaths.first(), ImmutableSourcePathMapping.builder()
                                                                                                          .putMappings(targetFormat, new MappingReadFromWrite(swappedPaths.second(), formatToRegex(targetMappingEntry.getValue().getFormat())))
                                                                                                          .build());
                                  });
                });
        return builder.build();
    }

    private Pair<String, String> swapMultiplicities(String sourcePath, String jsonPath, boolean isSpatial) {
        String newSourcePath = sourcePath;
        String newJsonPath = jsonPath;

        for (String element : Splitter.on('.')
                                      .splitToList(jsonPath)) {
            if (element.contains(("["))) {
                String elemWithoutRef = element.substring(0, element.indexOf("["));
                String ref = element.substring(element.indexOf("[") + 1, element.length() - 1);

                newJsonPath = newJsonPath.replace(element, elemWithoutRef);
                newSourcePath = newSourcePath.replaceFirst("([/\\]])" + ref + "/", "$1" + ref + "[" + elemWithoutRef + "]/");
            }
        }

        if (isSpatial) {
            newJsonPath = "geometry";
            newSourcePath = geomToWkt(newSourcePath);
        }

        return new Pair<>(newJsonPath.replace('.', '/'), newSourcePath);
    }

    private String formatToRegex(String format) {
        return Objects.nonNull(format) ? format.replaceAll("(?<=\\{\\{)(\\w+)_(\\w+)(?=\\}\\})", "$1" + UNDERSCORE_REPLACEMENT + "$2").replaceAll("\\{\\{(\\w+)\\}\\}", "(?<$1>.+)").replaceAll("/", "\\\\/") : null;
    }


    // TODO: also in FeatureProviderPgis, consolidate
    private String geomToWkt(String path) {
        int sep = path.lastIndexOf("/") + 1;
        return path.substring(0, sep) + "ST_AsText(ST_ForcePolygonCCW(" + path.substring(sep) + "))";
    }

    public static class MappingReadFromWrite implements TargetMapping<MappingReadFromWrite.DOES_NOT_MATTER> {

        private final String name;
        private final String format;

        public MappingReadFromWrite(String name, String format) {
            this.name = name;
            this.format = format;
        }

        @Nullable
        @Override
        public String getName() {
            return name;
        }

        @Nullable
        @Override
        public DOES_NOT_MATTER getType() {
            return null;
        }

        @Nullable
        @Override
        public Boolean getEnabled() {
            return null;
        }

        @Nullable
        @Override
        public Integer getSortPriority() {
            return null;
        }

        @Nullable
        @Override
        public String getFormat() {
            return format;
        }

        @Override
        public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
            return null;
        }

        @Override
        public boolean isSpatial() {
            return false;
        }

        enum DOES_NOT_MATTER {}

    }
}
