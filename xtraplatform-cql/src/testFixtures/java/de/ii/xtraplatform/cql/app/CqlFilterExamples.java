/**
 * Copyright 2021 interactive instruments GmbH
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

    static final CqlFilter EXAMPLE_1 = CqlFilter.of(Gt.of("floors", ScalarLiteral.of(5)));

    static final CqlFilter EXAMPLE_2 = CqlFilter.of(Lte.of("taxes", ScalarLiteral.of(500)));

    static final CqlFilter EXAMPLE_3 = CqlFilter.of(Like.of("owner", ScalarLiteral.of("% Jones %")));

    static final CqlFilter EXAMPLE_4 = CqlFilter.of(Like.of("owner", ScalarLiteral.of("Mike%"), "%", null, null, null));

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

    public static final CqlFilter EXAMPLE_15 = CqlFilter.of(Within.of("location", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0, OgcCrs.CRS84))));

    public static final CqlFilter EXAMPLE_16 = CqlFilter.of(Intersects.of("location", SpatialLiteral.of(Geometry.Polygon.of(OgcCrs.CRS84, ImmutableList.of(
            Geometry.Coordinate.of(-10.0, -10.0),
            Geometry.Coordinate.of(10.0, -10.0),
            Geometry.Coordinate.of(10.0, 10.0),
            Geometry.Coordinate.of(-10.0, -10.0)
    )))));

    static final CqlFilter EXAMPLE_17 = CqlFilter.of(And.of(
            EXAMPLE_1,
            CqlPredicate.of(Within.of("geometry", SpatialLiteral.of(Geometry.Envelope.of(-118.0, 33.8, -117.9, 34.0, OgcCrs.CRS84))))
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

    static final CqlFilter EXAMPLE_32 = CqlFilter.of(
            Gt.of(Property.of("filterValues.measure",
                            ImmutableMap.of("filterValues", CqlFilter.of(Eq.of("filterValues.property", ScalarLiteral.of("d30"))))),
                    ScalarLiteral.of(0.1)));

    static final CqlFilter EXAMPLE_33 = CqlFilter.of(
            Gt.of(Property.of("filterValues1.filterValues2.measure",
                            ImmutableMap.of("filterValues1", CqlFilter.of(Eq.of("filterValues1.property1", ScalarLiteral.of("d30"))),
                                    "filterValues2", CqlFilter.of(Lte.of("filterValues2.property2", ScalarLiteral.of(100))))),
                    ScalarLiteral.of(0.1)));

    static final CqlFilter EXAMPLE_34 = CqlFilter.of(Eq.of("landsat:scene_id", ScalarLiteral.of("LC82030282019133LGN00")));

    static final CqlFilter EXAMPLE_35 = CqlFilter.of(Like.of("name", ScalarLiteral.of("Smith."), null, ".", "+", false));

    static final CqlFilter EXAMPLE_36 = CqlFilter.of(AnyInteracts.of("event_date", getTemporalLiteral("1969-07-16T05:32:00Z/1969-07-24T16:50:35Z")));

    static final CqlFilter EXAMPLE_37 = CqlFilter.of(Lt.of("height", Property.of("floors")));

    static final CqlFilter EXAMPLE_38 = CqlFilter.of(AContains.of("layer:ids", ArrayLiteral.of("['layers-ca','layers-us']")));

    static final CqlFilter EXAMPLE_39 = CqlFilter.of(Not.of(Between.of("floors", ScalarLiteral.of(4), ScalarLiteral.of(8))));

    static final CqlFilter EXAMPLE_40 = CqlFilter.of(Not.of(In.of("owner", ScalarLiteral.of("Mike"), ScalarLiteral.of("John"), ScalarLiteral.of("Tom"))));


    private static TemporalLiteral getTemporalLiteral(String temporalData) {
        try {
            return TemporalLiteral.of(temporalData);
        } catch (CqlParseException e) {
            e.printStackTrace();
        }
        return null;
    }


}
