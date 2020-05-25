/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain

import spock.lang.Specification

class CoordinatesParserSpec extends Specification {

    def setupSpec() {

    }

    def '2d -> 2d, one chunk'() {

        given:

        SeperateStringsProcessor writer = Mock()
        CoordinatesParser parser = new CoordinatesParser(writer, 2, 2)
        char[] coordinates = "10.81 10.33, 10.91 20.05".toCharArray()

        when:

        parser.parse(coordinates, 0, coordinates.length)
        parser.close()

        then:

        1 * writer.onStart()
        1 * writer.onX(coordinates,0,5)
        1 * writer.onY(coordinates,6,5)
        1 * writer.onSeparator()
        1 * writer.onX(coordinates,13,5)
        1 * writer.onY("20.05".toCharArray(),0,5)
        1 * writer.onFlush()
        1 * writer.onEnd()
        0 * _
    }

    def '2d -> 2d, two chunks, same array, split between digits '() {

        given:

        SeperateStringsProcessor writer = Mock()
        CoordinatesParser parser = new CoordinatesParser(writer, 2, 2)
        char[] coordinates = "10.81 10.33, 10.91 20.05".toCharArray()
        int chunkBoundary = 10

        when:

        parser.parse(coordinates, 0, chunkBoundary)
        parser.parse(coordinates, chunkBoundary, coordinates.length - chunkBoundary)
        parser.close()

        then:

        1 * writer.onStart()
        1 * writer.onX(coordinates,0,5)
        1 * writer.onY("10.33".toCharArray(),0,5)
        1 * writer.onSeparator()
        1 * writer.onX(coordinates,13,5)
        1 * writer.onY("20.05".toCharArray(),0,5)
        1 * writer.onFlush()
        1 * writer.onEnd()
        0 * _
    }

    def '2d -> 2d, two chunks, same array, split between last digits '() {

        given:

        SeperateStringsProcessor writer = Mock()
        CoordinatesParser parser = new CoordinatesParser(writer, 2, 2)
        char[] coordinates = "10.81 10.33, 10.91 20.05".toCharArray()
        int chunkBoundary = 23

        when:

        parser.parse(coordinates, 0, chunkBoundary)
        parser.parse(coordinates, chunkBoundary, coordinates.length - chunkBoundary)
        parser.close()

        then:

        1 * writer.onStart()
        1 * writer.onX(coordinates,0,5)
        1 * writer.onY(coordinates,6,5)
        1 * writer.onSeparator()
        1 * writer.onX(coordinates,13,5)
        1 * writer.onY("20.05".toCharArray(),0,5)
        1 * writer.onFlush()
        1 * writer.onEnd()
        0 * _
    }

    def '2d -> 2d, two chunks, two arrays, split before separator '() {

        given:

        SeperateStringsProcessor writer = Mock()
        CoordinatesParser parser = new CoordinatesParser(writer, 2, 2)
        char[] coordinates = "10.81 10.33".toCharArray()
        char[] coordinates2 = ", 10.91 20.05".toCharArray()

        when:

        parser.parse(coordinates, 0, coordinates.length)
        parser.parse(coordinates2, 0, coordinates2.length)
        parser.close()

        then:

        1 * writer.onStart()
        1 * writer.onX(coordinates,0,5)
        1 * writer.onY("10.33".toCharArray(),0,5)
        1 * writer.onSeparator()
        1 * writer.onX(coordinates2,2,5)
        1 * writer.onY("20.05".toCharArray(),0,5)
        1 * writer.onFlush()
        1 * writer.onEnd()
        0 * _
    }

    def '2d -> 2d, two chunks, two arrays, split between separators '() {

        given:

        SeperateStringsProcessor writer = Mock()
        CoordinatesParser parser = new CoordinatesParser(writer, 2, 2)
        char[] coordinates = "10.81 10.33,".toCharArray()
        char[] coordinates2 = " 10.91 20.05".toCharArray()

        when:

        parser.parse(coordinates, 0, coordinates.length)
        parser.parse(coordinates2, 0, coordinates2.length)
        parser.close()

        then:

        1 * writer.onStart()
        1 * writer.onX(coordinates,0,5)
        1 * writer.onY(coordinates,6,5)
        1 * writer.onSeparator()
        1 * writer.onX(coordinates2,1,5)
        1 * writer.onY("20.05".toCharArray(),0,5)
        1 * writer.onFlush()
        1 * writer.onEnd()
        0 * _
    }

    def '2d -> 2d, two chunks, two arrays, split after separator '() {

        given:

        SeperateStringsProcessor writer = Mock()
        CoordinatesParser parser = new CoordinatesParser(writer, 2, 2)
        char[] coordinates = "10.81 10.33, ".toCharArray()
        char[] coordinates2 = "10.91 20.05".toCharArray()

        when:

        parser.parse(coordinates, 0, coordinates.length)
        parser.parse(coordinates2, 0, coordinates2.length)
        parser.close()

        then:

        1 * writer.onStart()
        1 * writer.onX(coordinates,0,5)
        1 * writer.onY(coordinates,6,5)
        1 * writer.onSeparator()
        1 * writer.onX(coordinates2,0,5)
        1 * writer.onY("20.05".toCharArray(),0,5)
        1 * writer.onFlush()
        1 * writer.onEnd()
        0 * _
    }

    def '3d -> 3d, one chunk'() {

        given:

        SeperateStringsProcessor writer = Mock()
        CoordinatesParser parser = new CoordinatesParser(writer, 3, 3)
        char[] coordinates = "10.81 10.33 13.49, 10.91 20.05 15.65".toCharArray()

        when:

        parser.parse(coordinates, 0, coordinates.length)
        parser.close()

        then:

        1 * writer.onStart()
        1 * writer.onX(coordinates,0,5)
        1 * writer.onY(coordinates,6,5)
        1 * writer.onZ(coordinates,12,5)
        1 * writer.onSeparator()
        1 * writer.onX(coordinates,19,5)
        1 * writer.onY(coordinates,25,5)
        1 * writer.onZ("15.65".toCharArray(),0,5)
        1 * writer.onFlush()
        1 * writer.onEnd()
        0 * _
    }

    def '3d -> 2d, one chunk'() {

        given:

        SeperateStringsProcessor writer = Mock()
        CoordinatesParser parser = new CoordinatesParser(writer, 3, 2)
        char[] coordinates = "10.81 10.33 13.49, 10.91 20.05 15.65".toCharArray()

        when:

        parser.parse(coordinates, 0, coordinates.length)
        parser.close()

        then:

        1 * writer.onStart()
        1 * writer.onX(coordinates,0,5)
        1 * writer.onY(coordinates,6,5)
        1 * writer.onSeparator()
        1 * writer.onX(coordinates,19,5)
        1 * writer.onY(coordinates,25,5)
        1 * writer.onFlush()
        1 * writer.onEnd()
        0 * _
    }
}
