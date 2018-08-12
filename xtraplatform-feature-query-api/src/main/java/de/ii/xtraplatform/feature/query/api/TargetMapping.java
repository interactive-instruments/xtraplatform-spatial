/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.feature.query.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xsf.dropwizard.cfg.JacksonProvider;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * @author zahnen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "mappingType")
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public interface TargetMapping<T extends Enum<T>> {
    final String BASE_TYPE = "general";

    //TODO
    @Nullable
    String getName();

    //TODO
    @Nullable
    T getType();

    //TODO
    @Nullable
    Boolean getEnabled();

    //TODO
    @Nullable
    Integer getSortPriority();

    //TODO
    @Nullable
    String getFormat();


    TargetMapping mergeCopyWithBase(TargetMapping targetMapping);

    //TODO
    @JsonIgnore
    @Value.Derived
    boolean isSpatial();

    //TODO
    @JsonIgnore
    @Value.Derived
    default boolean isEnabled() {
        return getEnabled() == null || getEnabled();
    }
}
