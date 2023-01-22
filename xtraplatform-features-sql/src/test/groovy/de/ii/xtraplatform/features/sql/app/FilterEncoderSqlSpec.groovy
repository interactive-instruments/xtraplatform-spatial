/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app


import de.ii.xtraplatform.cql.app.CqlFilterExamples
import de.ii.xtraplatform.cql.app.CqlImpl
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory
import de.ii.xtraplatform.crs.domain.OgcCrs
import de.ii.xtraplatform.crs.infra.CrsTransformerFactoryProj
import de.ii.xtraplatform.features.sql.domain.SqlDialectPostGis
import de.ii.xtraplatform.proj.domain.ProjLoaderImpl
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

class FilterEncoderSqlSpec extends Specification {

    @Shared
    FilterEncoderSql filterEncoder

    @Shared
    FilterEncoderSql filterEncoder2

    def setupSpec() {

        filterEncoder = new FilterEncoderSql(OgcCrs.CRS84, new SqlDialectPostGis(), null, null, new CqlImpl(), null)

        CrsTransformerFactoryProj transformerFactory = new CrsTransformerFactoryProj(new ProjLoaderImpl(Path.of(System.getProperty("java.io.tmpdir"), "proj", "data")))
        transformerFactory.onStart()
        filterEncoder2 = new FilterEncoderSql(OgcCrs.CRS84, new SqlDialectPostGis(), (CrsTransformerFactory) transformerFactory, null, new CqlImpl(), null)

    }

    def 'spatial operation, envelope, no join'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_GEOMETRY
        def filter = CqlFilterExamples.EXAMPLE_15

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE ST_Within(AA.location, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326)))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'spatial operation, polygon, 1:n join'() {

        given:
        def instanceContainer = QuerySchemaFixtures.JOINED_GEOMETRY
        def filter = CqlFilterExamples.EXAMPLE_16

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA JOIN geometry AB ON (AA.id=AB.id) WHERE ST_Intersects(AB.location, ST_GeomFromText('POLYGON((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0))',4326)))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'spatial operation, envelope, no join with transformed coordinates'() {

        given:

        def instanceContainer = QuerySchemaFixtures.SIMPLE_GEOMETRY
        def filter = CqlFilterExamples.EXAMPLE_15_RandomCrs

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE ST_Within(AA.location, ST_GeomFromText('POLYGON((33.8 -118.0,33.8 -117.9,34.0 -117.9,34.0 -118.0,33.8 -118.0))',4326)))"

        String actual = filterEncoder2.encode(filter, instanceContainer)

        then:

        actual == expected
    }

    def 'temporal operation, timestamp'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_TIMESTAMP
        def filter = CqlFilterExamples.EXAMPLE_12

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.built::timestamp(0) < TIMESTAMP '2012-06-05T00:00:00Z')"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'temporal operation, date'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_DATE
        def filter = CqlFilterExamples.EXAMPLE_12_date

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.built::date < DATE '2012-06-05')"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'temporal operation, interval'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_14

        when:
        String expected = "(A.id IN (SELECT AA.id FROM building AA WHERE AA.updated::timestamp(0) > TIMESTAMP '2017-06-10T07:30:00Z') AND A.id IN (SELECT AA.id FROM building AA WHERE AA.updated::timestamp(0) < TIMESTAMP '2017-06-11T10:30:00Z'))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'Not operation'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_14_Negation

        when:
        String expected = "NOT ((A.id IN (SELECT AA.id FROM building AA WHERE AA.updated::timestamp(0) > TIMESTAMP '2017-06-10T07:30:00Z') AND A.id IN (SELECT AA.id FROM building AA WHERE AA.updated::timestamp(0) < TIMESTAMP '2017-06-11T10:30:00Z')))"
        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def ' operation test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_17

        when:
        String expected = "(A.id IN (SELECT AA.id FROM building AA WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM building AA WHERE ST_Within(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))))"
        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'In operation test 2'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_18

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.floors BETWEEN 4 AND 8)"
        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'In list test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_19

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.owner IN ('Mike','John','Tom'))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'In list test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_19

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.owner IN ('Mike','John','Tom'))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'is Null test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_20

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.owner IS NULL)"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'Not in list test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_40

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.owner NOT IN ('Mike','John','Tom'))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'like test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_3

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.owner::varchar LIKE '% Jones %')"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'CASEI test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_CASEI

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE LOWER(AA.road_class) IN ('οδος','straße'))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'ACCENTI test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_ACCENTI

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE LOWER(AA.road_class) IN ('οδος','straße'))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        thrown(IllegalArgumentException)

    }

    def 'Polygon test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_GEOMETRY
        def filter = CqlFilterExamples.EXAMPLE_16

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE ST_Intersects(AA.location, ST_GeomFromText('POLYGON((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0))',4326)))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'Interval list test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_GEOMETRY
        def filter = CqlFilterExamples.EXAMPLE_25z

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE (COALESCE(AA.start,TIMESTAMP '-infinity'), TIMESTAMP 'infinity') OVERLAPS (DATE '2017-06-10',TIMESTAMP 'infinity'))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'temnpdssdsoral test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_DATE
        def filter = CqlFilterExamples.EXAMPLE_25y

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE (COALESCE(AA.start,TIMESTAMP '-infinity'), COALESCE(AA.end,TIMESTAMP 'infinity')) OVERLAPS (DATE '2017-06-10',DATE '2017-06-12'))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'MultiPolygon test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_GEOMETRY
        def filter = CqlFilterExamples.EXAMPLE_16_MultiPolygon

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE ST_Intersects(AA.location, ST_GeomFromText('MULTIPOLYGON(((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0)),((-15.0 -15.0,15.0 -15.0,15.0 15.0,-15.0 -15.0)))',4326)))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'MultiLineString test'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_GEOMETRY
        def filter = CqlFilterExamples.EXAMPLE_16_MultiLineString

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE ST_Intersects(AA.location, ST_GeomFromText('MULTILINESTRING((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0),(-15.0 -15.0,15.0 -15.0,15.0 15.0,-15.0 -15.0))',4326)))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }


}
