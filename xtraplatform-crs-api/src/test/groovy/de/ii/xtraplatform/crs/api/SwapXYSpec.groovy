package de.ii.xtraplatform.crs.api

import spock.lang.Specification

class SwapXYSpec extends Specification {

    def '2d'() {

        given:

        DoubleArrayProcessor next = Mock()
        SwapXY swapXY = ImmutableSwapXY.of(next)
        double[] coordinates = [10.81, 10.33, 10.91, 20.05]
        int dimension = 2

        when:

        swapXY.onCoordinates(coordinates, coordinates.length, dimension)

        then:

        1 * next.onCoordinates([10.33, 10.81, 20.05, 10.91], coordinates.length, dimension)
        0 * _
    }

    def '3d'() {

        given:

        DoubleArrayProcessor next = Mock()
        SwapXY swapXY = ImmutableSwapXY.of(next)
        double[] coordinates = [10.81, 10.33, 100.0, 10.91, 20.05, 110.0]
        int dimension = 3

        when:

        swapXY.onCoordinates(coordinates, coordinates.length, dimension)

        then:

        1 * next.onCoordinates([10.33, 10.81, 100.0, 20.05, 10.91, 110.0], coordinates.length, dimension)
        0 * _
    }
}
