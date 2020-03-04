package de.ii.xtraplatform.cql.app;


import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.*;

import java.util.Objects;

public class CqlFilterExamples {

    static final CqlFilter EXAMPLE_1 = CqlFilter.of(Gt.of("floors", ScalarLiteral.of(5)));

    static final CqlFilter EXAMPLE_2 = CqlFilter.of(Lte.of("taxes", ScalarLiteral.of(500)));

    static final CqlFilter EXAMPLE_3 = CqlFilter.of(Like.of("owner", ScalarLiteral.of("% Jones %")));

    static final CqlFilter EXAMPLE_4 = CqlFilter.of(Like.of("owner", ScalarLiteral.of("Mike%"), "%"));

    static final CqlFilter EXAMPLE_5 = CqlFilter.of(Not.of(Like.of("owner", ScalarLiteral.of("% Mike %"))));

    static final CqlFilter EXAMPLE_6 = CqlFilter.of(Eq.of("swimming_pool", ScalarLiteral.of(Boolean.TRUE)));

    static final CqlFilter EXAMPLE_7 = CqlFilter.of(And.of(
            EXAMPLE_1,
            CqlPredicate.of(Eq.of("swimming_pool", ScalarLiteral.of(true)))
    ));

    static final CqlFilter EXAMPLE_8 = CqlFilter.of(And.of(
            EXAMPLE_6,
            CqlPredicate.of(Or.of(
                    EXAMPLE_1,
                    CqlPredicate.of(Like.of("material", ScalarLiteral.of("brick%"))),
                    CqlPredicate.of(Like.of("material", ScalarLiteral.of("%brick")))
            ))
    ));

    static final CqlFilter EXAMPLE_9 = CqlFilter.of(Or.of(
            CqlPredicate.of(And.of(
                    EXAMPLE_1,
                    CqlPredicate.of(Eq.of("material", ScalarLiteral.of("brick")))
            )),
            EXAMPLE_6
    ));

    static final CqlFilter EXAMPLE_10 = CqlFilter.of(Or.of(
            CqlPredicate.of(Not.of(Lt.of("floors", ScalarLiteral.of(5)))),
            EXAMPLE_6
    ));

    static final CqlFilter EXAMPLE_11 = CqlFilter.of(And.of(
            CqlPredicate.of(Or.of(
                    CqlPredicate.of(Like.of("owner", ScalarLiteral.of("mike%"))),
                    CqlPredicate.of(Like.of("owner", ScalarLiteral.of("Mike%")))
            )),
            CqlPredicate.of(Lt.of("floors", ScalarLiteral.of(4)))
    ));

    public static final CqlFilter EXAMPLE_12 = CqlFilter.of(Before.of("built", TemporalLiteral.of("2015-01-01T00:00:00Z")));

    static final CqlFilter EXAMPLE_13 = CqlFilter.of(After.of("built", TemporalLiteral.of("2012-06-05T00:00:00Z")));

    public static final CqlFilter EXAMPLE_14 = CqlFilter.of(During.of("updated", TemporalLiteral.of(ImmutableList.of("2017-06-10T07:30:00Z", "2017-06-11T10:30:00Z"))));

    public static final CqlFilter EXAMPLE_15 = CqlFilter.of(Within.of("location", SpatialLiteral.of(Geometry.Envelope.of(33.8, -118.0, 34.0, -117.9))));

    public static final CqlFilter EXAMPLE_16 = CqlFilter.of(Intersects.of("location", SpatialLiteral.of(Geometry.Polygon.of(ImmutableList.of(
            Geometry.Coordinate.of(-10.0, -10.0),
            Geometry.Coordinate.of(10.0, -10.0),
            Geometry.Coordinate.of(10.0, 10.0),
            Geometry.Coordinate.of(-10.0, -10.0)
    )))));

    static final CqlFilter EXAMPLE_17 = CqlFilter.of(And.of(
            EXAMPLE_1,
            CqlPredicate.of(Within.of("geometry", SpatialLiteral.of(Geometry.Envelope.of(33.8, -118.0, 34.0, -117.9))))
    ));

    static final CqlFilter EXAMPLE_18 = CqlFilter.of(Between.of("floors", ScalarLiteral.of(4), ScalarLiteral.of(8)));

    static final CqlFilter EXAMPLE_19 = CqlFilter.of(In.of("owner", ScalarLiteral.of("Mike"), ScalarLiteral.of("John"), ScalarLiteral.of("Tom")));

    static final CqlFilter EXAMPLE_20 = CqlFilter.of(IsNull.of("owner"));

    static final CqlFilter EXAMPLE_21 = CqlFilter.of(Not.of(IsNull.of("owner")));

    // EXISTS and DOES-NOT-EXIST are deactivated in the parser
    //static final CqlFilter EXAMPLE_22 = CqlFilter.of(Exists.of("owner"));

    //static final CqlFilter EXAMPLE_23 = CqlFilter.of(Not.of(Exists.of("owner")));

    static final CqlFilter EXAMPLE_24 = CqlFilter.of(Before.of("built", getTemporalLiteral("2015-01-01")));

    static final CqlFilter EXAMPLE_25 = CqlFilter.of(During.of("updated",
            Objects.requireNonNull(getTemporalLiteral("2017-06-10/2017-06-11"))));

    static final CqlFilter EXAMPLE_26 = CqlFilter.of(During.of("updated",
            Objects.requireNonNull(getTemporalLiteral("2017-06-10T07:30:00Z/.."))));

    static final CqlFilter EXAMPLE_27 = CqlFilter.of(During.of("updated",
            Objects.requireNonNull(getTemporalLiteral("../2017-06-11T10:30:00Z"))));

    static final CqlFilter EXAMPLE_28 = CqlFilter.of(During.of("updated",
            Objects.requireNonNull(getTemporalLiteral("../.."))));

    static final CqlFilter EXAMPLE_29 = CqlFilter.of(Eq.ofFunction(
            Function.of("pos", ImmutableList.of()), ScalarLiteral.of(1)));

    static final CqlFilter EXAMPLE_30 = CqlFilter.of(Gte.ofFunction(
            Function.of("indexOf", ImmutableList.of(Property.of("names"), ScalarLiteral.of("Mike"))), ScalarLiteral.of(5)));

    static final CqlFilter EXAMPLE_31 = CqlFilter.of(Eq.ofFunction(
            Function.of("year", ImmutableList.of(Objects.requireNonNull(getTemporalLiteral("2012-06-05T00:00:00Z")))), ScalarLiteral.of(2012)));


    private static TemporalLiteral getTemporalLiteral(String temporalData) {
        try {
            return TemporalLiteral.of(temporalData);
        } catch (CqlParseException e) {
            e.printStackTrace();
        }
        return null;
    }


}
