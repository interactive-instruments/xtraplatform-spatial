/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.Objects;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCasei.Builder.class)
public interface Casei extends CqlNode, Operand, Scalar {

    String TYPE = "casei";

    @JsonProperty(TYPE)
    Operand getValue();

    static Casei of(Operand value) {
        return new ImmutableCasei.Builder()
            .value(value)
            .build();
    }

    @Override
    default <U> U accept(CqlVisitor<U> visitor) {

        U value = getValue().accept(visitor);

        return visitor.visit(this, Objects.nonNull(value) ? ImmutableList.of(value) : ImmutableList.of());
    }
}



