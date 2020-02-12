package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCqlPredicate.Builder.class)
public interface CqlPredicate extends LogicalExpression, ScalarExpression, SpatialExpression, TemporalExpression, CqlNode {

    static CqlPredicate of(CqlNode node) {
        ImmutableCqlPredicate.Builder builder = new ImmutableCqlPredicate.Builder();

        if (node instanceof And) {
            builder.and((And) node);
        } else if (node instanceof Or) {
            builder.or((Or) node);
        }  else if (node instanceof Not) {
            builder.not((Not) node);
        }  else if (node instanceof Eq) {
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
        } else if (node instanceof Exists) {
            builder.exists((Exists) node);
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
        } else if (node instanceof Equals) {
            builder.within((Within) node);
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
                getExists(),
                getIsNull(),
                getEquals(),
                getDisjoint(),
                getTouches(),
                getWithin(),
                getOverlaps(),
                getCrosses(),
                getIntersects(),
                getContains(),
                getAfter(),
                getBefore(),
                getBegins(),
                getBegunBy(),
                getTContains(),
                getDuring(),
                getEndedBy(),
                getEnds(),
                getTEquals(),
                getMeets(),
                getMetBy(),
                getTOverlaps(),
                getOverlappedBy()
        )
                            .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(ImmutableList.toImmutableList());
    }

    @Override
    default String toCqlText() {
        return getExpressions().get(0)
                               .toCqlText();
    }

    @Override
    default String toCqlTextTopLevel() {
        return getExpressions().get(0)
                .toCqlTextTopLevel();
    }
}
