/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(as = IsNull.class)
public interface IsNull extends NonBinaryScalarOperation, CqlNode {

    @JsonCreator
    static IsNull of(String property) {
        return new ImmutableIsNull.Builder().operand(Property.of(property)).build();
    }

    @JsonCreator
    static IsNull of(Property property) { return new ImmutableIsNull.Builder().operand(property).build(); }

    @JsonCreator
    static IsNull of(Function function) {
        return new ImmutableIsNull.Builder().operand(function).build();
    }

    @JsonValue
    Optional<Operand> getOperand();

    @Value.Check
    default void check() {
        Preconditions.checkState(getOperand().isPresent(), "IS NULL operation must have exactly one operand, found 0");
    }

    abstract class Builder {

        public abstract IsNull build();

        @JsonCreator
        public abstract IsNull.Builder operand(Operand operand);
    }

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        return visitor.visit(this, Lists.newArrayList(getOperand().get().accept(visitor)));
    }
}
