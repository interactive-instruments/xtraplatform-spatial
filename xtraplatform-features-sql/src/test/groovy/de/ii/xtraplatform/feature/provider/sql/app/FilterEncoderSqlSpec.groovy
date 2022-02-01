/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app


import de.ii.xtraplatform.cql.app.CqlFilterExamples
import de.ii.xtraplatform.cql.app.CqlImpl
import de.ii.xtraplatform.crs.domain.OgcCrs
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

class FilterEncoderSqlSpec extends Specification {

    @Shared
    FilterEncoderSql filterEncoder

    def setupSpec() {
        filterEncoder = new FilterEncoderSql(OgcCrs.CRS84, new SqlDialectPostGis(), null, new CqlImpl(), null)
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

    def 'temporal operation, instant'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INSTANT
        def filter = CqlFilterExamples.EXAMPLE_12

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.built::timestamp(0) < TIMESTAMP '2012-06-05T00:00:00Z')"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'temporal operation, interval'() {

        given:
        def instanceContainer = QuerySchemaFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_14

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA WHERE AA.updated::timestamp(0) BETWEEN TIMESTAMP '2017-06-10T07:30:00Z' AND TIMESTAMP '2017-06-11T10:30:00Z')"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }



}
