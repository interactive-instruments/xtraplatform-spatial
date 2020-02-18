package de.ii.xtraplatform.cql.app

import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.cql.domain.*
import org.threeten.extra.Interval

import java.time.Instant

class CqlPredicateExamples {

    static final CqlFilter EXAMPLE_1 = new ImmutableCqlFilter.Builder()
            .gt(new ImmutableGt.Builder()
                    .property(Property.of('floors'))
                    .value(ScalarLiteral.of(5))
                    .build())
            .build()

    static final CqlFilter EXAMPLE_2 = new ImmutableCqlFilter.Builder()
            .lte(new ImmutableLte.Builder()
                    .property(Property.of("taxes"))
                    .value(ScalarLiteral.of(500))
                    .build())
            .build()

    static final CqlFilter EXAMPLE_3 = new ImmutableCqlFilter.Builder()
            .like(new ImmutableLike.Builder()
                    .property("owner")
                    .value(ScalarLiteral.of("% Jones %"))
                    .build())
            .build()

    static final CqlFilter EXAMPLE_4 = new ImmutableCqlFilter.Builder()
            .like(new ImmutableLike.Builder()
                    .property("owner")
                    .value(ScalarLiteral.of("Mike%"))
                    .wildCards("%")
                    .build())
            .build()

    static final CqlFilter EXAMPLE_5 = new ImmutableCqlFilter.Builder()
            .not(Not.of(ImmutableList.of(new ImmutableCqlPredicate.Builder()
                    .like(new ImmutableLike.Builder()
                            .property("owner")
                            .value(ScalarLiteral.of("% Mike %"))
                            .build())
                    .build())))
            .build()

    static final CqlFilter EXAMPLE_6 = new ImmutableCqlFilter.Builder()
            .eq(new ImmutableEq.Builder()
                    .property("swimming_pool")
                    .value(ScalarLiteral.of(Boolean.TRUE))
                    .build())
            .build()


    static final CqlFilter EXAMPLE_7 = new ImmutableCqlFilter.Builder()
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

    static final CqlFilter EXAMPLE_8 = new ImmutableCqlFilter.Builder()
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

    static final CqlFilter EXAMPLE_9 = new ImmutableCqlFilter.Builder()
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

    static final CqlFilter EXAMPLE_10 = new ImmutableCqlFilter.Builder()
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

    static final CqlFilter EXAMPLE_11 = new ImmutableCqlFilter.Builder()
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

    static final CqlFilter EXAMPLE_12 = new ImmutableCqlFilter.Builder()
            .before(new ImmutableBefore.Builder()
                    .property("built")
                    .value(TemporalLiteral.of(Instant.parse("2015-01-01T00:00:00Z")))
                    .build())
            .build()

    static final CqlFilter EXAMPLE_13 = new ImmutableCqlFilter.Builder()
            .after(new ImmutableAfter.Builder()
                    .property("built")
                    .value(TemporalLiteral.of(Instant.parse("2012-06-05T00:00:00Z")))
                    .build())
            .build()


    static final CqlFilter EXAMPLE_14 = new ImmutableCqlFilter.Builder()
            .during(new ImmutableDuring.Builder()
                    .property(Property.of('updated'))
                    .value(TemporalLiteral.of(Interval.of(Instant.parse("2017-06-10T07:30:00Z"), Instant.parse("2017-06-11T10:30:00Z"))))
                    .build())
            .build()

    static final CqlFilter EXAMPLE_15 = new ImmutableCqlFilter.Builder()
            .within(new ImmutableWithin.Builder()
                    .property(Property.of('location'))
                    .value(SpatialLiteral.of(new ImmutableEnvelope.Builder()
                            .coordinates(ImmutableList.of(new Double(33.8), new Double(-118.0), new Double(34.0), new Double(-117.9)))
                            .build()))
                    .build())
            .build()

    static final CqlFilter EXAMPLE_16 = new ImmutableCqlFilter.Builder()
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

    static final CqlFilter EXAMPLE_17 = new ImmutableCqlFilter.Builder()
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

    static final CqlFilter EXAMPLE_18 = new ImmutableCqlFilter.Builder()
            .between(new ImmutableBetween.Builder()
                    .property("floors")
                    .lower(ScalarLiteral.of(4))
                    .upper(ScalarLiteral.of(8))
                    .build())
            .build()

    static final CqlFilter EXAMPLE_19 = new ImmutableCqlFilter.Builder()
            .inOperator(new ImmutableIn.Builder()
                    .property("owner")
                    .values(ImmutableList.of(ScalarLiteral.of("Mike"), ScalarLiteral.of("John"), ScalarLiteral.of("Tom")))
                    .build())
            .build()

    static final CqlFilter EXAMPLE_20 = new ImmutableCqlFilter.Builder()
            .isNull(new ImmutableIsNull.Builder()
                    .property("owner")
                    .build())
            .build()

    static final CqlFilter EXAMPLE_21 = new ImmutableCqlFilter.Builder()
            .not(Not.of(ImmutableList.of(new ImmutableCqlPredicate.Builder()
                    .isNull(new ImmutableIsNull.Builder()
                            .property("owner")
                            .build())
                    .build())))
            .build()

    static final CqlFilter EXAMPLE_22 = new ImmutableCqlFilter.Builder()
            .exists(new ImmutableExists.Builder()
                    .property("owner")
                    .build())
            .build()

    static final CqlFilter EXAMPLE_23 = new ImmutableCqlFilter.Builder()
            .not(Not.of(ImmutableList.of(new ImmutableCqlPredicate.Builder()
                    .exists(new ImmutableExists.Builder()
                            .property("owner")
                            .build())
                    .build())))
            .build()

}
