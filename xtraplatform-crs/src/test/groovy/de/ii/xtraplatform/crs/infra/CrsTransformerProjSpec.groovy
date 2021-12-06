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
import de.ii.xtraplatform.nativ.proj.api.ProjLoaderImpl
import org.kortforsyningen.proj.Units
import org.opengis.referencing.cs.AxisDirection
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

class CrsTransformerProjSpec extends Specification {

    @Shared
    CrsTransformerFactoryProj transformerFactory

    def setupSpec() {
        transformerFactory = new CrsTransformerFactoryProj(new ProjLoaderImpl(Path.of(System.getProperty("java.io.tmpdir"), "proj", "data")))
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
        double[] target = gct.transform(source, 1, 3)

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
        CoordinateTuple coordinateTuple2 = gct.transform(new CoordinateTuple(x, y))
        double[] re = gct.transform(ra, 5, 2)

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

    def 'CRS transformer test 3D to 2D'() {
        when:
        CrsTransformerProj gct = (CrsTransformerProj) transformerFactory.getTransformer(sourceCrs, targetCrs).get()
        double[] result = gct.transform(source as double[], 1, 3)

        then:
        result == target as double[]

        where:
        sourceCrs        | targetCrs         | source                            | target
        EpsgCrs.of(5555) | EpsgCrs.of(25832) | [420735.071, 5392914.343, 131.96] | [420735.071, 5392914.343, 0.0]
        EpsgCrs.of(5555) | EpsgCrs.of(4326)  | [420735.071, 5392914.343, 131.96] | [48.68423644912392, 7.923077973066287, 0.0]
        EpsgCrs.of(5555) | EpsgCrs.of(3857)  | [420735.071, 5392914.343, 131.96] | [881993.0054771411, 6221451.78116173, 0.0]
        EpsgCrs.of(5555) | OgcCrs.CRS84      | [420735.071, 5392914.343, 131.96] | [7.923077973066287, 48.68423644912392, 0.0]

        EpsgCrs.of(4979) | EpsgCrs.of(25832) | [48.684, 7.923, 131.96]           | [420728.9609056481, 5392888.1411416, 0.0]
        EpsgCrs.of(4979) | EpsgCrs.of(4326)  | [48.684, 7.923, 131.96]           | [48.684, 7.923, 0.0]
        EpsgCrs.of(4979) | EpsgCrs.of(3857)  | [48.684, 7.923, 131.96]           | [881984.3255551065, 6221411.912936983, 0.0]
        EpsgCrs.of(4979) | OgcCrs.CRS84      | [48.684, 7.923, 131.96]           | [7.923, 48.684, 0.0]

        OgcCrs.CRS84h    | EpsgCrs.of(25832) | [7.923, 48.684, 131.96]           | [420728.9609056481, 5392888.1411416, 0.0]
        OgcCrs.CRS84h    | EpsgCrs.of(4326)  | [7.923, 48.684, 131.96]           | [48.684, 7.923, 0.0]
        OgcCrs.CRS84h    | EpsgCrs.of(3857)  | [7.923, 48.684, 131.96]           | [881984.3255551065, 6221411.912936983, 0.0]
        OgcCrs.CRS84h    | OgcCrs.CRS84      | [7.923, 48.684, 131.96]           | [7.923, 48.684, 0.0]


    }

    def 'CRS info test'() {

        expect:
        transformerFactory.getAxisAbbreviations(crs) == axisAbbreviations
        transformerFactory.getAxisUnits(crs) == axisUnits
        transformerFactory.getAxisDirections(crs) == axisDirections
        transformerFactory.getAxisRangeMeanings(crs) == axisRangeMeanings

        where:
        crs               | axisAbbreviations   | axisUnits                                 | axisDirections                                              | axisRangeMeanings
        EpsgCrs.of(5555)  | ["E", "N", "H"]     | [Units.METRE, Units.METRE, Units.METRE]   | [AxisDirection.EAST, AxisDirection.NORTH, AxisDirection.UP] | [Optional.empty(), Optional.empty(), Optional.empty()]
        EpsgCrs.of(5556)  | ["E", "N", "H"]     | [Units.METRE, Units.METRE, Units.METRE]   | [AxisDirection.EAST, AxisDirection.NORTH, AxisDirection.UP] | [Optional.empty(), Optional.empty(), Optional.empty()]
        EpsgCrs.of(4979)  | ["Lat", "Lon", "h"] | [Units.DEGREE, Units.DEGREE, Units.METRE] | [AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP] | [Optional.empty(), Optional.empty(), Optional.empty()]
        EpsgCrs.of(25832) | ["E", "N"]          | [Units.METRE, Units.METRE]                | [AxisDirection.EAST, AxisDirection.NORTH]                   | [Optional.empty(), Optional.empty()]
        EpsgCrs.of(4326)  | ["Lat", "Lon"]      | [Units.DEGREE, Units.DEGREE]              | [AxisDirection.NORTH, AxisDirection.EAST]                   | [Optional.empty(), Optional.empty()]
        EpsgCrs.of(3857)  | ["X", "Y"]          | [Units.METRE, Units.METRE]                | [AxisDirection.EAST, AxisDirection.NORTH]                   | [Optional.empty(), Optional.empty()]
        EpsgCrs.of(4269)  | ["Lat", "Lon"]      | [Units.DEGREE, Units.DEGREE]              | [AxisDirection.NORTH, AxisDirection.EAST]                   | [Optional.empty(), Optional.empty()]
        OgcCrs.CRS84      | ["Lon", "Lat"]      | [Units.DEGREE, Units.DEGREE]              | [AxisDirection.EAST, AxisDirection.NORTH]                   | [Optional.empty(), Optional.empty()]
        OgcCrs.CRS84h     | ["Lon", "Lat", "h"] | [Units.DEGREE, Units.DEGREE, Units.METRE] | [AxisDirection.EAST, AxisDirection.NORTH, AxisDirection.UP] | [Optional.empty(), Optional.empty(), Optional.empty()]

    }

    def 'Compound CRS test'() {

        when:
        CrsTransformerProj gct = (CrsTransformerProj) transformerFactory.getTransformer(sourceCrs, targetCrs).get()
        double[] result = gct.transform(source as double[], 1, 3)

        then:
        result == target as double[]

        where:
        sourceCrs               | targetCrs               | source                            | target

        EpsgCrs.of(5555)        | EpsgCrs.of(25832, 7837) | [420735.071, 5392914.343, 131.96] | [420735.071, 5392914.343, 131.96]
        EpsgCrs.of(4979)        | EpsgCrs.of(25832, 7837) | [48.684, 7.923, 131.96]           | [420728.9609056481, 5392888.1411416, 131.96]
        OgcCrs.CRS84h           | EpsgCrs.of(25832, 7837) | [7.923, 48.684, 131.96]           | [420728.9609056481, 5392888.1411416, 131.96]

        EpsgCrs.of(25832, 7837) | EpsgCrs.of(5555)        | [420735.071, 5392914.343, 131.96] | [420735.071, 5392914.343, 131.96]
        EpsgCrs.of(25832, 7837) | EpsgCrs.of(4979)        | [420735.071, 5392914.343, 131.96] | [48.68423644912392, 7.923077973066287, 131.96]
        EpsgCrs.of(25832, 7837) | OgcCrs.CRS84h           | [420735.071, 5392914.343, 131.96] | [7.923077973066287, 48.68423644912392, 131.96]

    }

}
