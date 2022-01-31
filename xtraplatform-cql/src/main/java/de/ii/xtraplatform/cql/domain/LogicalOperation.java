/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

public interface LogicalOperation extends CqlNode {

    @JsonValue
    List<CqlPredicate> getPredicates();

    @Value.Check
    default void check() {
        Preconditions.checkState(getPredicates().size() > 1, "a boolean operation must have at least two children, found %s", getPredicates().size());
    }

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        List<T> children = getPredicates()
                .stream()
                .map(predicate -> predicate.accept(visitor))
                .collect(Collectors.toList());

        return visitor.visit(this, children);
    }
}
