/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app

import de.ii.xtraplatform.cql.domain.And
import de.ii.xtraplatform.cql.domain.Cql
import de.ii.xtraplatform.cql.domain.Cql2Expression
import de.ii.xtraplatform.cql.domain.Eq
import de.ii.xtraplatform.cql.domain.Gt
import de.ii.xtraplatform.cql.domain.Lte
import de.ii.xtraplatform.cql.domain.Neq
import de.ii.xtraplatform.cql.domain.Not
import de.ii.xtraplatform.cql.domain.Or
import de.ii.xtraplatform.cql.domain.Property
import de.ii.xtraplatform.cql.domain.ScalarLiteral
import spock.lang.Shared
import spock.lang.Specification

class CqlNotMapperSpec extends Specification {

    @Shared
    Cql cql

    def setupSpec() {
        cql = new CqlImpl()
    }

    def 'Test the visitor that maps NOT predicates'() {
        given:
        CqlVisitorMapNots visitor = new CqlVisitorMapNots()

        when:
        def actual = CqlFilterExamples.EXAMPLE_NOT.accept(visitor, true)

        Cql2Expression eq1 = Eq.of(Property.of("test"), ScalarLiteral.of(1))
        Cql2Expression neq1 = Neq.of(Property.of("test1"), ScalarLiteral.of(1))
        Cql2Expression eq2 = Eq.of(Property.of("test2"), ScalarLiteral.of("foo"))
        Cql2Expression lte1 = Lte.of(Property.of("test3"), ScalarLiteral.of("bar"))
        Cql2Expression neq2 = Neq.of(Property.of("test"), ScalarLiteral.of(1))
        Cql2Expression expected = Or.of(eq1, And.of(neq1, eq2, lte1), Or.of(neq1, eq2, lte1), neq2)

        then:
        actual == expected
    }

}
