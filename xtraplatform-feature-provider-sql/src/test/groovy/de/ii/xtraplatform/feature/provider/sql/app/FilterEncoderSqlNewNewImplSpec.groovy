/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.cql.app.CqlFilterExamples
import de.ii.xtraplatform.crs.domain.OgcCrs
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer
import de.ii.xtraplatform.features.domain.FeatureStoreRelatedContainer
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.BiFunction
import java.util.function.Function

class FilterEncoderSqlNewNewImplSpec extends Specification {
/*
    @Shared
    FilterEncoderSqlNewNewImpl filterEncoder

    def setupSpec() {
        Function<FeatureStoreAttributesContainer, List<String>> aliasesGenerator = { attributesContainer -> ImmutableList.of("A") }
        BiFunction<FeatureStoreAttributesContainer, List<String>, String> joinsGenerator = { attributesContainer, aliases -> attributesContainer instanceof FeatureStoreRelatedContainer ? String.format('JOIN %1$s %2$s ON %4$s.%5$s=%2$s.%3$s', "geometry", "AB", "id", "AA", "id") : "" }
        filterEncoder = new FilterEncoderSqlNewNewImpl(aliasesGenerator, joinsGenerator, OgcCrs.CRS84, new SqlDialectPostGis())
    }

    def 'spatial operation, envelope, no join'() {

        given:
        def instanceContainer = FeatureStoreFixtures.SIMPLE_GEOMETRY
        def filter = CqlFilterExamples.EXAMPLE_15

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA  WHERE ST_Within(AA.location, ST_GeomFromText('POLYGON((33.8 -118.0,34.0 -118.0,34.0 -117.9,33.8 -117.9,33.8 -118.0))',4326)))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'spatial operation, polygon, 1:n join'() {

        given:
        def instanceContainer = FeatureStoreFixtures.JOINED_GEOMETRY
        def filter = CqlFilterExamples.EXAMPLE_16

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA JOIN geometry AB ON AA.id=AB.id WHERE ST_Intersects(AA.location, ST_GeomFromText('POLYGON((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0))',4326)))"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'temporal operation, instant'() {

        given:
        def instanceContainer = FeatureStoreFixtures.SIMPLE_INSTANT
        def filter = CqlFilterExamples.EXAMPLE_12

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA  WHERE AA.built < '2015-01-01T00:00:00Z')"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

    def 'temporal operation, interval'() {

        given:
        def instanceContainer = FeatureStoreFixtures.SIMPLE_INTERVAL
        def filter = CqlFilterExamples.EXAMPLE_14

        when:
        String expected = "A.id IN (SELECT AA.id FROM building AA  WHERE AA.updated BETWEEN '2017-06-10T07:30:00Z' AND '2017-06-11T10:30:00Z')"

        String actual = filterEncoder.encode(filter, instanceContainer)

        then:

        actual == expected

    }

 */

}
