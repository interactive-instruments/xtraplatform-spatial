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
import de.ii.xtraplatform.cql.domain.ScalarLiteral
import spock.lang.Shared
import spock.lang.Specification

class CqlTypeCheckerSpec extends Specification {

    @Shared
    Cql cql

    @Shared
    CqlTypeChecker visitor

    def setupSpec() {
        cql = new CqlImpl()
        def propertyTypes = ImmutableMap.of("road_class", "STRING", "length", "FLOAT")
        visitor = new CqlTypeChecker(propertyTypes, cql)
    }

    def 'Test the predicate-checking visitor'() {
        given:
        // run the test on 2 different queries to make sure that old reports are removed

        when:
        def test1 = CqlFilter.of(
                Or.of(
                        CqlPredicate.of(Gt.of("road_class", ScalarLiteral.of(1))),
                        CqlPredicate.of(Lt.of("length", ScalarLiteral.of(1)))
                )).accept(visitor)

        then:
        test1.size() == 1
        test1.get(0).startsWith("road_class > 1")

        and:

        when:
        def test2 = CqlFilter.of(Gt.of("road_class", ScalarLiteral.of("1"))).accept(visitor)

        then:
        test2.size() == 0
    }

    def 'CASEI must be Strings'() {
        given:
        String cqlText = "CASEI(length) IN (CASEI('Οδος'), CASEI('Straße'))"

        when: 'reading text'
        def test = cql.read(cqlText, Cql.Format.TEXT).accept(visitor)

        then:
        test.size() == 1
        test.get(0).startsWith("CASEI(length);")
    }

    // these are tested with a different visitor
    def 'Ignore invalid properties'() {
        given:
        String cqlText = "dummy = 'dummy'"

        when: 'reading text'
        def test = cql.read(cqlText, Cql.Format.TEXT).accept(visitor)

        then:
        test.size() == 0
    }

}
