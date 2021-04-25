/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableIsNull.Builder.class)
public interface IsNull extends NonBinaryScalarOperation, CqlNode {

    static IsNull of(String property) {
        return new ImmutableIsNull.Builder().operand(Property.of(property)).build();
    }

    static IsNull of(Function function) {
        return new ImmutableIsNull.Builder().operand(function).build();
    }

    Optional<Scalar> getOperand();

    @Value.Check
    default void check() {
        int count = getOperands().size();
        Preconditions.checkState(count == 1, "IS NULL operation must have exactly one operand, found %s", count);
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<Operand> getOperands() {
        return ImmutableList.of(
                getOperand()
        )
                            .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(ImmutableList.toImmutableList());
    }

    abstract class Builder {

        public abstract IsNull build();

        public abstract IsNull.Builder operand(Scalar operand);
    }

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        return visitor.visit(this, Lists.newArrayList(getOperands().get(0).accept(visitor)));
    }
}
