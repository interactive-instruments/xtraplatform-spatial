/**
 * Copyright 2021 interactive instruments GmbH
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
@JsonDeserialize(builder = ImmutableArrayLiteral.Builder.class)
public interface ArrayLiteral extends Vector, Literal, CqlNode {

    static ArrayLiteral of(String elements) throws CqlParseException {
        return new ArrayLiteral.Builder(elements).build();
    }

    static ArrayLiteral of(List<Scalar> elements) {
        return new ArrayLiteral.Builder(elements).build();
    }

    class Builder extends ImmutableArrayLiteral.Builder {
        public Builder() {
            super();
        }

        @JsonCreator
        public Builder(List<Scalar> literal) {
            super();
            value(literal);
            type(List.class);
        }

        @JsonCreator
        public Builder(String literal) throws CqlParseException {
            super();
            value(literal);
            type(String.class);
        }
    }

}
