/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

public interface BinaryOperation<T extends Literal> extends CqlNode {

    Optional<Operand> getOperand1();

    Optional<Operand> getOperand2();

    @Value.Check
    default void check() {
        int count = getOperands().size();
        Preconditions.checkState(count == 2, "a binary operation must have exactly two operands, found %s", count);
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<Operand> getOperands() {
        return ImmutableList.of(
                getOperand1(),
                getOperand2()
        )
                            .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(ImmutableList.toImmutableList());
    }


    abstract class Builder<T extends Literal, U extends BinaryOperation<T>> {

        public abstract U build();

        public abstract Builder<T,U> operand1(Operand operand1);

        public abstract Builder<T,U> operand2(Operand operand2);
    }

    @Override
    default <U> U accept(CqlVisitor<U> visitor) {
        U operand1 = getOperands().get(0)
                                  .accept(visitor);
        U operand2 = getOperands().get(1)
                                  .accept(visitor);

        return visitor.visit(this, Lists.newArrayList(operand1, operand2));
    }
}
