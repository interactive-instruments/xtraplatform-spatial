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
@JsonDeserialize(builder = ImmutableSDisjoint.Builder.class)
public interface SDisjoint extends BinarySpatialOperation, CqlNode {

    String TYPE = "s_disjoint";

    @Override
    @Value.Derived
    default String getOp() {
        return TYPE;
    }

    static SDisjoint of(Spatial spatial1, Spatial spatial2) {
        return new ImmutableSDisjoint.Builder()
            .addArgs(spatial1, spatial2)
            .build();
    }

    abstract class Builder extends BinarySpatialOperation.Builder<SDisjoint> {
    }

}
