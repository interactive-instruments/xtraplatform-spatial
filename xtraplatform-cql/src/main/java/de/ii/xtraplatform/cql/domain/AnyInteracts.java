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
@JsonDeserialize(builder = ImmutableAnyInteracts.Builder.class)
public interface AnyInteracts extends TemporalOperation, CqlNode {

    static AnyInteracts of(String property, TemporalLiteral temporalLiteral) {
        return new ImmutableAnyInteracts.Builder().property(property)
                                            .value(temporalLiteral)
                                            .build();
    }

    abstract class Builder extends TemporalOperation.Builder<AnyInteracts> {
    }

}
