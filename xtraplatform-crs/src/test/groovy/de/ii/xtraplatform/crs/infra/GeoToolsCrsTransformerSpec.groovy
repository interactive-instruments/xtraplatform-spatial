/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.crs.infra


import de.ii.xtraplatform.crs.domain.EpsgCrs
import de.ii.xtraplatform.crs.domain.OgcCrs
import spock.lang.Shared
import spock.lang.Specification

class GeoToolsCrsTransformerSpec extends Specification {

    @Shared
    GeoToolsCrsTransformerFactory transformerFactory

    def setupSpec() {
        transformerFactory = new GeoToolsCrsTransformerFactory()
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

}
