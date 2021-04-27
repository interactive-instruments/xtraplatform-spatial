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

@Value.Immutable
@JsonDeserialize(builder = ImmutableSpatialLiteral.Builder.class)
public interface SpatialLiteral extends Spatial, Literal, CqlNode {

    static SpatialLiteral of(String literal) throws CqlParseException {
        return new SpatialLiteral.Builder(literal).build();
    }

    static SpatialLiteral of(Geometry<?> literal) {
        return new SpatialLiteral.Builder(literal).build();
    }

    class Builder extends ImmutableSpatialLiteral.Builder {
        public Builder() {
            super();
        }

        @JsonCreator
        public Builder(Geometry<?> literal) {
            super();
            value(literal);
            type(Geometry.class);
        }

        @JsonCreator
        public Builder(String literal) throws CqlParseException {
            super();
            value(literal);
            type(String.class);
        }
    }
}
