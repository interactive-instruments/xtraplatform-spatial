/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableSchemaConstraints.Builder.class)
public interface SchemaConstraints {

    Optional<String> getCodelist();

    @JsonProperty(value = "enum")
    List<String> getEnumValues();

    Optional<String> getRegex();

    Optional<Boolean> getRequired();

    Optional<Double> getMin();

    Optional<Double> getMax();

    Optional<Integer> getMinOccurrence();

    Optional<Integer> getMaxOccurrence();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isRequired() {
        return getRequired().filter(required -> Objects.equals(required, true)).isPresent();
    }

}
