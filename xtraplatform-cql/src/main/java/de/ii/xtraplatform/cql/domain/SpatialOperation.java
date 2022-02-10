/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = SpatialOperation.class)
public interface SpatialOperation extends BinaryOperation<SpatialLiteral>, CqlNode {

    @JsonValue
    SpatialOperator getOperator();

    @JsonCreator
    static SpatialOperation of(SpatialOperator operator, List<Operand> operands) {
        return new ImmutableSpatialOperation.Builder()
            .operator(operator)
            .operands(operands)
            .build();
    }

    static SpatialOperation of(SpatialOperator operator, Spatial temporal1, Spatial temporal2) {
        return new ImmutableSpatialOperation.Builder()
            .operator(operator)
            .operands(ImmutableList.of(temporal1, temporal2))
            .build();
    }

    static SpatialOperation of(SpatialOperator operator, String property, SpatialLiteral temporalLiteral) {
        return new ImmutableSpatialOperation.Builder()
            .operator(operator)
            .operands(ImmutableList.of(Property.of(property),temporalLiteral))
            .build();
    }

    static SpatialOperation of(SpatialOperator operator, String property, String property2) {
        return new ImmutableSpatialOperation.Builder()
            .operator(operator)
            .operands(ImmutableList.of(Property.of(property), Property.of(property2)))
            .build();
    }

    static SpatialOperation of(SpatialOperator operator, String property, BoundingBox boundingBox) {
        return new ImmutableSpatialOperation.Builder()
            .operator(operator)
            .operands(ImmutableList.of(Property.of(property),SpatialLiteral.of(Geometry.Envelope.of(boundingBox))))
            .build();
    }

    @Value.Check
    @Override
    default void check() {
        BinaryOperation.super.check();
        getOperands().forEach(operand -> Preconditions.checkState(operand instanceof Spatial, "a spatial operation must have spatial operands, found %s", operand.getClass().getSimpleName()));
    }

    abstract class Builder extends BinaryOperation.Builder<SpatialLiteral, SpatialOperation> {}

}
