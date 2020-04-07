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

import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@ValueBuilderMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureProviderDataV2.Builder.class)
public interface FeatureProviderDataV2 extends EntityData {

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

    @Nullable //TODO: remove when done
    @JsonIgnore //TODO: remove when done
    ConnectionInfo getConnectionInfo();

    EpsgCrs getNativeCrs();

    //behaves exactly like Map<String, FeatureTypeMapping>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    ValueBuilderMap<FeatureTypeV2, ImmutableFeatureTypeV2.Builder> getTypes();




    // custom builder to automatically use keys of types as name of FeatureTypeV2
    abstract class Builder implements EntityDataBuilder<FeatureProviderDataV2> {
        @JsonIgnore
        public abstract Map<String, ImmutableFeatureTypeV2.Builder> getTypes();

        @JsonProperty(value = "types")
        public Map<String, ImmutableFeatureTypeV2.Builder> getTypes2() {
            Map<String, ImmutableFeatureTypeV2.Builder> types = getTypes();

            return new KeyToNameBuilderMap(types);

            //return new LinkedHashMap<String, ImmutableFeatureType.Builder>(types){};
        }

        public abstract ImmutableFeatureProviderDataV2.Builder putTypes(String key, ImmutableFeatureTypeV2.Builder builder);

        @JsonProperty(value = "types")
        public ImmutableFeatureProviderDataV2.Builder putTypes2(String key, ImmutableFeatureTypeV2.Builder builder) {
            return putTypes(key, builder.name(key));
        }

        private static class KeyToNameBuilderMap implements Map<String, ImmutableFeatureTypeV2.Builder> {
            private final Map<String, ImmutableFeatureTypeV2.Builder> types;

            public KeyToNameBuilderMap(Map<String, ImmutableFeatureTypeV2.Builder> types) {
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
            public ImmutableFeatureTypeV2.Builder get(Object o) {
                return types.get(o);
            }

            @Override
            public ImmutableFeatureTypeV2.Builder put(String s, ImmutableFeatureTypeV2.Builder builder) {
                return types.put(s, builder.name(s));
            }

            @Override
            public ImmutableFeatureTypeV2.Builder remove(Object o) {
                return types.remove(o);
            }

            @Override
            public void putAll(Map<? extends String, ? extends ImmutableFeatureTypeV2.Builder> map) {
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
            public Collection<ImmutableFeatureTypeV2.Builder> values() {
                return types.values();
            }

            @Override
            public Set<Entry<String, ImmutableFeatureTypeV2.Builder>> entrySet() {
                return types.entrySet();
            }
        }
    }
}
