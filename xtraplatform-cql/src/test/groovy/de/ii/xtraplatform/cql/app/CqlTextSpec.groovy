/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app


import de.ii.xtraplatform.cql.domain.Cql
import de.ii.xtraplatform.cql.domain.Cql2Predicate
import de.ii.xtraplatform.cql.domain.CqlParseException
import spock.lang.Ignore
import spock.lang.PendingFeature
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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        String cqlText = "T_BEFORE(built, TIMESTAMP('2012-06-05T00:00:00Z'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        String cqlText = "T_AFTER(built, TIMESTAMP('2012-06-05T00:00:00Z'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        String cqlText = "T_DURING(updated, INTERVAL('2017-06-10T07:30:00Z','2017-06-11T10:30:00Z'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_14

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_14, Cql.Format.TEXT)

        then:
        actual2 == cqlText

    }

    @PendingFeature
    def 'Location in the box between -118,33.8 and -117.9,34 in long/lat (geometry 1)'() {

        given:
        String cqlText = "S_WITHIN(location, ENVELOPE(-118.0,33.8,-117.9,34.0))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_15

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_15, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Location that intersects with geometry'() {

        given:
        String cqlText = "S_INTERSECTS(location, POLYGON((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0)))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_16

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_16, Cql.Format.TEXT)

        then:
        actual2 == cqlText

    }

    @PendingFeature
    def 'More than 5 floors and is within geometry 1 (below)'() {

        given:
        String cqlText = "floors > 5 AND S_WITHIN(geometry, ENVELOPE(-118.0,33.8,-117.9,34.0))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_17

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_17, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Number of floors between 4 and 8'() {
        given:
        String cqlText = "floors BETWEEN 4 AND 8"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_18

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_18, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Owner name is either Mike, John or Tom'() {
        given:
        String cqlText = "owner IN ('Mike', 'John', 'Tom')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

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
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_21

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_21, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Built before 2015 (only date, no time information)'() {
        given:
        String cqlText = "T_BEFORE(built, DATE('2015-01-01'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_24

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_24, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Updated between June 10, 2017 and June 11, 2017'() {
        given:
        String cqlText = "T_DURING(updated, INTERVAL('2017-06-10','2017-06-11'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_25b

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_25b, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Updated between 7:30am June 10, 2017 and open end date'() {
        given:
        String cqlText = "T_DURING(updated, INTERVAL('2017-06-10T07:30:00Z','..'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_26

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_26, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Updated between open start date and 10:30am June 11, 2017'() {
        given:
        String cqlText = "T_DURING(updated, INTERVAL('..','2017-06-11T10:30:00Z'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_27

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_27, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Open interval on both ends'() {
        given:
        String cqlText = "T_DURING(updated, INTERVAL('..','..'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_28

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_28, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'Interval with properties on both ends'() {
        given:
        String cqlText = "T_INTERSECTS(event_date, INTERVAL(startDate,endDate))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_TINTERSECTS

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_TINTERSECTS, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Function with no arguments'() {
        given:
        String cqlText = "pos() = 1"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_29

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_29, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Function with multiple arguments'() {
        given:
        String cqlText = "indexOf(names,'Mike') >= 5"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_30

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_30, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Function with a temporal argument'() {
        given:
        String cqlText = "year(TIMESTAMP('2012-06-05T00:00:00Z')) = 2012"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_31

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_31, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Property with a nested filter'() {
        given:
        String cqlText = "filterValues[property = 'd30'].measure > 0.1"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_32

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_32, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Property with two nested filters'() {
        given:
        String cqlText = "filterValues1[property1 = 'd30'].filterValues2[property2 <= 100].measure > 0.1"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_33

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_33, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Find the Landsat scene with identifier "LC82030282019133LGN00"'() {

        given:
        String cqlText = "landsat:scene_id = 'LC82030282019133LGN00'"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_34

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_34, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Evaluate if the value of an array property contains the specified subset of values'() {

        given:
        String cqlText = "A_CONTAINS(layer:ids, ['layers-ca','layers-us'])"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_38

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_38, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Both operands are property references'() {

        given:
        String cqlText = "height < floors"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_37

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_37, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Number of floors NOT between 4 and 8'() {
        given:
        String cqlText = "floors NOT BETWEEN 4 AND 8"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_39

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_39, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Owner name is NOT Mike, John, Tom'() {
        given:
        String cqlText = "owner NOT IN ('Mike', 'John', 'Tom')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_40

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_40, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Nested regular filter'() {
        given:
        String cqlText = "filterValues[property = 'd30'].measure > 0.1"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_32

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_32, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Nested filter with a function'() {
        given:
        String cqlText = "filterValues[position() IN (1, 3)].measure BETWEEN 1 AND 5"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_NESTED_FUNCTION

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_NESTED_FUNCTION, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Array predicate with nested filter'() {
        given:
        String cqlText = "A_CONTAINS(theme[scheme = 'profile'].concept, ['DLKM','Basis-DLM','DLM50'])"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_NESTED_WITH_ARRAYS

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_NESTED_WITH_ARRAYS, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'IN predicate with a function'() {
        given:
        String cqlText = "position() IN (1, 3)"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_IN_WITH_FUNCTION

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_IN_WITH_FUNCTION, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'true'() {
        given:
        String cqlText = "true"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_TRUE

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_TRUE, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'true AND (false OR NOT (false))'() {
        given:
        String cqlText = "true AND (false OR NOT (false))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_BOOLEAN_VALUES

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_BOOLEAN_VALUES, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'keyword as property name'() {
        given:
        String cqlText = "root.\"date\" > DATE('2022-04-17')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_KEYWORD

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_KEYWORD, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'LT with temporal values'() {
        given:
        String cqlText = "built < TIMESTAMP('2012-06-05T00:00:00Z')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_12_alt

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_12_alt, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'LTEQ with temporal values'() {
        given:
        String cqlText = "built <= TIMESTAMP('2012-06-05T00:00:00Z')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_12eq_alt

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_12eq_alt, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'GT with temporal values'() {
        given:
        String cqlText = "built > TIMESTAMP('2012-06-05T00:00:00Z')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_13_alt

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_13_alt, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'GTEQ with temporal values'() {
        given:
        String cqlText = "built >= TIMESTAMP('2012-06-05T00:00:00Z')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_13eq_alt

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_13eq_alt, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'EQ with temporal values'() {
        given:
        String cqlText = "built = TIMESTAMP('2012-06-05T00:00:00Z')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_13A_alt

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_13A_alt, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'NEQ with temporal values'() {
        given:
        String cqlText = "built <> TIMESTAMP('2012-06-05T00:00:00Z')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_13Aneq_alt

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_13Aneq_alt, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    // CQL2: between has been restricted to numeric values
    @Ignore
    def 'BETWEEN with temporal arguments'() {
        given:
        String cqlText = "updated BETWEEN TIMESTAMP('2017-06-10T07:30:00Z') AND TIMESTAMP('2017-06-11T10:30:00Z')"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_14_OLD

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_14_OLD, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'IN with temporal arguments'() {
        given:
        String cqlText = "updated IN (TIMESTAMP('2017-06-10T07:30:00Z'), TIMESTAMP('2018-06-10T07:30:00Z'), TIMESTAMP('2019-06-10T07:30:00Z'), TIMESTAMP('2020-06-10T07:30:00Z'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_IN_WITH_TEMPORAL

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_IN_WITH_TEMPORAL, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    def 'LT with temporal value -- interval'() {
        given:
        String cqlText = "built < INTERVAL('2017-06-10T07:30:00Z','2017-06-11T10:30:00Z')"

        when: 'reading text'
        cql.read(cqlText, Cql.Format.TEXT)

        then:
        thrown(CqlParseException)
    }

    def 'BETWEEN with temporal arguments -- intervals'() {
        given:
        String cqlText = "updated BETWEEN INTERVAL('2017-06-10T07:30:00Z','2017-06-11T10:30:00Z') AND INTERVAL('2018-06-10T07:30:00Z','2018-06-11T10:30:00Z')"

        when: 'reading text'
        cql.read(cqlText, Cql.Format.TEXT)

        then:
        thrown(CqlParseException)
    }

    def 'IN with temporal arguments -- intervals'() {
        given:
        String cqlText = "updated IN (INTERVAL('2017-06-10T07:30:00Z','2017-06-11T10:30:00Z'), INTERVAL('2018-06-10T07:30:00Z','2018-06-11T10:30:00Z'))"

        when: 'reading text'
        cql.read(cqlText, Cql.Format.TEXT)

        then:
        thrown(CqlParseException)
    }

    @PendingFeature
    def 'Case insensitive string comparison function CASEI'() {
        given:
        String cqlText = "CASEI(road_class) IN (CASEI('Οδος'), CASEI('Straße'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_CASEI

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_CASEI, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

    @PendingFeature
    def 'Accent insensitive string comparison function ACCENTI'() {
        given:
        String cqlText = "ACCENTI(road_class) IN (ACCENTI('Οδος'), ACCENTI('Straße'))"

        when: 'reading text'
        Cql2Predicate actual = cql.read(cqlText, Cql.Format.TEXT)

        then:
        actual == CqlFilterExamples.EXAMPLE_ACCENTI

        and:

        when: 'writing text'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_ACCENTI, Cql.Format.TEXT)

        then:
        actual2 == cqlText
    }

}
