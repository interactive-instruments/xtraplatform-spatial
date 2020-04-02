/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilder;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueInstance;
import de.ii.xtraplatform.entity.api.maptobuilder.encoding.ValueBuilderMapEncodingEnabled;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import org.immutables.value.Value;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@ValueBuilderMapEncodingEnabled
//TODO: @JsonAnySetter not generated for ModifiableFeatureTypeMapping
//TODO: map order only sustained with Builder
@JsonDeserialize(builder = ImmutableFeatureTypeMapping.Builder.class)
//@JsonDeserialize(as = ModifiableFeatureTypeMappingAnySetter.class)
public abstract class FeatureTypeMapping implements ValueInstance {

    private static final Splitter SPLITTER_NS_HTTP = Splitter.onPattern("\\/(?=http)");
    private static final Splitter SPLITTER_NS_HTTPS = Splitter.onPattern("\\/(?=https)");
    private static final Splitter SPLITTER_DEFAULT = Splitter.on("/");
    public static final String NS_HTTP = "http://";
    public static final String NS_HTTPS = "https://";

    abstract static class Builder implements ValueBuilder<FeatureTypeMapping> {
        //@JsonAnySetter
        //public abstract ImmutableFeatureTypeMapping.Builder putMappings(String key, SourcePathMapping value);

        @JsonAnySetter
        public abstract ImmutableFeatureTypeMapping.Builder putMappings(String key, ImmutableSourcePathMapping.Builder builder);
    }

    @Override
    public ImmutableFeatureTypeMapping.Builder toBuilder() {
        return new ImmutableFeatureTypeMapping.Builder().from(this);
    }

    @JsonAnyGetter
    //public abstract Map<String, SourcePathMapping> getMappings();

    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    public abstract ValueBuilderMap<SourcePathMapping, ImmutableSourcePathMapping.Builder> getMappings();

    @JsonIgnore
    @Value.Derived
    public Map<List<String>, SourcePathMapping> getMappingsWithPathAsList() {
        return getMappings().entrySet()
                            .stream()
                            .map(this::pathToList)
                            .flatMap(this::splitDoubleColumnPath)
                            .map(this::mergeBaseMapping)
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // TODO: use prefixes for gml paths, split on / only
    private List<String> splitPath(String path) {
            Splitter splitter = path.contains(NS_HTTP) ? SPLITTER_NS_HTTP : path.contains(NS_HTTPS) ? SPLITTER_NS_HTTPS : SPLITTER_DEFAULT;
        return splitter.omitEmptyStrings()
                       .splitToList(path);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return getMappings() == null || getMappings().isEmpty();
    }

    public Optional<TargetMapping> findMappings(String path, String targetType) {
        return getMappings().containsKey(path) ? findMappings(getMappings().get(path), targetType) : Optional.empty();
    }

    public Optional<TargetMapping> findMappings(List<String> path, String targetType) {
        return getMappingsWithPathAsList().containsKey(path) ? findMappings(getMappingsWithPathAsList().get(path), targetType) : Optional.empty();
    }

    private Optional<TargetMapping> findMappings(SourcePathMapping mapping, String targetType) {
        if (mapping.hasMappingForType(targetType)) {
            TargetMapping targetMapping = mapping.getMappingForType(targetType);

            //TODO
            /*if (mapping.hasMappingForType(TargetMapping.BASE_TYPE)) {
                TargetMapping baseMapping = mapping.getMappingForType(TargetMapping.BASE_TYPE);
                targetMapping = targetMapping.mergeCopyWithBase(baseMapping);
            }*/

            if (targetMapping.isEnabled()) {
                return Optional.of(targetMapping);
            }
        }
        return Optional.empty();
    }

    //TODO: only used once, check
    public Map<String, TargetMapping> findMappings(String targetType) {
        Map<String, TargetMapping> mappings = new LinkedHashMap<>();

        for (String path : getMappings().keySet()) {
            if (getMappings().get(path)
                             .hasMappingForType(targetType)) {
                TargetMapping targetMapping = getMappings().get(path)
                                                           .getMappingForType(targetType);

                //TODO
                if (!targetType.equals(TargetMapping.BASE_TYPE) && getMappings().get(path)
                                                                                .hasMappingForType(TargetMapping.BASE_TYPE)) {
                    TargetMapping baseMapping = getMappings().get(path)
                                                             .getMappingForType(TargetMapping.BASE_TYPE);
                    targetMapping = targetMapping.mergeCopyWithBase(baseMapping);
                }

                mappings.put(path, targetMapping);
            }
        }

        return mappings;
    }

    private Function<String, Stream<String>> splitDoubleColumn() {
        return column -> column.startsWith("http://") ? Stream.of(column) : Splitter.on(':')
                                                                                    .omitEmptyStrings()
                                                                                    .splitToList(column)
                                                                                    .stream();
    }


    private Stream<Map.Entry<List<String>, SourcePathMapping>> splitDoubleColumnPath(
            Map.Entry<List<String>, SourcePathMapping> entry) {
        Optional<String> column = getColumnElement(entry.getKey());
        if (column.isPresent() && column.get()
                                        .contains(":")) {
            return splitDoubleColumn().apply(column.get())
                                      .map(col -> new AbstractMap.SimpleImmutableEntry<>(ImmutableList.<String>builder().addAll(entry.getKey()
                                                                                                                                     .subList(0, entry.getKey()
                                                                                                                                                      .size() - 1))
                                                                                                                        .add(col)
                                                                                                                        .build(), entry.getValue()));
        }
        return Stream.of(entry);
    }

    private Map.Entry<List<String>, SourcePathMapping> mergeBaseMapping(
            Map.Entry<List<String>, SourcePathMapping> entry) {
        SourcePathMapping oldMappings = entry.getValue();
        Map<String, TargetMapping> mergedMappings = oldMappings.getMappings()
                                                               .entrySet()
                                                               .stream()
                                                               .map(stringTargetMappingEntry -> {

                                                                   TargetMapping mergedMapping = stringTargetMappingEntry.getValue();

                                                                   if (oldMappings.hasMappingForType(TargetMapping.BASE_TYPE)) {
                                                                       TargetMapping baseMapping = oldMappings.getMappingForType(TargetMapping.BASE_TYPE);
                                                                       mergedMapping = mergedMapping.mergeCopyWithBase(baseMapping);
                                                                   }

                                                                   return new AbstractMap.SimpleImmutableEntry<>(stringTargetMappingEntry.getKey(), mergedMapping);
                                                               })
                                                               .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), new ImmutableSourcePathMapping.Builder().putAllMappings(mergedMappings)
                                                                                                              .build());
    }

    private Optional<String> getColumnElement(List<String> path) {
        return path.size() <= 1 ? Optional.empty() : Optional.of(path.get(path.size() - 1));
    }

    private Map.Entry<List<String>, SourcePathMapping> pathToList(Map.Entry<String, SourcePathMapping> entry) {
        return new AbstractMap.SimpleImmutableEntry<>(splitPath(entry.getKey()), entry.getValue());
    }


    /*public Map<String, Map<String, TargetMapping>> getMappings() {
        return mappings;
    }

    void setMappings(Map<String, Map<String, TargetMapping>> mappings) {
        this.mappings.putAll(mappings);
    }*/

    //TODO: legacy
    /*void setMappings(Map<String, Map<String, List<TargetMapping>>> mappings) {
        this.mappings.putAll(mappings);

        mappings.keySet().forEach(key -> {
            // TODO
            List<String> pathAsList = Splitter.onPattern("\\/(?=http)").splitToList(key);
            mappings2.put(pathAsList, mappings.get(key));
        });
    }*/
}
