/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableIntersects.Builder.class)
public interface Intersects extends SpatialOperation, CqlNode {

    static Intersects of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableIntersects.Builder().property(property)
                                                .value(spatialLiteral)
                                                .build();
    }

    static Intersects of(String property, BoundingBox boundingBox) {
        return new ImmutableIntersects.Builder().property(property)
                                                .value(SpatialLiteral.of(Geometry.Envelope.of(boundingBox)))
                                                .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Intersects> {
    }

}
