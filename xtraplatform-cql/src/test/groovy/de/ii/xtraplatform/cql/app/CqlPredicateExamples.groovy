package de.ii.xtraplatform.cql.app

import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.cql.domain.*
import org.threeten.extra.Interval

import java.time.Instant

class CqlPredicateExamples {

    static final CqlPredicate EXAMPLE_1 = new ImmutableCqlPredicate.Builder()
            .gt(new ImmutableGt.Builder()
                    .property(Property.of('floors'))
                    .value(ScalarLiteral.of(5))
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_2 = new ImmutableCqlPredicate.Builder()
            .lte(new ImmutableLte.Builder()
                    .property(Property.of("taxes"))
                    .value(ScalarLiteral.of(500))
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_3 = new ImmutableCqlPredicate.Builder()
            .like(new ImmutableLike.Builder()
                    .property("owner")
                    .value(ScalarLiteral.of("% Jones %"))
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_4 = new ImmutableCqlPredicate.Builder()
            .like(new ImmutableLike.Builder()
                    .property("owner")
                    .value(ScalarLiteral.of("Mike%"))
                    .wildCards("%")
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_5 = new ImmutableCqlPredicate.Builder()
            .not(Not.of(ImmutableList.of(new ImmutableCqlPredicate.Builder()
                    .like(new ImmutableLike.Builder()
                            .property("owner")
                            .value(ScalarLiteral.of("% Mike %"))
                            .build())
                    .build())))
            .build()

    static final CqlPredicate EXAMPLE_6 = new ImmutableCqlPredicate.Builder()
            .eq(new ImmutableEq.Builder()
                    .property("swimming_pool")
                    .value(ScalarLiteral.of(Boolean.TRUE))
                    .build())
            .build()


    static final CqlPredicate EXAMPLE_7 = new ImmutableCqlPredicate.Builder()
            .and(And.of(ImmutableList.of(
                    EXAMPLE_1,
                    new ImmutableCqlPredicate.Builder()
                            .eq(new ImmutableEq.Builder()
                                    .property(Property.of('swimming_pool'))
                                    .value(ScalarLiteral.of(true))
                                    .build())
                            .build()
            )))
            .build()

    static final CqlPredicate EXAMPLE_8 = new ImmutableCqlPredicate.Builder()
            .and(And.of(ImmutableList.of(
                    EXAMPLE_6,
                    new ImmutableCqlPredicate.Builder()
                            .or(Or.of(ImmutableList.of(
                                    EXAMPLE_1,
                                    new ImmutableCqlPredicate.Builder()
                                            .like(new ImmutableLike.Builder()
                                                    .property("material")
                                                    .value(ScalarLiteral.of("brick%"))
                                                    .build())
                                            .build(),
                                    new ImmutableCqlPredicate.Builder()
                                            .like(new ImmutableLike.Builder()
                                                    .property("material")
                                                    .value(ScalarLiteral.of("%brick"))
                                                    .build())
                                            .build()
                            )))
                            .build()
            )))
            .build()

    static final CqlPredicate EXAMPLE_9 = new ImmutableCqlPredicate.Builder()
            .or(Or.of(ImmutableList.of(
                    new ImmutableCqlPredicate.Builder()
                            .and(And.of(ImmutableList.of(
                                    EXAMPLE_1,
                                    new ImmutableCqlPredicate.Builder()
                                            .eq(new ImmutableEq.Builder()
                                                    .property("material")
                                                    .value(ScalarLiteral.of("brick"))
                                                    .build())
                                            .build()
                            )))
                            .build(),
                    EXAMPLE_6)))
            .build()

    static final CqlPredicate EXAMPLE_10 = new ImmutableCqlPredicate.Builder()
            .or(Or.of(ImmutableList.of(
                    new ImmutableCqlPredicate.Builder()
                            .not(Not.of(ImmutableList.of(new ImmutableCqlPredicate.Builder()
                                    .lt(new ImmutableLt.Builder()
                                            .property("floors")
                                            .value(ScalarLiteral.of(5))
                                            .build())
                                    .build())))
                            .build(),
                    EXAMPLE_6
            )))
            .build()

    static final CqlPredicate EXAMPLE_11 = new ImmutableCqlPredicate.Builder()
            .and(And.of(ImmutableList.of(
                    new ImmutableCqlPredicate.Builder()
                            .or(Or.of(ImmutableList.of(
                                    new ImmutableCqlPredicate.Builder()
                                            .like(new ImmutableLike.Builder()
                                                    .property("owner")
                                                    .value(ScalarLiteral.of("mike%"))
                                                    .build())
                                            .build(),
                                    new ImmutableCqlPredicate.Builder()
                                            .like(new ImmutableLike.Builder()
                                                    .property("owner")
                                                    .value(ScalarLiteral.of("Mike%"))
                                                    .build())
                                            .build()
                            )))
                            .build(),
                    new ImmutableCqlPredicate.Builder()
                            .lt(new ImmutableLt.Builder()
                                    .property("floors")
                                    .value(ScalarLiteral.of(4))
                                    .build())
                            .build()
            )))
            .build()

    static final CqlPredicate EXAMPLE_12 = new ImmutableCqlPredicate.Builder()
            .before(new ImmutableBefore.Builder()
                    .property("built")
                    .value(TemporalLiteral.of(Instant.parse("2015-01-01T00:00:00Z")))
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_13 = new ImmutableCqlPredicate.Builder()
            .after(new ImmutableAfter.Builder()
                    .property("built")
                    .value(TemporalLiteral.of(Instant.parse("2012-06-05T00:00:00Z")))
                    .build())
            .build()


    static final CqlPredicate EXAMPLE_14 = new ImmutableCqlPredicate.Builder()
            .during(new ImmutableDuring.Builder()
                    .property(Property.of('updated'))
                    .value(TemporalLiteral.of(Interval.of(Instant.parse("2017-06-10T07:30:00Z"), Instant.parse("2017-06-11T10:30:00Z"))))
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_15 = new ImmutableCqlPredicate.Builder()
            .within(new ImmutableWithin.Builder()
                    .property(Property.of('location'))
                    .value(SpatialLiteral.of(new ImmutableEnvelope.Builder()
                            .coordinates(ImmutableList.of(new Double(33.8), new Double(-118.0), new Double(34.0), new Double(-117.9)))
                            .build()))
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_16 = new ImmutableCqlPredicate.Builder()
            .intersects(new ImmutableIntersects.Builder()
                    .property(Property.of('location'))
                    .value(SpatialLiteral.of(new ImmutablePolygon.Builder()
                            .coordinates(ImmutableList.of(ImmutableList.of(
                                    new Geometry.Coordinate(new Double(-10.0), new Double(-10.0)),
                                    new Geometry.Coordinate(new Double(10.0), new Double(-10.0)),
                                    new Geometry.Coordinate(new Double(10.0), new Double(10.0)),
                                    new Geometry.Coordinate(new Double(-10.0), new Double(-10.0))
                            )))
                            .build()))
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_17 = new ImmutableCqlPredicate.Builder()
            .and(And.of(ImmutableList.of(
                    EXAMPLE_1,
                    new ImmutableCqlPredicate.Builder()
                            .within(new ImmutableWithin.Builder()
                                    .property("geometry")
                                    .value(SpatialLiteral.of(new ImmutableEnvelope.Builder()
                                            .coordinates(ImmutableList.of(new Double(33.8), new Double(-118.0), new Double(34.0), new Double(-117.9)))
                                            .build()))
                                    .build())
                            .build())))
            .build()

    static final CqlPredicate EXAMPLE_18 = new ImmutableCqlPredicate.Builder()
            .between(new ImmutableBetween.Builder()
                    .property("floors")
                    .lower(ScalarLiteral.of(4))
                    .upper(ScalarLiteral.of(8))
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_19 = new ImmutableCqlPredicate.Builder()
            .inOperator(new ImmutableIn.Builder()
                    .property("owner")
                    .values(ImmutableList.of(ScalarLiteral.of("Mike"), ScalarLiteral.of("John"), ScalarLiteral.of("Tom")))
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_20 = new ImmutableCqlPredicate.Builder()
            .isNull(new ImmutableIsNull.Builder()
                    .property("owner")
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_21 = new ImmutableCqlPredicate.Builder()
            .not(Not.of(ImmutableList.of(new ImmutableCqlPredicate.Builder()
                    .isNull(new ImmutableIsNull.Builder()
                            .property("owner")
                            .build())
                    .build())))
            .build()

    static final CqlPredicate EXAMPLE_22 = new ImmutableCqlPredicate.Builder()
            .exists(new ImmutableExists.Builder()
                    .property("owner")
                    .build())
            .build()

    static final CqlPredicate EXAMPLE_23 = new ImmutableCqlPredicate.Builder()
            .not(Not.of(ImmutableList.of(new ImmutableCqlPredicate.Builder()
                    .exists(new ImmutableExists.Builder()
                            .property("owner")
                            .build())
                    .build())))
            .build()

}
