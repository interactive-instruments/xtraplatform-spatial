/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain

import spock.lang.Specification

class CoordinatesTransformerSpec extends Specification {

    def 'pass through'() {

        given:

        CoordinatesWriter<?> coordinatesWriter = Mock()
        CoordinatesTransformer coordinatesTransformer = ImmutableCoordinatesTransformer.builder()
                                                                                        .sourceDimension(2)
                                                                                        .targetDimension(2)
                                                                                        .coordinatesWriter(coordinatesWriter)
                                                                                        .build()
        String coordinates = "10.81 10.33, 10.91 20.05"

        when:

        coordinatesTransformer.write(coordinates)
        coordinatesTransformer.close()

        then:

        1 * coordinatesWriter.onStart()
        2 * coordinatesWriter.onX(*_)
        2 * coordinatesWriter.onY(*_)
        1 * coordinatesWriter.onSeparator()
        1 * coordinatesWriter.onFlush()
        1 * coordinatesWriter.onEnd()
        0 * _
    }

    def 'transform'() {

        given:

        CoordinatesWriter<?> coordinatesWriter = Mock()
        CoordinatesTransformer coordinatesTransformer = ImmutableCoordinatesTransformer.builder()
                                                                                    .sourceDimension(2)
                                                                                    .targetDimension(2)
                                                                                    .coordinatesWriter(coordinatesWriter)
                                                                                    .isSwapXY(true)
                                                                                    .build()
        String coordinates = "10.81 10.33, 10.91 20.05"

        when:

        coordinatesTransformer.write(coordinates)
        coordinatesTransformer.close()

        then:

        1 * coordinatesWriter.onStart()
        2 * coordinatesWriter.onX(*_)
        2 * coordinatesWriter.onY(*_)
        1 * coordinatesWriter.onSeparator()
        2 * coordinatesWriter.onFlush()
        1 * coordinatesWriter.onEnd()
        0 * _
    }
}
