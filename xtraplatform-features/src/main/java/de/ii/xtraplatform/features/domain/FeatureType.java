/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilder;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueInstance;
import de.ii.xtraplatform.entity.api.maptobuilder.encoding.ValueBuilderMapEncodingEnabled;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@ValueBuilderMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureType.Builder.class)
public interface FeatureType extends ValueInstance {

    abstract static class Builder implements ValueBuilder<FeatureType> {
        public abstract ImmutableFeatureType.Builder putProperties(String key,
                                                                   ImmutableFeatureProperty.Builder builder);

        @JsonAnySetter
        @JsonProperty(value = "properties")
        public ImmutableFeatureType.Builder putProperties2(String key, ImmutableFeatureProperty.Builder builder) {
            return putProperties(key, builder.name(key));
        }
    }

    @Override
    default ImmutableFeatureType.Builder toBuilder() {
        return new ImmutableFeatureType.Builder().from(this);
    }

    @JsonAnyGetter
    //public abstract Map<String, SourcePathMapping> getMappings();

    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    ValueBuilderMap<FeatureProperty, ImmutableFeatureProperty.Builder> getProperties();

    @JsonIgnore
    String getName();

    //TODO
    @JsonIgnore
    @Value.Derived
    default Map<List<String>, List<FeatureProperty>> getPropertiesByPath() {
        Map<List<String>, List<FeatureProperty>> builder = new LinkedHashMap<>();

        getProperties().values()
                       .forEach(featureProperty -> {
                           //TODO
                           List<String> path = Splitter.on('/')
                                                       .omitEmptyStrings()
                                                       .splitToList(featureProperty.getPath().replaceAll("\\{.*?\\}", ""))
                                                       .stream()
                                                       .map(element -> {
                                                           String resolvedElement = element.replaceAll("\\{.*?\\}", "");

                                                           for (Map.Entry<String, String> entry : getAdditionalInfo().entrySet()) {
                                                               String prefix = entry.getKey();
                                                               String uri = entry.getValue();
                                                               resolvedElement = resolvedElement.replaceAll(prefix + ":", uri + ":");
                                                           }

                                                           return resolvedElement;
                                                       })
                                                       .collect(Collectors.toList());

                           //TODO: implement double col support as provider transformer and remove this
                           String root = path.get(0);
                           String column = path.get(path.size() - 1);
                           if (column.contains(":") && !root.contains(":")) {
                               List<String> columns = Splitter.on(':')
                                                              .splitToList(column);

                               List<String> parentPath = path.subList(0, path.size()-1);

                               List<String> path1 = new ImmutableList.Builder<String>().addAll(parentPath).add(columns.get(0)).build();
                               List<String> path2 = new ImmutableList.Builder<String>().addAll(parentPath).add(columns.get(1)).build();

                               builder.putIfAbsent(path1, new ArrayList<>());
                               builder.get(path1)
                                      .add(featureProperty);
                               builder.putIfAbsent(path2, new ArrayList<>());
                               builder.get(path2)
                                      .add(featureProperty);

                               return;
                           }


                           builder.putIfAbsent(path, new ArrayList<>());

                           builder.get(path)
                                  .add(featureProperty);
                       });

        return builder;
    }

    Map<String, String> getAdditionalInfo();


    default List<FeatureProperty> findPropertiesForPath(List<String> path) {
        return getPropertiesByPath().getOrDefault(path, ImmutableList.of());
    }

}
