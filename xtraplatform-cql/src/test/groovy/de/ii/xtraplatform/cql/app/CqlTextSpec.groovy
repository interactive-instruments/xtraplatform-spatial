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
        actual == CqlFilterExamples.EXAMPLE_1

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_1, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Taxes less than or equal to 500'() {

        given:
        String cqlText = "taxes <= 500"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_2

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_2, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Owner name contains "Jones"'() {

        given:
        String cqlText = "owner LIKE '% Jones %'"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_3

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_3, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Owner name starts with "Mike"'() {

        given:
        String cqlText = "owner LIKE 'Mike%'"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_4

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_4, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }


    def 'Owner name does not contain "Mike"'() {

        given:
        String cqlText = "owner NOT LIKE '% Mike %'"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_5

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_5, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }


    def 'A swimming pool'() {

        given:
        String cqlText = "swimming_pool = true"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_6

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_6, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }


    def 'More than 5 floors and a swimming pool'() {

        given:
        String cqlText = "floors > 5 AND swimming_pool = true"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_7

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_7, Cql.Format.TEXT)

        then:
        actual2 == cqlText

    }

    def 'A swimming pool and (more than five floors or material is brick)'() {

        given:
        String cqlText = "swimming_pool = true AND (floors > 5 OR material LIKE 'brick%' OR material LIKE '%brick')"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_8

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_8, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def '[More than five floors and material is brick] or swimming pool is true'() {

        given:
        String cqlText = "(floors > 5 AND material = 'brick') OR swimming_pool = true"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_9

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_9, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Not under 5 floors or a swimming pool'() {

        given:
        String cqlText = "NOT (floors < 5) OR swimming_pool = true"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_10

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_10, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Owner name starts with "mike" or "Mike" and is less than 4 floors'() {

        given:
        String cqlText = "(owner LIKE 'mike%' OR owner LIKE 'Mike%') AND floors < 4"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_11

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_11, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Built before 2015'() {

        given:
        String cqlText = "built BEFORE 2015-01-01T00:00:00Z"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_12

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_12, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Built after June 5, 2012'() {

        given:
        String cqlText = "built AFTER 2012-06-05T00:00:00Z"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_13

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_13, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Updated between 7:30am June 10, 2017 and 10:30am June 11, 2017'() {

        given:
        String cqlText = "updated DURING 2017-06-10T07:30:00Z/2017-06-11T10:30:00Z"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_14

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_14, Cql.Format.TEXT)

        then:
        actual2 == cqlText

    }

    def 'Location in the box between -118,33.8 and -117.9,34 in lat/long (geometry 1)'() {

        given:
        String cqlText = "WITHIN(location, ENVELOPE(33.8,-118.0,34.0,-117.9))"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_15

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_15, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Location that intersects with geometry'() {

        given:
        String cqlText = "INTERSECTS(location, POLYGON((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0)))"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_16

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_16, Cql.Format.TEXT)

        then:
        actual2 == cqlText

    }

    def 'More than 5 floors and is within geometry 1 (below)'() {

        given:
        String cqlText = "floors > 5 AND WITHIN(geometry, ENVELOPE(33.8,-118.0,34.0,-117.9))"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_17

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_17, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Number of floors between 4 and 8'() {
        given:
        String cqlText = "floors BETWEEN 4 AND 8"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_18

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_18, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Owner name is either Mike, John or Tom'() {
        given:
        String cqlText = "owner IN ('Mike', 'John', 'Tom')"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_19

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_19, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'owner is NULL'() {
        given:
        String cqlText = "owner IS NULL"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_20

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_20, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'owner is not NULL'() {
        given:
        String cqlText = "owner IS NOT NULL"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_21

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_21, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }


    // EXISTS and DOES-NOT-EXIST are deactivated in the parser

    /*def 'Property "owner" exists'() {
        given:
        String cqlText = "owner EXISTS"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_22

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_22, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Property "owner" does not exist'() {
        given:
        String cqlText = "owner DOES-NOT-EXIST"

        when: 'reading text'
        CqlPredicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_23

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_23, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }*/

}
