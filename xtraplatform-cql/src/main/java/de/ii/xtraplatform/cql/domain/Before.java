/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBefore.Builder.class)
public interface Before extends TemporalOperation, CqlNode {

    static Before of(String property, TemporalLiteral temporalLiteral) {
        return new ImmutableBefore.Builder().property(property)
                                            .value(temporalLiteral)
                                            .build();
    }

    abstract class Builder extends TemporalOperation.Builder<Before> {
    }

}
