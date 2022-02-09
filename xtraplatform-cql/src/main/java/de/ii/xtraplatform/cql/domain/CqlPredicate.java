/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCqlPredicate.Builder.class)
public interface CqlPredicate extends LogicalExpression, ScalarExpression, SpatialExpression, TemporalExpression, ArrayExpression, CqlNode {

    static CqlPredicate of(CqlNode node) {
        ImmutableCqlPredicate.Builder builder = new ImmutableCqlPredicate.Builder();

        if (node instanceof And) {
            builder.and((And) node);
        } else if (node instanceof Or) {
            builder.or((Or) node);
        } else if (node instanceof Not) {
            builder.not((Not) node);
        } else if (node instanceof Eq) {
            builder.eq((Eq) node);
        } else if (node instanceof Neq) {
            builder.neq((Neq) node);
        } else if (node instanceof Gt) {
            builder.gt((Gt) node);
        } else if (node instanceof Gte) {
            builder.gte((Gte) node);
        } else if (node instanceof Lt) {
            builder.lt((Lt) node);
        } else if (node instanceof Lte) {
            builder.lte((Lte) node);
        } else if (node instanceof Between) {
            builder.between((Between) node);
        } else if (node instanceof In) {
            builder.inOperator((In) node);
        } else if (node instanceof Like) {
            builder.like((Like) node);
        } else if (node instanceof IsNull) {
            builder.isNull((IsNull) node);
        } else if (node instanceof TemporalOperation) {
            builder.temporalOperation((TemporalOperation) node);
        } else if (node instanceof SEquals) {
            builder.sEquals((SEquals) node);
        } else if (node instanceof SDisjoint) {
            builder.sDisjoint((SDisjoint) node);
        } else if (node instanceof STouches) {
            builder.sTouches((STouches) node);
        } else if (node instanceof SWithin) {
            builder.sWithin((SWithin) node);
        } else if (node instanceof SOverlaps) {
            builder.sOverlaps((SOverlaps) node);
        } else if (node instanceof SCrosses) {
            builder.sCrosses((SCrosses) node);
        } else if (node instanceof SIntersects) {
            builder.sIntersects((SIntersects) node);
        } else if (node instanceof SContains) {
            builder.sContains((SContains) node);
        } else if (node instanceof AContains)  {
            builder.aContains((AContains) node);
        } else if (node instanceof AEquals) {
            builder.aEquals((AEquals) node);
        } else if (node instanceof AOverlaps) {
            builder.aOverlaps((AOverlaps) node);
        } else if (node instanceof AContainedBy) {
            builder.aContainedBy((AContainedBy) node);
        }

        return builder.build();
    }

    @Value.Check
    default void check() {
        int count = getExpressions().size();

        Preconditions.checkState(count == 1, "a cql predicate must have exactly one child, found %s", count);
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<CqlNode> getExpressions() {
        return ImmutableList.of(
                getAnd(),
                getOr(),
                getNot(),
                getEq(),
                getNeq(),
                getGt(),
                getGte(),
                getLt(),
                getLte(),
                getLike(),
                getBetween(),
                getInOperator(),
                getIsNull(),
                getSEquals(),
                getSDisjoint(),
                getSTouches(),
                getSWithin(),
                getSOverlaps(),
                getSCrosses(),
                getSIntersects(),
                getSContains(),
                getTemporalOperation(),
                getAContains(),
                getAEquals(),
                getAOverlaps(),
                getAContainedBy()
        )
                            .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(ImmutableList.toImmutableList());
    }

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        T expression = getExpressions().get(0)
                                       .accept(visitor);
        return visitor.visit(this, Lists.newArrayList(expression));
    }
}
