/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableContains.Builder.class)
public interface Contains extends SpatialOperation, CqlNode {

    static Contains of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableContains.Builder()
                .property(property)
                .value(spatialLiteral)
                .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Contains> {
    }
}
