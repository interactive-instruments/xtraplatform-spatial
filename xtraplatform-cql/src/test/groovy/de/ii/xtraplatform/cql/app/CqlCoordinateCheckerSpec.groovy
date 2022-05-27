/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app

import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.cql.domain.*
import de.ii.xtraplatform.cql.infra.CqlIncompatibleTypes
import de.ii.xtraplatform.crs.domain.CrsInfo
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory
import de.ii.xtraplatform.crs.domain.EpsgCrs
import de.ii.xtraplatform.crs.domain.OgcCrs
import de.ii.xtraplatform.crs.infra.CrsTransformerFactoryProj
import de.ii.xtraplatform.proj.domain.ProjLoaderImpl
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

class CqlCoordinateCheckerSpec extends Specification {

    @Shared
    Cql cql

    @Shared
    CrsTransformerFactoryProj transformerFactory

    @Shared
    CqlCoordinateChecker visitor1

    @Shared
    CqlCoordinateChecker visitor2

    def setupSpec() {
        cql = new CqlImpl()
        transformerFactory = new CrsTransformerFactoryProj(new ProjLoaderImpl(Path.of(System.getProperty("java.io.tmpdir"), "proj", "data")))
        visitor1 = new CqlCoordinateChecker((CrsTransformerFactory) transformerFactory, (CrsInfo) transformerFactory, OgcCrs.CRS84, EpsgCrs.of(5555))
        visitor2 = new CqlCoordinateChecker((CrsTransformerFactory) transformerFactory, (CrsInfo) transformerFactory, OgcCrs.CRS84, OgcCrs.CRS84)
    }

    def 'Valid CRS84 bbox'() {
        given:
        //

        when:
        SIntersects.of(Property.of("bbox"), SpatialLiteral.of(Geometry.Envelope.of(6,48,12, 52,OgcCrs.CRS84))).accept(visitor1)

        then:
        noExceptionThrown()

        and:

        when:
        SIntersects.of(Property.of("bbox"), SpatialLiteral.of(Geometry.Envelope.of(6,48,12, 52,OgcCrs.CRS84))).accept(visitor2)

        then:
        noExceptionThrown()
    }

    def 'min > max'() {
        given:
        //

        when:
        SIntersects.of(Property.of("bbox"), SpatialLiteral.of(Geometry.Envelope.of(6,53,12, 52,OgcCrs.CRS84))).accept(visitor1)

        then:
        thrown IllegalArgumentException

        and:

        when:
        SIntersects.of(Property.of("bbox"), SpatialLiteral.of(Geometry.Envelope.of(6,53,12, 52,OgcCrs.CRS84))).accept(visitor2)

        then:
        thrown IllegalArgumentException
    }

    def 'lat > 90'() {
        given:
        //

        when:
        SIntersects.of(Property.of("bbox"), SpatialLiteral.of(Geometry.Envelope.of(6,10,12, 100,OgcCrs.CRS84))).accept(visitor1)

        then:
        thrown IllegalArgumentException

        and:

        when:
        SIntersects.of(Property.of("bbox"), SpatialLiteral.of(Geometry.Envelope.of(6,10,12, 100,OgcCrs.CRS84))).accept(visitor2)

        then:
        thrown IllegalArgumentException
    }

    def 'crossing antimeridian'() {
        given:
        //

        when:
        SIntersects.of(Property.of("bbox"), SpatialLiteral.of(Geometry.Envelope.of(160,-30,-160, 10,OgcCrs.CRS84))).accept(visitor1)

        then:
        thrown IllegalArgumentException

        and:

        when:
        SIntersects.of(Property.of("bbox"), SpatialLiteral.of(Geometry.Envelope.of(160,-30,-160, 10,OgcCrs.CRS84))).accept(visitor2)

        then:
        noExceptionThrown()
    }

    def 'Basic test'() {
        given:
        String cqlText = "S_INTERSECTS(bbox,POLYGON((6.841676292954137 51.530481422116154,6.910590039820299 51.48142884149749,6.920492602491171 51.47642346300475,6.92048790793338 51.48838138815214,6.920711061968184 51.49719648216943,6.9155177499026191 51.49619902382169,6.915465875139691 51.51941060843487,6.909367346948651 51.52980833786589,6.906686188338441 51.536126767037025,6.841676292954131 51.530481422116154)))"

        when:
        def test = cql.read(cqlText, Cql.Format.TEXT).accept(visitor1)

        then:
        noExceptionThrown()
    }
}
