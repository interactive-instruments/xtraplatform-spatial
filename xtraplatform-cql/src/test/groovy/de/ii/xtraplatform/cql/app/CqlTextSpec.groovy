package de.ii.xtraplatform.cql.app


import de.ii.xtraplatform.cql.domain.Cql
import de.ii.xtraplatform.cql.domain.CqlPredicate
import spock.lang.Shared
import spock.lang.Specification

class CqlTextSpec extends Specification {

    @Shared
    Cql cql

    def setupSpec() {
        cql = new CqlImpl()
    }

    def 'Floors greater than 5'() {

        given:
        String cqlText = "floors > 5"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlPredicateExamples.EXAMPLE_1

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_1, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'More than 5 floors and a swimming pool'() {

        given:
        String cqlText = "floors > 5 AND swimming_pool = true"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlPredicateExamples.EXAMPLE_7

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_7, Cql.Format.TEXT)

        then:
        actual2 == cqlText

    }

    def 'Updated between 7:30am June 10, 2017 and 10:30am June 11, 2017'() {

        given:
        String cqlText = "updated DURING 2017-06-10T07:30:00Z/2017-06-11T10:30:00Z"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlPredicateExamples.EXAMPLE_14

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_14, Cql.Format.TEXT)

        then:
        actual2 == cqlText

    }

    def 'Location that intersects with geometry'() {

        given:
        String cqlText = "INTERSECTS(location, POLYGON((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0)))"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlPredicateExamples.EXAMPLE_16

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_16, Cql.Format.TEXT)

        then:
        actual2 == cqlText

    }


}
