/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.crs.infra

import de.ii.xtraplatform.crs.domain.CoordinateTuple
import de.ii.xtraplatform.crs.domain.EpsgCrs
import de.ii.xtraplatform.crs.domain.OgcCrs
import spock.lang.Shared
import spock.lang.Specification

class CrsTransformerProjSpec extends Specification {

    @Shared
    CrsTransformerFactoryProj transformerFactory

    def setupSpec() {
        transformerFactory = new CrsTransformerFactoryProj()
    }

    def 'find transformer - #dim (#src, #trgt)'() {

        when:

        def transformer = transformerFactory.getTransformer(sourceCrs, targetCrs)

        then:

        transformer.isPresent()

        where:

        dim        | src          | trgt         | sourceCrs         | targetCrs
        "2d -> 2d" | "EPSG:25832" | "CRS84"      | EpsgCrs.of(25832) | OgcCrs.CRS84
        "2d -> 2d" | "CRS84"      | "EPSG:25832" | OgcCrs.CRS84      | EpsgCrs.of(25832)
        "3d -> 2d" | "EPSG:5555"  | "CRS84"      | EpsgCrs.of(5555)  | OgcCrs.CRS84
        "3d -> 2d" | "CRS84h"     | "EPSG:25832" | OgcCrs.CRS84h     | EpsgCrs.of(25832)
        "2d -> 3d" | "EPSG:25832" | "CRS84h"     | EpsgCrs.of(25832) | OgcCrs.CRS84h
        "2d -> 3d" | "CRS84"      | "EPSG:5555"  | OgcCrs.CRS84      | EpsgCrs.of(5555)
        "3d -> 3d" | "EPSG:5555"  | "CRS84h"     | EpsgCrs.of(5555)  | OgcCrs.CRS84h
        "3d -> 3d" | "CRS84h"     | "EPSG:5555"  | OgcCrs.CRS84h     | EpsgCrs.of(5555)

    }

    def 'find transformer - #sourceOrTarget CRS is null'() {
        when:
        def transformer = transformerFactory.getTransformer(sourceCrs, targetCrs)

        then:
        thrown(IllegalArgumentException)

        where:
        sourceOrTarget | sourceCrs        | targetCrs
        "target"       | EpsgCrs.of(5555) | null
        "source"       | null             | EpsgCrs.of(5555)

    }


    def 'CRS transformer test 3D'() {
        given:
        EpsgCrs sourceCrs = EpsgCrs.of(5555)
        EpsgCrs targetCrs = EpsgCrs.of(4979)
        double[] source = [420735.071, 5392914.343, 131.96]

        when:
        CrsTransformerProj gct = (CrsTransformerProj) transformerFactory.getTransformer(sourceCrs, targetCrs).get()
        double[] target = gct.transform3d(source, 1, false)

        then:
        target == [48.68423644912392, 7.923077973066287, 131.96] as double[]
    }

    def 'CRS transformer test 2D'() {
        given:
        EpsgCrs sourceCrs = EpsgCrs.of(4326)
        EpsgCrs targetCrs = EpsgCrs.of(3857)
        double x = 50.7164
        double y = 7.086
        double rx = 788809.9117611365
        double ry = 6571280.658009369
        double[] ra = new double[10]
        for (int i = 0; i < 10; i += 2) {
            ra[i] = x
            ra[i + 1] = y
        }

        when:
        CrsTransformerProj gct = (CrsTransformerProj) transformerFactory.getTransformer(sourceCrs, targetCrs).get()
        CoordinateTuple coordinateTuple1 = gct.transform(x, y)
        CoordinateTuple coordinateTuple2 = gct.transform(new CoordinateTuple(x, y), false)
        double[] re = gct.transform(ra, 5, false)

        then:
        coordinateTuple1.getX() == rx
        coordinateTuple1.getY() == ry
        coordinateTuple2.getX() == rx
        coordinateTuple2.getY() == ry
        for (int i = 0; i < 10; i += 2) {
            re[i] == rx
            re[i + 1] == ry
        }
    }

}
