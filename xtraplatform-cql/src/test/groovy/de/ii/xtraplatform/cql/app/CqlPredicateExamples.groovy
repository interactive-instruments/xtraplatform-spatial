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

    static final CqlPredicate EXAMPLE_14 = new ImmutableCqlPredicate.Builder()
            .during(new ImmutableDuring.Builder()
                    .property(Property.of('updated'))
                    .value(TemporalLiteral.of(Interval.of(Instant.parse("2017-06-10T07:30:00Z"), Instant.parse("2017-06-11T10:30:00Z"))))
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

}
