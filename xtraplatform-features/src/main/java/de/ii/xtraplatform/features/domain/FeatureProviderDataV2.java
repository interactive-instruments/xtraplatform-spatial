/**
 * Copyright 2020 interactive instruments GmbH
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
import de.ii.xtraplatform.entities.domain.AutoEntity;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableMap;
import de.ii.xtraplatform.entities.domain.maptobuilder.encoding.BuildableMapEncodingEnabled;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@BuildableMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureProviderDataV2.Builder.class)
public interface FeatureProviderDataV2 extends EntityData, AutoEntity {

    @Override
    @Value.Derived
    default long getEntitySchemaVersion() {
        return 2;
    }

    String getProviderType();

    String getFeatureProviderType();

    @Value.Derived
    @Override
    default Optional<String> getEntitySubType() {
        return Optional.of(String.format("%s/%s", getProviderType(), getFeatureProviderType()).toLowerCase());
    }

    ConnectionInfo getConnectionInfo();

    Optional<EpsgCrs> getNativeCrs();

    Optional<String> getDefaultLanguage();

    //behaves exactly like Map<String, FeatureTypeMapping>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getTypes();

    Map<String, Map<String, String>> getCodelists();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default Optional<String> getDataSourceUrl() {
        return Optional.ofNullable(getConnectionInfo()).flatMap(ConnectionInfo::getConnectionUri);
    }


    // custom builder to automatically use keys of types as name of FeatureTypeV2
    abstract class Builder implements EntityDataBuilder<FeatureProviderDataV2> {
        @JsonIgnore
        public abstract Map<String, ImmutableFeatureSchema.Builder> getTypes();

        @JsonProperty(value = "types")
        public Map<String, ImmutableFeatureSchema.Builder> getTypes2() {
            Map<String, ImmutableFeatureSchema.Builder> types = getTypes();

            return new KeyToNameBuilderMap(types);

            //return new LinkedHashMap<String, ImmutableFeatureType.Builder>(types){};
        }

        public abstract ImmutableFeatureProviderDataV2.Builder putTypes(String key, ImmutableFeatureSchema.Builder builder);

        @JsonProperty(value = "types")
        public ImmutableFeatureProviderDataV2.Builder putTypes2(String key, ImmutableFeatureSchema.Builder builder) {
            return putTypes(key, builder.name(key));
        }

        private static class KeyToNameBuilderMap implements Map<String, ImmutableFeatureSchema.Builder> {
            private final Map<String, ImmutableFeatureSchema.Builder> types;

            public KeyToNameBuilderMap(Map<String, ImmutableFeatureSchema.Builder> types) {
                this.types = types;
            }

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
            public ImmutableFeatureSchema.Builder get(Object o) {
                return types.get(o);
            }

            @Override
            public ImmutableFeatureSchema.Builder put(String s, ImmutableFeatureSchema.Builder builder) {
                return types.put(s, builder.name(s));
            }

            @Override
            public ImmutableFeatureSchema.Builder remove(Object o) {
                return types.remove(o);
            }

            @Override
            public void putAll(Map<? extends String, ? extends ImmutableFeatureSchema.Builder> map) {
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
            public Collection<ImmutableFeatureSchema.Builder> values() {
                return types.values();
            }

            @Override
            public Set<Entry<String, ImmutableFeatureSchema.Builder>> entrySet() {
                return types.entrySet();
            }
        }
    }
}
