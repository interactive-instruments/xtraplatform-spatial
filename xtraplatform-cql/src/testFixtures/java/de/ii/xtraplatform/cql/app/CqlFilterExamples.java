/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.*;
import de.ii.xtraplatform.crs.domain.OgcCrs;

import java.util.Objects;

public class CqlFilterExamples {

    public static final Cql2Predicate EXAMPLE_1 = Gt.of("floors", ScalarLiteral.of(5));
    public static final CqlFilter EXAMPLE_1_OLD = CqlFilter.of(Gt.of("floors", ScalarLiteral.of(5)));

    public static final Cql2Predicate EXAMPLE_2 = Lte.of("taxes", ScalarLiteral.of(500));
    public static final CqlFilter EXAMPLE_2_OLD = CqlFilter.of(Lte.of("taxes", ScalarLiteral.of(500)));

    public static final Cql2Predicate EXAMPLE_3 = Like.of("owner", ScalarLiteral.of("% Jones %"));
    public static final CqlFilter EXAMPLE_3_OLD = CqlFilter.of(Like.of("owner", ScalarLiteral.of("% Jones %")));

    public static final Cql2Predicate EXAMPLE_4 = Like.of("owner", ScalarLiteral.of("Mike%"));
    public static final CqlFilter EXAMPLE_4_OLD = CqlFilter.of(Like.of("owner", ScalarLiteral.of("Mike%")));

    public static final Cql2Predicate EXAMPLE_5 = Not.of(Like.of("owner", ScalarLiteral.of("% Mike %")));
    public static final CqlFilter EXAMPLE_5_OLD = CqlFilter.of(/*Not.of(*/Like.of("owner", ScalarLiteral.of("% Mike %"))/*)*/);

    public static final Cql2Predicate EXAMPLE_6 = Eq.of("swimming_pool", ScalarLiteral.of(true));
    public static final CqlFilter EXAMPLE_6_OLD = CqlFilter.of(Eq.of("swimming_pool", ScalarLiteral.of(Boolean.TRUE)));

    public static final Cql2Predicate EXAMPLE_7 = And.of(EXAMPLE_1, EXAMPLE_6);
    public static final CqlFilter EXAMPLE_7_OLD = CqlFilter.of(And.of(
        EXAMPLE_1_OLD,
        CqlPredicate.of(Eq.of("swimming_pool", ScalarLiteral.of(true)))
    ));

    public static final Cql2Predicate EXAMPLE_8 = And.of(
        EXAMPLE_6,
        Or.of(
            EXAMPLE_1,
            Like.of("material", ScalarLiteral.of("brick%")),
            Like.of("material", ScalarLiteral.of("%brick"))
        )
    );
    public static final CqlFilter EXAMPLE_8_OLD = CqlFilter.of(And.of(
        EXAMPLE_6_OLD,
        CqlPredicate.of(Or.of(
            EXAMPLE_1_OLD,
            CqlPredicate.of(Like.of("material", ScalarLiteral.of("brick%"))),
            CqlPredicate.of(Like.of("material", ScalarLiteral.of("%brick")))
        ))
    ));

    public static final Cql2Predicate EXAMPLE_9 = Or.of(
        And.of(
            EXAMPLE_1,
            Eq.of("material", ScalarLiteral.of("brick"))
        ),
        EXAMPLE_6
    );
    public static final CqlFilter EXAMPLE_9_OLD = CqlFilter.of(Or.of(
        CqlPredicate.of(And.of(
            EXAMPLE_1_OLD,
            CqlPredicate.of(Eq.of("material", ScalarLiteral.of("brick")))
        )),
        EXAMPLE_6_OLD
    ));

    public static final Cql2Predicate EXAMPLE_10 = Or.of(
        Not.of(Lt.of("floors", ScalarLiteral.of(5))),
        EXAMPLE_6
    );
    public static final CqlFilter EXAMPLE_10_OLD = CqlFilter.of(Or.of(
        CqlPredicate.of(/*Not.of(*/Lt.of("floors", ScalarLiteral.of(5))/*)*/),
        EXAMPLE_6_OLD
    ));

    public static final Cql2Predicate EXAMPLE_11 = And.of(
        Or.of(
            Like.of("owner", ScalarLiteral.of("mike%")),
            Like.of("owner", ScalarLiteral.of("Mike%"))
        ),
        Lt.of("floors", ScalarLiteral.of(4))
    );
    public static final CqlFilter EXAMPLE_11_OLD = CqlFilter.of(And.of(
        CqlPredicate.of(Or.of(
            CqlPredicate.of(Like.of("owner", ScalarLiteral.of("mike%"))),
            CqlPredicate.of(Like.of("owner", ScalarLiteral.of("Mike%")))
        )),
        CqlPredicate.of(Lt.of("floors", ScalarLiteral.of(4)))
    ));

    public static final Cql2Predicate EXAMPLE_12 = TBefore.of(Property.of("built"), TemporalLiteral.of("2012-06-05T00:00:00Z"));
    public static final CqlFilter EXAMPLE_12_OLD = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_BEFORE, "built", TemporalLiteral.of("2012-06-05T00:00:00Z")));
    public static final CqlFilter EXAMPLE_12_date = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_BEFORE, "built", TemporalLiteral.of("2012-06-05")));

    public static final CqlFilter EXAMPLE_12_alt = CqlFilter.of(Lt.of(ImmutableList.of(Property.of("built"), TemporalLiteral.of("2012-06-05T00:00:00Z"))));
    public static final CqlFilter EXAMPLE_12eq_alt = CqlFilter.of(Lte.of(ImmutableList.of(Property.of("built"), TemporalLiteral.of("2012-06-05T00:00:00Z"))));

    public static final Cql2Predicate EXAMPLE_13 = TAfter.of(Property.of("built"), TemporalLiteral.of("2012-06-05T00:00:00Z"));
    public static final CqlFilter EXAMPLE_13_OLD = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_AFTER, "built", TemporalLiteral.of("2012-06-05T00:00:00Z")));

    public static final CqlFilter EXAMPLE_13_alt = CqlFilter.of(Gt.of(ImmutableList.of(Property.of("built"), TemporalLiteral.of("2012-06-05T00:00:00Z"))));
    public static final CqlFilter EXAMPLE_13eq_alt = CqlFilter.of(Gte.of(ImmutableList.of(Property.of("built"), TemporalLiteral.of("2012-06-05T00:00:00Z"))));

    public static final CqlFilter EXAMPLE_13A_alt = CqlFilter.of(Eq.of(ImmutableList.of(Property.of("built"), TemporalLiteral.of("2012-06-05T00:00:00Z"))));
    public static final CqlFilter EXAMPLE_13Aneq_alt = CqlFilter.of(Neq.of(ImmutableList.of(Property.of("built"), TemporalLiteral.of("2012-06-05T00:00:00Z"))));

    public static final Cql2Predicate EXAMPLE_14 = TDuring.of(Property.of("updated"), TemporalLiteral.of(ImmutableList.of("2017-06-10T07:30:00Z", "2017-06-11T10:30:00Z")));
    public static final CqlFilter EXAMPLE_14_OLD = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_DURING, "updated", TemporalLiteral.of(ImmutableList.of("2017-06-10T07:30:00Z", "2017-06-11T10:30:00Z"))));

    public static final CqlFilter EXAMPLE_15 = CqlFilter.of(SpatialOperation.of(SpatialOperator.S_WITHIN, "location", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0, OgcCrs.CRS84))));

    public static final CqlFilter EXAMPLE_16 = CqlFilter.of(SpatialOperation.of(SpatialOperator.S_INTERSECTS, "location", SpatialLiteral.of(Geometry.Polygon.of(OgcCrs.CRS84, ImmutableList.of(
            Geometry.Coordinate.of(-10.0, -10.0),
            Geometry.Coordinate.of(10.0, -10.0),
            Geometry.Coordinate.of(10.0, 10.0),
            Geometry.Coordinate.of(-10.0, -10.0)
    )))));

    public static final CqlFilter EXAMPLE_17 = CqlFilter.of(And.of(
        EXAMPLE_1_OLD,
            CqlPredicate.of(SpatialOperation.of(SpatialOperator.S_WITHIN, "geometry", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0, OgcCrs.CRS84))))
    ));

    public static final CqlFilter EXAMPLE_18 = CqlFilter.of(Between.of("floors", ScalarLiteral.of(4), ScalarLiteral.of(8)));

    public static final Cql2Predicate EXAMPLE_19 = In.of("owner", ScalarLiteral.of("Mike"), ScalarLiteral.of("John"), ScalarLiteral.of("Tom"));
    public static final CqlFilter EXAMPLE_19_OLD = CqlFilter.of(In.of("owner", ScalarLiteral.of("Mike"), ScalarLiteral.of("John"), ScalarLiteral.of("Tom")));
    public static final Cql2Predicate EXAMPLE_20 = IsNull.of("owner");
    public static final CqlFilter EXAMPLE_20_OLD = CqlFilter.of(IsNull.of("owner"));

    public static final Cql2Predicate EXAMPLE_21 = Not.of(IsNull.of("owner"));
    public static final CqlFilter EXAMPLE_21_OLD = CqlFilter.of(Not.of(IsNull.of("owner")));

    public static final Cql2Predicate EXAMPLE_24 = TBefore.of(Property.of("built"), TemporalLiteral.of("2015-01-01"));
    public static final CqlFilter EXAMPLE_24_OLD = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_BEFORE, "built", TemporalLiteral.of("2015-01-01")));

    public static final Cql2Predicate EXAMPLE_25 = TDuring.of(Property.of("updated"), Objects.requireNonNull(TemporalLiteral.of("2017-06-10T07:30:00Z", "2017-06-11T10:30:00Z")));
    public static final Cql2Predicate EXAMPLE_25b = TDuring.of(Property.of("updated"), Objects.requireNonNull(TemporalLiteral.of("2017-06-10", "2017-06-11")));
    public static final CqlFilter EXAMPLE_25_OLD = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_DURING, "updated",
        Objects.requireNonNull(TemporalLiteral.of("2017-06-10", "2017-06-11"))));

    public static final Cql2Predicate EXAMPLE_26 = TDuring.of(Property.of("updated"),
                                                                       Objects.requireNonNull(TemporalLiteral.of("2017-06-10T07:30:00Z", "..")));
    public static final CqlFilter EXAMPLE_26_OLD = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_DURING, "updated",
        Objects.requireNonNull(TemporalLiteral.of("2017-06-10T07:30:00Z", ".."))));

    public static final Cql2Predicate EXAMPLE_27 = TDuring.of(Property.of("updated"),
                                                                       Objects.requireNonNull(TemporalLiteral.of("..", "2017-06-11T10:30:00Z")));
    public static final CqlFilter EXAMPLE_27_OLD = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_DURING, "updated",
        Objects.requireNonNull(TemporalLiteral.of("..", "2017-06-11T10:30:00Z"))));

    public static final Cql2Predicate EXAMPLE_28 = TDuring.of(Property.of("updated"),
        Objects.requireNonNull(TemporalLiteral.of("..", "..")));
    public static final CqlFilter EXAMPLE_28_OLD = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_DURING, "updated",
                                                                       Objects.requireNonNull(TemporalLiteral.of("..", ".."))));

    public static final CqlFilter EXAMPLE_29 = CqlFilter.of(Eq.ofFunction(
            Function.of("pos", ImmutableList.of()), ScalarLiteral.of(1)));

    public static final CqlFilter EXAMPLE_30 = CqlFilter.of(Gte.ofFunction(
            Function.of("indexOf", ImmutableList.of(Property.of("names"), ScalarLiteral.of("Mike"))), ScalarLiteral.of(5)));

    public static final CqlFilter EXAMPLE_31 = CqlFilter.of(Eq.ofFunction(
            Function.of("year", ImmutableList.of(Objects.requireNonNull(TemporalLiteral.of("2012-06-05T00:00:00Z")))), ScalarLiteral.of(2012)));

    public static final CqlFilter EXAMPLE_32 = CqlFilter.of(
            Gt.of(Property.of("filterValues.measure",
                            ImmutableMap.of("filterValues", CqlFilter.of(Eq.of("filterValues.property", ScalarLiteral.of("d30"))))),
                    ScalarLiteral.of(0.1)));

    public static final CqlFilter EXAMPLE_33 = CqlFilter.of(
            Gt.of(Property.of("filterValues1.filterValues2.measure",
                    ImmutableMap.of("filterValues1", CqlFilter.of(Eq.of("filterValues1.property1", ScalarLiteral.of("d30"))),
                            "filterValues2", CqlFilter.of(Lte.of("filterValues2.property2", ScalarLiteral.of(100))))),
                    ScalarLiteral.of(0.1)));

    public static final CqlFilter EXAMPLE_41 = CqlFilter.of(
        Eq.of(Property.of("filterValues.classification",
            ImmutableMap.of("filterValues", CqlFilter.of(Eq.of("filterValues.property", ScalarLiteral.of("Bodenklassifizierung"))))),
            ScalarLiteral.of("GU/GT")));

    public static final CqlFilter EXAMPLE_42 = CqlFilter.of(
            Or.of(EXAMPLE_32, EXAMPLE_41));

    public static final CqlFilter EXAMPLE_34 = CqlFilter.of(Eq.of("landsat:scene_id", ScalarLiteral.of("LC82030282019133LGN00")));

    public static final CqlFilter EXAMPLE_35 = CqlFilter.of(Like.of("name", ScalarLiteral.of("Smith.")));

    public static final CqlFilter EXAMPLE_36 = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_INTERSECTS, "event_date", TemporalLiteral.of("1969-07-16T05:32:00Z","1969-07-24T16:50:35Z")));

    public static final CqlFilter EXAMPLE_37 = CqlFilter.of(Lt.of("height", "floors"));

    public static final CqlFilter EXAMPLE_38 = CqlFilter.of(ArrayOperation.of(ArrayOperator.A_CONTAINS, "layer:ids", ArrayLiteral.of(ImmutableList.of(ScalarLiteral.of("layers-ca"), ScalarLiteral.of("layers-us")))));

    public static final CqlFilter EXAMPLE_39 = CqlFilter.of(Not.of(Between.of("floors", ScalarLiteral.of(4), ScalarLiteral.of(8))));

    public static final CqlFilter EXAMPLE_40 = CqlFilter.of(Not.of(In.of("owner", ScalarLiteral.of("Mike"), ScalarLiteral.of("John"), ScalarLiteral.of("Tom"))));

    public static final CqlFilter EXAMPLE_TEQUALS = CqlFilter.of(Eq.of(ImmutableList.of(Property.of("built"), TemporalLiteral.of("2012-06-05T00:00:00Z"))));

    public static final CqlFilter EXAMPLE_TDISJOINT = CqlFilter.of(TemporalOperation.of(TemporalOperator.T_DISJOINT,"event_date", TemporalLiteral.of("1969-07-16T05:32:00Z","1969-07-24T16:50:35Z")));

    public static final Cql2Predicate EXAMPLE_TINTERSECTS = TIntersects.of(Property.of("event_date"), Function.of("INTERVAL", ImmutableList.of(Property.of("startDate"), Property.of("endDate"))));

    public static final CqlFilter EXAMPLE_SDISJOINT = CqlFilter.of(SpatialOperation.of(SpatialOperator.S_DISJOINT, "geometry", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0))));

    public static final CqlFilter EXAMPLE_SEQUALS = CqlFilter.of(SpatialOperation.of(SpatialOperator.S_EQUALS, "geometry", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0))));

    public static final CqlFilter EXAMPLE_STOUCHES = CqlFilter.of(SpatialOperation.of(SpatialOperator.S_TOUCHES, "geometry", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0))));

    public static final CqlFilter EXAMPLE_SOVERLAPS = CqlFilter.of(SpatialOperation.of(SpatialOperator.S_OVERLAPS, "geometry", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0))));

    public static final CqlFilter EXAMPLE_SCROSSES = CqlFilter.of(SpatialOperation.of(SpatialOperator.S_CROSSES, "geometry", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0))));

    public static final CqlFilter EXAMPLE_SCONTAINS = CqlFilter.of(SpatialOperation.of(SpatialOperator.S_CONTAINS, "geometry", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0))));

    public static final CqlFilter EXAMPLE_NESTED_TEMPORAL = CqlFilter.of(
            Gt.of(Property.of("filterValues.measure",
                    ImmutableMap.of("filterValues", CqlFilter.of(Gt.of(ImmutableList.of(Property.of("filterValues.updated"), TemporalLiteral.of("2012-06-05T00:00:00Z")))))),
                    ScalarLiteral.of(0.1)));

    public static final CqlFilter EXAMPLE_NESTED_SPATIAL = CqlFilter.of(
            Gt.of(Property.of("filterValues.measure",
                    ImmutableMap.of("filterValues", CqlFilter.of(SpatialOperation.of(SpatialOperator.S_TOUCHES, "filterValues.location", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0)))))),
                    ScalarLiteral.of(0.1)));

    public static final CqlFilter EXAMPLE_IN_WITH_FUNCTION = CqlFilter.of(In.ofFunction(
            Function.of("position", ImmutableList.of()), ImmutableList.of(ScalarLiteral.of(1), ScalarLiteral.of(3))));

    public static final CqlFilter EXAMPLE_NESTED_FUNCTION = CqlFilter.of(
            Between.of(Property.of("filterValues.measure",
                    ImmutableMap.of("filterValues", EXAMPLE_IN_WITH_FUNCTION)),
                    ScalarLiteral.of(1),
                    ScalarLiteral.of(5)));

    public static final CqlFilter EXAMPLE_NESTED_WITH_ARRAYS = CqlFilter.of(
            ArrayOperation.of(ArrayOperator.A_CONTAINS,
                              Property.of("theme.concept", ImmutableMap.of("theme", CqlFilter.of(Eq.of("theme.scheme", ScalarLiteral.of("profile"))))),
                              ArrayLiteral.of(ImmutableList.of(ScalarLiteral.of("DLKM"), ScalarLiteral.of("Basis-DLM"), ScalarLiteral.of("DLM50")))));

    public static final CqlFilter EXAMPLE_IN_WITH_TEMPORAL = CqlFilter.of(In.of("updated",
            TemporalLiteral.of("2017-06-10T07:30:00Z"), TemporalLiteral.of("2018-06-10T07:30:00Z"),
            TemporalLiteral.of("2019-06-10T07:30:00Z"), TemporalLiteral.of("2020-06-10T07:30:00Z")));

    public static final CqlFilter EXAMPLE_TRUE = CqlFilter.of(ScalarLiteral.of(true));

    public static final CqlFilter EXAMPLE_BOOLEAN_VALUES = CqlFilter.of(And.of(
        CqlPredicate.of(ScalarLiteral.of(true)),
        CqlPredicate.of(Or.of(
            CqlPredicate.of(ScalarLiteral.of(false)),
            CqlPredicate.of(Not.of(CqlPredicate.of(ScalarLiteral.of(false))))))));

    public static final CqlFilter EXAMPLE_KEYWORD = CqlFilter.of(Gt.of(ImmutableList.of(Property.of("root.date"), TemporalLiteral.of("2022-04-17"))));

    public static final CqlFilter EXAMPLE_CASEI = CqlFilter.of(In.ofFunction(
            Function.of("CASEI", ImmutableList.of(Property.of("road_class"))),
            ImmutableList.of(
                    Function.of("CASEI", ImmutableList.of(ScalarLiteral.of("Οδος"))),
                    Function.of("CASEI", ImmutableList.of(ScalarLiteral.of("Straße"))))
    ));

    public static final CqlFilter EXAMPLE_ACCENTI = CqlFilter.of(In.ofFunction(
            Function.of("ACCENTI", ImmutableList.of(Property.of("road_class"))),
            ImmutableList.of(
                    Function.of("ACCENTI", ImmutableList.of(ScalarLiteral.of("Οδος"))),
                    Function.of("ACCENTI", ImmutableList.of(ScalarLiteral.of("Straße"))))
    ));
}
