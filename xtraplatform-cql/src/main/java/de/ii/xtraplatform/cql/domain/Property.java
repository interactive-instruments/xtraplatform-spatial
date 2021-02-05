/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Splitter;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(of = "new")
@JsonDeserialize(as = ImmutableProperty.class)
public interface Property extends Scalar, Spatial, Temporal, Operand, CqlNode {

    static Property of(String name) {
        return ImmutableProperty.builder()
                .name(name)
                .build();
    }

    static Property of(String name, Map<String, CqlFilter> nestedFilters) {
        return ImmutableProperty.builder()
                .name(name)
                .nestedFilters(nestedFilters)
                .build();
    }

    @JsonValue
    @Value.Parameter
    String getName();

    @JsonIgnore
    Map<String, CqlFilter> getNestedFilters();

    Splitter PATH_SPLITTER = Splitter.on('.')
                                     .omitEmptyStrings();

    @JsonIgnore
    @Value.Derived
    default List<String> getPath() {
        return PATH_SPLITTER.splitToList(getName());
    }

}
