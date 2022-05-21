/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTDuring.Builder.class)
public interface TDuring extends BinaryTemporalOperation, CqlNode {

    String TYPE = "t_during";

    @Override
    @Value.Derived
    default String getOp() {
        return TYPE;
    }

    static TDuring of(Temporal temporal1, Temporal temporal2) {
        return new ImmutableTDuring.Builder()
            .addArgs(temporal1, temporal2)
            .build();
    }

    abstract class Builder extends BinaryTemporalOperation.Builder<TDuring> {
    }

}