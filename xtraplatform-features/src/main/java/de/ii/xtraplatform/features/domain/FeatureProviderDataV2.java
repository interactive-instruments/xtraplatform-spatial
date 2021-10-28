/**
 * Copyright 2021 interactive instruments GmbH
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
import de.ii.xtraplatform.store.domain.entities.AutoEntity;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
@JsonDeserialize(builder = ImmutableFeatureProviderCommonData.Builder.class)
public interface FeatureProviderDataV2 extends EntityData, AutoEntity, ExtendableConfiguration {

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

    Optional<EpsgCrs> getNativeCrs();

    @JsonDeserialize(converter = ZoneIdFromString.class)
    Optional<ZoneId> getNativeTimeZone();

    Optional<String> getDefaultLanguage();

    @Value.Default
    default MODE getTypeValidation() {
        return MODE.NONE;
    }

    @Override
    List<ExtensionConfiguration> getExtensions();

    //behaves exactly like Map<String, FeatureSchema>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getTypes();

    Map<String, Map<String, String>> getCodelists();

    List<String> getAutoTypes();


    // custom builder to automatically use keys of types as name of FeatureTypeV2
    abstract class Builder<T extends Builder<T>> implements EntityDataBuilder<FeatureProviderDataV2> {
        @JsonIgnore
        public abstract Map<String, ImmutableFeatureSchema.Builder> getTypes();

        @JsonProperty(value = "types")
        public Map<String, ImmutableFeatureSchema.Builder> getTypes2() {
            Map<String, ImmutableFeatureSchema.Builder> types = getTypes();

            return new ApplyKeyToValueMap<>(types, (key, builder) -> builder.name(key));
        }

        public abstract T putTypes(String key, ImmutableFeatureSchema.Builder builder);

        @JsonProperty(value = "types")
        public T putTypes2(String key, ImmutableFeatureSchema.Builder builder) {
            return putTypes(key, builder.name(key));
        }

        public abstract T id(String id);
        public abstract T providerType(String providerType);
        public abstract T featureProviderType(String featureProviderType);

        // jackson should append to instead of replacing extensions
        @JsonIgnore
        public abstract T extensions(Iterable<? extends ExtensionConfiguration> elements);

        @JsonProperty("extensions")
        public abstract T addAllExtensions(Iterable<? extends ExtensionConfiguration> elements);

    }

}
