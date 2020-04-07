/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = Or.class)
public interface Or extends LogicalOperation, CqlNode {

    static Or of(CqlPredicate... predicates) {
        return new ImmutableOr.Builder()
                .addPredicates(predicates)
                .build();
    }

    @JsonCreator
    static Or of(List<CqlPredicate> predicates) {
        return new ImmutableOr.Builder()
                .predicates(predicates)
                .build();
    }

}
