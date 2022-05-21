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
@JsonDeserialize(builder = ImmutableTIntersects.Builder.class)
public interface TIntersects extends BinaryTemporalOperation, CqlNode {

    String TYPE = "t_intersects";

    @Override
    @Value.Derived
    default String getOp() {
        return TYPE;
    }

    static TIntersects of(Temporal temporal1, Temporal temporal2) {
        return new ImmutableTIntersects.Builder()
            .addArgs(temporal1, temporal2)
            .build();
    }

    abstract class Builder extends BinaryTemporalOperation.Builder<TIntersects> {
    }

}
