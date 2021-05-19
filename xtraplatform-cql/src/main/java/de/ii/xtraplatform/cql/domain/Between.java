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
@JsonDeserialize(builder = ImmutableBetween.Builder.class)
public interface Between extends NonBinaryScalarOperation, CqlNode {

    static Between of(String property, ScalarLiteral scalarLiteral1, ScalarLiteral scalarLiteral2) {
        return new ImmutableBetween.Builder().value(Property.of(property))
                                             .lower(scalarLiteral1)
                                             .upper(scalarLiteral2)
                                             .build();
    }

    static Between of(Property property, ScalarLiteral scalarLiteral1, ScalarLiteral scalarLiteral2) {
        return new ImmutableBetween.Builder().value(property)
                .lower(scalarLiteral1)
                .upper(scalarLiteral2)
                .build();
    }

    static Between ofFunction(Function function, ScalarLiteral scalarLiteral1, ScalarLiteral scalarLiteral2) {
        return new ImmutableBetween.Builder().value(function)
                                             .lower(scalarLiteral1)
                                             .upper(scalarLiteral2)
                                             .build();
    }

    Optional<Scalar> getValue();

    Optional<Scalar> getLower();

    Optional<Scalar> getUpper();

    @Value.Check
    default void check() {
        int count = getOperands().size();
        Preconditions.checkState(count == 3, "a BETWEEN operation must have exactly three operands, found %s", count);
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<Operand> getOperands() {
        return ImmutableList.of(
                getValue(),
                getLower(),
                getUpper()
        )
                            .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(ImmutableList.toImmutableList());
    }

    abstract class Builder {

        public abstract Between build();

        public abstract Between.Builder value(Scalar operand);

        public abstract Between.Builder lower(Scalar lower);

        public abstract Between.Builder upper(Scalar upper);
    }

    @Override
    default <U> U accept(CqlVisitor<U> visitor) {
        U operand = getValue().get()
                                  .accept(visitor);
        U lower = getLower().get()
                            .accept(visitor);
        U upper = getUpper().get()
                            .accept(visitor);

        return visitor.visit(this, Lists.newArrayList(operand, lower, upper));
    }
}
