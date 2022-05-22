/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app

import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.cql.domain.Cql
import de.ii.xtraplatform.cql.domain.CqlFilter
import de.ii.xtraplatform.cql.domain.CqlPredicate
import de.ii.xtraplatform.cql.domain.Or
import de.ii.xtraplatform.cql.domain.Lt
import de.ii.xtraplatform.cql.domain.Gt
import de.ii.xtraplatform.cql.domain.Property
import de.ii.xtraplatform.cql.domain.ScalarLiteral
import de.ii.xtraplatform.cql.infra.CqlIncompatibleTypes
import spock.lang.Shared
import spock.lang.Specification

class CqlTypeCheckerSpec extends Specification {

    @Shared
    Cql cql

    @Shared
    CqlTypeChecker visitor

    def setupSpec() {
        cql = new CqlImpl()
        def propertyTypes = ImmutableMap.of(
                "road_class", "STRING",
                "count", "LONG",
                "length", "FLOAT",
                "begin", "DATE",
                "end", "DATE",
                "event", "DATE")
        visitor = new CqlTypeChecker(propertyTypes, cql)
    }

    def 'Test the predicate-checking visitor'() {
        given:
        // run the test on 2 different queries to make sure that old reports are removed

        when:
                Or.of(
                        Lt.of(Property.of("length"), ScalarLiteral.of(1)),
                        Lt.of(Property.of("length"), Property.of("count")),
                        Gt.of(Property.of("road_class"), ScalarLiteral.of(1))
                ).accept(visitor)

        then:
        thrown CqlIncompatibleTypes

        and:

        when:
        Gt.of(Property.of("road_class"), ScalarLiteral.of("1")).accept(visitor)

        then:
        noExceptionThrown()
    }

    def 'CASEI must be Strings'() {
        given:
        String cqlText = "CASEI(length) IN (CASEI('Οδος'), CASEI('Straße'))"

        when: 'reading text'
        def test = cql.read(cqlText, Cql.Format.TEXT).accept(visitor)

        then:
        thrown CqlIncompatibleTypes
    }

    // these are tested with a different visitor
    def 'Ignore invalid properties'() {
        given:
        String cqlText = "dummy > event"

        when: 'reading text'
        def test = cql.read(cqlText, Cql.Format.TEXT).accept(visitor)

        then:
        noExceptionThrown()
    }

    def 'temporal'() {
        given:
        String cqlText = "T_INTERSECTS(TIMESTAMP('2011-12-26T20:55:27Z'), INTERVAL('2011-01-01','2011-12-31')) OR " +
                "T_INTERSECTS(INTERVAL('..','2011-12-31'), INTERVAL(begin,end)) OR " +
                "T_BEFORE(event,DATE('2000-01-01'))"

        when: 'reading text'
        def test = cql.read(cqlText, Cql.Format.TEXT).accept(visitor)

        then:
        noExceptionThrown()
    }

    def 'interval of date and timestamp'() {
        given:
        String cqlText = "T_INTERSECTS(TIMESTAMP('2011-12-26T20:55:27Z'), INTERVAL('2011-01-01','2011-12-31T23:59:59Z')) OR " +
                "T_INTERSECTS(INTERVAL('..','2011-12-31'), INTERVAL(begin,end)) OR " +
                "T_BEFORE(event,DATE('2000-01-01'))"

        when: 'reading text'
        def test = cql.read(cqlText, Cql.Format.TEXT).accept(visitor)

        then:
        thrown CqlIncompatibleTypes
    }

}
