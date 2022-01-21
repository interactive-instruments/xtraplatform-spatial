/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = Intersects.class)
public interface Intersects extends SpatialOperation, CqlNode {

    @JsonCreator
    static Intersects of(List<Operand> operands) {
        return new ImmutableIntersects.Builder().operands(operands)
                                        .build();
    }

    static Intersects of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableIntersects.Builder().operands(ImmutableList.of(Property.of(property),spatialLiteral))
                                            .build();
    }

    static Intersects of(String property, String property2) {
        return new ImmutableIntersects.Builder().operands(ImmutableList.of(Property.of(property), Property.of(property2)))
                                            .build();
    }

    static Intersects of(String property, BoundingBox boundingBox) {
        return new ImmutableIntersects.Builder().operands(ImmutableList.of(Property.of(property),SpatialLiteral.of(Geometry.Envelope.of(boundingBox))))
                                                .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Intersects> {
    }

}
