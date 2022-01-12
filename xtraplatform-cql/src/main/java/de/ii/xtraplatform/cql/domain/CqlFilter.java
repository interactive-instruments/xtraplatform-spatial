/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCqlFilter.Builder.class)
public interface CqlFilter extends CqlPredicate {

    static CqlFilter of(CqlNode node) {
        if (node instanceof CqlFilter) {
            return of(((CqlFilter) node).getExpressions()
                                        .get(0));
        } else if (node instanceof CqlPredicate) {
            return of(((CqlPredicate) node).getExpressions()
                                           .get(0));
        }

        ImmutableCqlFilter.Builder builder = new ImmutableCqlFilter.Builder();

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
        } else if (node instanceof TAfter) {
            builder.tAfter((TAfter) node);
        } else if (node instanceof TBefore) {
            builder.tBefore((TBefore) node);
        } else if (node instanceof TStarts) {
            builder.tStarts((TStarts) node);
        } else if (node instanceof TStartedBy) {
            builder.tStartedBy((TStartedBy) node);
        } else if (node instanceof TContains) {
            builder.tContains((TContains) node);
        } else if (node instanceof TDuring) {
            builder.tDuring((TDuring) node);
        } else if (node instanceof TFinishedBy) {
            builder.tFinishedBy((TFinishedBy) node);
        } else if (node instanceof TFinishes) {
            builder.tFinishes((TFinishes) node);
        } else if (node instanceof TEquals) {
            builder.tEquals((TEquals) node);
        } else if (node instanceof TMeets) {
            builder.tMeets((TMeets) node);
        } else if (node instanceof TMetBy) {
            builder.tMetBy((TMetBy) node);
        } else if (node instanceof TOverlaps) {
            builder.tOverlaps((TOverlaps) node);
        } else if (node instanceof TOverlappedBy) {
            builder.tOverlappedBy((TOverlappedBy) node);
        } else if (node instanceof TDisjoint) {
            builder.tDisjoint((TDisjoint) node);
        } else if (node instanceof TIntersects) {
            builder.tIntersects((TIntersects) node);
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
        } else if (node instanceof AContains) {
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

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        T expression = getExpressions().get(0)
                                       .accept(visitor);
        return visitor.visit(this, Lists.newArrayList(expression));
    }
}
