/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain

import spock.lang.Specification

class ReverseLineSpec extends Specification {

    def '2d'() {

        given:

        DoubleArrayProcessor next = Mock()
        ReverseLine reverseLine = ImmutableReverseLine.of(next)
        double[] coordinates = [10.0, 10.0, 11.0, 11.0, 12.0, 9.0, 13.0, 10.0]
        int dimension = 2

        when:

        reverseLine.onCoordinates(coordinates, coordinates.length, dimension)

        then:

        1 * next.onCoordinates([13.0, 10.0, 12.0, 9.0, 11.0, 11.0, 10.0, 10.0], coordinates.length, dimension)
        0 * _
    }

    def '3d'() {

        given:

        DoubleArrayProcessor next = Mock()
        ReverseLine reverseLine = ImmutableReverseLine.of(next)
        double[] coordinates = [10.0, 10.0, 10.0, 11.0, 11.0, 10.0, 12.0, 9.0, 10.0, 13.0, 10.0, 10.0]
        int dimension = 3

        when:

        reverseLine.onCoordinates(coordinates, coordinates.length, dimension)

        then:

        1 * next.onCoordinates([13.0, 10.0, 10.0, 12.0, 9.0, 10.0, 11.0, 11.0, 10.0, 10.0, 10.0, 10.0], coordinates.length, dimension)
        0 * _
    }
}
