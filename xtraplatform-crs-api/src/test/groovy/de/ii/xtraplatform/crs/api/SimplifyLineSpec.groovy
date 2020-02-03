package de.ii.xtraplatform.crs.api

import spock.lang.Specification

class SimplifyLineSpec extends Specification {

    def '2d'() {

        given:

        DoubleArrayProcessor next = Mock()
        SimplifyLine simplifyLine = ImmutableSimplifyLine.of(next, 1.0, 0)
        double[] coordinates = [10.0, 10.0, 11.0, 11.0, 12.0, 9.0, 13.0, 10.0]
        int dimension = 2

        when:

        simplifyLine.onCoordinates(coordinates, coordinates.length, dimension)

        then:

        1 * next.onCoordinates([10.0, 10.0, 13.0, 10.0], 4, dimension)
        0 * _
    }

    def '3d'() {

        given:

        DoubleArrayProcessor next = Mock()
        SimplifyLine simplifyLine = ImmutableSimplifyLine.of(next, 1.0, 0)
        double[] coordinates = [10.0, 10.0, 10.0, 11.0, 11.0, 10.0, 12.0, 9.0, 10.0, 13.0, 10.0, 10.0]
        int dimension = 3

        when:

        simplifyLine.onCoordinates(coordinates, coordinates.length, dimension)

        then:

        1 * next.onCoordinates([10.0, 10.0, 10.0, 13.0, 10.0, 10.0], 6, dimension)
        0 * _
    }

    def 'no op'() {

        given:

        DoubleArrayProcessor next = Mock()
        SimplifyLine simplifyLine = ImmutableSimplifyLine.of(next, 0.99, 0)
        double[] coordinates = [10.0, 10.0, 11.0, 11.0, 12.0, 9.0, 13.0, 10.0]
        int dimension = 2

        when:

        simplifyLine.onCoordinates(coordinates, coordinates.length, dimension)

        then:

        1 * next.onCoordinates(coordinates, coordinates.length, dimension)
        0 * _
    }

    def 'min points'() {

        given:

        DoubleArrayProcessor next = Mock()
        SimplifyLine simplifyLine = ImmutableSimplifyLine.of(next, 1.35, 3)
        double[] coordinates = [10.0, 10.0, 11.0, 11.0, 12.0, 9.0, 13.0, 10.0]
        int dimension = 2

        when:

        simplifyLine.onCoordinates(coordinates, coordinates.length, dimension)

        then:

        1 * next.onCoordinates([10.0, 10.0, 12.0, 9.0, 13.0, 10.0], 6, dimension)
        0 * _
    }
}
