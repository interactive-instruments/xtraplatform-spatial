/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBooleanValue2.Builder.class)
public interface BooleanValue2 extends Cql2Predicate, Literal, CqlNode {

    @Value.Derived
    @Override
    default Class<?> getType() {
        return java.lang.Boolean.class;
    }

    static BooleanValue2 of(java.lang.Boolean literal) {
        return new ImmutableBooleanValue2.Builder().value(literal).build();
    }
}
