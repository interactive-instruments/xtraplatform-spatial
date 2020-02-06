/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.entity.api.maptobuilder.encoding.ValueBuilderMapEncodingEnabled;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author zahnen
 */
//@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "providerType", visible = true)
//@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@ValueBuilderMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureProviderDataV1.Builder.class)
public interface FeatureProviderDataV1 extends EntityData {

    abstract class Builder implements EntityDataBuilder<FeatureProviderDataV1> {
        public abstract ImmutableFeatureProviderDataV1.Builder putTypes(String key, ImmutableFeatureType.Builder builder);

        @JsonProperty(value = "types")
        public ImmutableFeatureProviderDataV1.Builder putTypes2(String key, ImmutableFeatureType.Builder builder) {
            return putTypes(key, builder.name(key));
        }

        @JsonIgnore
        public abstract Map<String, ImmutableFeatureType.Builder> getTypes();

        @JsonProperty(value = "types")
        public Map<String, ImmutableFeatureType.Builder> getTypes2() {
            Map<String, ImmutableFeatureType.Builder> types = getTypes();

            return new Map<String, ImmutableFeatureType.Builder>() {
                @Override
                public int size() {
                    return types.size();
                }

                @Override
                public boolean isEmpty() {
                    return types.isEmpty();
                }

                @Override
                public boolean containsKey(Object o) {
                    return types.containsKey(o);
                }

                @Override
                public boolean containsValue(Object o) {
                    return types.containsValue(o);
                }

                @Override
                public ImmutableFeatureType.Builder get(Object o) {
                    return types.get(o);
                }

                @Override
                public ImmutableFeatureType.Builder put(String s, ImmutableFeatureType.Builder builder) {
                    return types.put(s, builder.name(s));
                }

                @Override
                public ImmutableFeatureType.Builder remove(Object o) {
                    return types.remove(o);
                }

                @Override
                public void putAll(Map<? extends String, ? extends ImmutableFeatureType.Builder> map) {
                    types.putAll(map);
                }

                @Override
                public void clear() {
                    types.clear();
                }

                @Override
                public Set<String> keySet() {
                    return types.keySet();
                }

                @Override
                public Collection<ImmutableFeatureType.Builder> values() {
                    return types.values();
                }

                @Override
                public Set<Entry<String, ImmutableFeatureType.Builder>> entrySet() {
                    return types.entrySet();
                }
            };

            //return new LinkedHashMap<String, ImmutableFeatureType.Builder>(types){};
        }
    }

    String getProviderType();

    String getFeatureProviderType();

    ConnectionInfo getConnectionInfo();

    EpsgCrs getNativeCrs();

    //behaves exactly like Map<String, FeatureTypeMapping>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    ValueBuilderMap<FeatureType, ImmutableFeatureType.Builder> getTypes();

    //TODO
    @Value.Default
    default ImmutableMappingStatus getMappingStatus() {
        return new ImmutableMappingStatus.Builder()
                .enabled(true)
                .supported(true)
                .refined(true)
                .build();
    }



    //TODO
    @Value.Default
    default String getConnectorType() {
        return getConnectionInfo().getConnectorType();
    }

    //TODO
    @JsonIgnore
    @Value.Default
    default Optional<String> getDataSourceUrl() {
        return getConnectionInfo().getConnectionUri();
    }
}
