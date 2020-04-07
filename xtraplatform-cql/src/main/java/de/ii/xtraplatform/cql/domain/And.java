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
@JsonDeserialize(as = And.class)
public interface And extends LogicalOperation, CqlNode {

    static And of(CqlPredicate... predicates) {
        return new ImmutableAnd.Builder()
                .addPredicates(predicates)
                .build();
    }

    @JsonCreator
    static And of(List<CqlPredicate> predicates) {
        return new ImmutableAnd.Builder().predicates(predicates)
                                         .build();
    }

}
