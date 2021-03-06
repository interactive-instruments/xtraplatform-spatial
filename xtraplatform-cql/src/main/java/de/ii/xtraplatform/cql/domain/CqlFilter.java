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
        } else if (node instanceof After) {
            builder.after((After) node);
        } else if (node instanceof Before) {
            builder.before((Before) node);
        } else if (node instanceof Begins) {
            builder.begins((Begins) node);
        } else if (node instanceof BegunBy) {
            builder.begunBy((BegunBy) node);
        } else if (node instanceof TContains) {
            builder.tContains((TContains) node);
        } else if (node instanceof During) {
            builder.during((During) node);
        } else if (node instanceof EndedBy) {
            builder.endedBy((EndedBy) node);
        } else if (node instanceof Ends) {
            builder.ends((Ends) node);
        } else if (node instanceof TEquals) {
            builder.tEquals((TEquals) node);
        } else if (node instanceof Meets) {
            builder.meets((Meets) node);
        } else if (node instanceof MetBy) {
            builder.metBy((MetBy) node);
        } else if (node instanceof TOverlaps) {
            builder.tOverlaps((TOverlaps) node);
        } else if (node instanceof OverlappedBy) {
            builder.overlappedBy((OverlappedBy) node);
        } else if (node instanceof AnyInteracts) {
            builder.anyInteracts((AnyInteracts) node);
        } else if (node instanceof Equals) {
            builder.equals((Equals) node);
        } else if (node instanceof Disjoint) {
            builder.disjoint((Disjoint) node);
        } else if (node instanceof Touches) {
            builder.touches((Touches) node);
        } else if (node instanceof Within) {
            builder.within((Within) node);
        } else if (node instanceof Overlaps) {
            builder.overlaps((Overlaps) node);
        } else if (node instanceof Crosses) {
            builder.crosses((Crosses) node);
        } else if (node instanceof Intersects) {
            builder.intersects((Intersects) node);
        } else if (node instanceof Contains) {
            builder.contains((Contains) node);
        } else if (node instanceof AContains) {
            builder.aContains((AContains) node);
        } else if (node instanceof AEquals) {
            builder.aEquals((AEquals) node);
        } else if (node instanceof AOverlaps) {
            builder.aOverlaps((AOverlaps) node);
        } else if (node instanceof ContainedBy) {
            builder.containedBy((ContainedBy) node);
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
