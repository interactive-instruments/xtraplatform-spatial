/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app

import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.cql.domain.Cql
import spock.lang.Shared
import spock.lang.Specification

class CqlFunctionCheckerSpec extends Specification {

    @Shared
    Cql cql

    def setupSpec() {
        cql = new CqlImpl()
    }

    def 'Test the function-checking visitor'() {
        given:
        // run the test on 2 different queries to make sure that old function names are removed
        def allowedFunctions = ImmutableMap.of("indexOf", 2, "pos", 1)
        CqlFunctionChecker visitor = new CqlFunctionChecker(allowedFunctions)

        when:
        def invalidFunctions = CqlFilterExamples.EXAMPLE_31.accept(visitor)

        then:
        invalidFunctions.size() == 1
        invalidFunctions.containsKey("year")

        and:

        when:
        def invalidFunctions2 = CqlFilterExamples.EXAMPLE_29.accept(visitor)

        then:
        invalidFunctions2.size() == 1
        invalidFunctions2.containsKey("pos")
    }

}
