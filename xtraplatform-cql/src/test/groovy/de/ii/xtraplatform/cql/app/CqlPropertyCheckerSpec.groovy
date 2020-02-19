package de.ii.xtraplatform.cql.app

import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.cql.domain.Cql
import spock.lang.Shared
import spock.lang.Specification

class CqlPropertyCheckerSpec extends Specification {

    @Shared
    Cql cql

    def setupSpec() {
        cql = new CqlImpl()
    }

    def 'Test the property-checking visitor'() {
        given:
        // run the test on 2 different queries to make sure that old properties are removed
        def allowedProperties = ImmutableList.of("doors", "floors")
        CqlPropertyChecker visitor = new CqlPropertyChecker(allowedProperties)

        when:
        def invalidProperties = CqlFilterExamples.EXAMPLE_10.accept(visitor)

        then:
        invalidProperties.size() == 1
        invalidProperties.get(0) == "swimming_pool"

        and:

        when:
        def invalidProperties2 = CqlFilterExamples.EXAMPLE_23.accept(visitor)

        then:
        invalidProperties2.size() == 1
        invalidProperties2.get(0) == "owner"
    }

}
