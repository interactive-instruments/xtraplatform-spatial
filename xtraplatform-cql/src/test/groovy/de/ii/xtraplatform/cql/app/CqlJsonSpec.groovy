/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app

import de.ii.xtraplatform.cql.domain.Cql
import de.ii.xtraplatform.cql.domain.CqlPredicate
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Shared
import spock.lang.Specification

class CqlJsonSpec extends Specification {

    @Shared
    Cql cql

    def setupSpec() {
        cql = new CqlImpl()
    }

    def 'Floors greater than 5'() {

        given:
        String cqlJson = """
            {
                "gt": {
                    "property": "floors",
                    "value": 5
                }
            } 
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_1

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_1, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)

    }

    def 'Taxes less than or equal to 500'() {

        given:
        String cqlJson = """
            {
                 "lte": {
                     "property": "taxes",
                     "value": 500
                 }
            }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_2

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_2, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Owner name contains "Jones"'() {

        given:
        String cqlJson = """
        {
            "like": {
                "property": "owner",
                "value": "% Jones %"
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_3

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_3, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }


    def 'Owner name starts with "Mike"'() {

        given:
        String cqlJson = """
        {
            "like": {
                "wildCards": "%",
                "property": "owner",
                "value": "Mike%"
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_4

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_4, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Owner name does not contain "Mike"'() {

        given:
        String cqlJson = """
        {
            "not": [
                {
                    "like": {
                        "property": "owner",
                        "value": "% Mike %"
                    }
                }
            ]
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_5

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_5, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'A swimming pool'() {

        given:
        String cqlJson = """
        {
            "eq": {
                "property": "swimming_pool",
                "value": true
            }
          }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_6

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_6, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'More than 5 floors and a swimming pool'() {

        given:
        String cqlJson = """
            {
                "and": [
                    {
                        "gt": {
                            "property": "floors",
                            "value": 5
                        }
                    },
                    {
                        "eq": {
                            "property": "swimming_pool",
                            "value": true
                        }
                    }
                ]
            }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_7

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_7, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)

    }

    def 'A swimming pool and (more than five floors or material is brick)'() {

        given:
        String cqlJson = """
        {
            "and": [
                {
                    "eq": {
                        "property": "swimming_pool",
                        "value": true
                    }
                },
                {
                    "or": [
                        {
                            "gt": {
                                "property": "floors",
                                "value": 5
                            }
                        },
                        {
                            "like": {
                                "property": "material",
                                "value": "brick%"
                            }
                        },
                        {
                            "like": {
                                "property": "material",
                                "value": "%brick"
                            }
                        }
                    ]
                }
            ]
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_8

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_8, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def '[More than five floors and material is brick] or swimming pool is true'() {

        given:
        String cqlJson = """
        {
            "or": [
                {
                    "and": [
                        {
                            "gt": {
                                "property": "floors",
                                "value": 5
                            }
                        },
                        {
                            "eq": {
                                "property": "material",
                                "value": "brick"
                            }
                        }
                    ]
                },
                {
                    "eq": {
                        "property": "swimming_pool",
                        "value": true
                    }
                }
            ]
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_9

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_9, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Not under 5 floors or a swimming pool'() {

        given:
        String cqlJson = """
        {
            "or": [
                {
                    "not": [
                        {
                            "lt": {
                                "property": "floors",
                                "value": 5
                            }
                        }
                    ]
                },
                {
                    "eq": {
                        "property": "swimming_pool",
                        "value": true
                    }
                }
            ]
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_10

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_10, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Owner name starts with "mike" or "Mike" and is less than 4 floors'() {

        given:
        String cqlJson = """
        {
            "and": [
                {
                    "or": [
                        {
                            "like": {
                                "property": "owner",
                                "value": "mike%"
                            }
                        },
                        {
                            "like": {
                                "property": "owner",
                                "value": "Mike%"
                            }
                        }
                    ]
                },
                {
                    "lt": {
                        "property": "floors",
                        "value": 4
                    }
                }
            ]
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_11

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_11, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Built before 2015'() {

        given:
        String cqlJson = """
        {
            "before": {
                "property": "built",
                "value": "2015-01-01T00:00:00Z"
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_12

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_12, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }


    def 'Built after June 5, 2012'() {

        given:
        String cqlJson = """
        {
            "after": {
                "property": "built",
                "value": "2012-06-05T00:00:00Z"
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_13

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_13, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }


    def 'Updated between 7:30am June 10, 2017 and 10:30am June 11, 2017'() {

        given:
        String cqlJson = """
            {
                "during": {
                    "property": "updated",
                    "value": ["2017-06-10T07:30:00Z","2017-06-11T10:30:00Z"]
                }
            }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_14

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_14, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)

    }

    def 'Location in the box between -118,33.8 and -117.9,34 in lat/long (geometry 1)'() {

        given:
        String cqlJson = """
        {
             "within": {
                "property": "location",
                "value": { 
                    "bbox": [33.8,-118,34,-117.9],
                    "crs": {
                        "code": 4326,
                        "forceAxisOrder": "LON_LAT"
                    }
                }
              }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_15

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_15, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Location that intersects with geometry'() {

        given:
        String cqlJson = """
            {
                "intersects": {
                    "property": "location",
                    "value": {
                        "type": "Polygon",
                        "coordinates": [[[-10.0, -10.0],[10.0, -10.0],[10.0, 10.0],[-10.0, -10.0]]],
                        "crs": {
                            "code": 4326,
                            "forceAxisOrder": "LON_LAT"
                        }
                    }
                }
            }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_16

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_16, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)

    }

    def 'More than 5 floors and is within geometry 1 (below)'() {

        given:
        String cqlJson = """
        {
            "and": [
                {
                    "gt": {
                        "property": "floors",
                        "value": 5
                    }
                },
                {
                   "within": {
                      "property": "geometry",
                      "value": { 
                        "bbox": [33.8,-118,34,-117.9],
                        "crs": {
                            "code": 4326,
                            "forceAxisOrder": "LON_LAT"
                        }
                      }
                   }
                }
            ]
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_17

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_17, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Number of floors between 4 and 8'() {

        given:
        String cqlJson = """
        {
            "between": {
                "property": "floors",
                "lower": 4,
                "upper": 8
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_18

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_18, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Owner name is either Mike, John or Tom'() {

        given:
        String cqlJson = """
        {
            "in": {
                "property": "owner",
                "values": [ "Mike", "John", "Tom" ],
                "nocase" : true
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_19

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_19, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'owner is NULL'() {

        given:
        String cqlJson = """
        {
            "isNull": {
                "property": "owner"
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_20

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_20, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'owner is not NULL'() {

        given:
        String cqlJson = """
        {
            "not": [
                {
                    "isNull": {
                        "property": "owner"
                    }
                }
            ]
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_21

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_21, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    //EXISTS and DOES-NOT-EXIST are deactivated in the parser
    /*def 'Property "owner" exists'() {

        given:
        String cqlJson = """
        {
            "exists": {
                "property": "owner"
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_22

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_22, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Property "owner" does not exist'() {

        given:
        String cqlJson = """
        {
            "not": [
                {
                    "exists": {
                        "property": "owner"
                    }
                }
            ]
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_23

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_23, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }*/

    def 'Built before 2015 (only date, no time information)'() {

        given:
        String cqlJson = """
        {
            "before": {
                "property": "built",
                "value": "2015-01-01"
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_24

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_24, Cql.Format.JSON)

        then:
        String cqlJson2 = """
        {
            "before": {
                "property": "built",
                "value": "2015-01-01T00:00:00Z"
            }
        }
        """
        JSONAssert.assertEquals(cqlJson2, actual2, true)
    }

    def 'Updated between June 10, 2017 and June 11, 2017'() {

        given:
        String cqlJson = """
            {
                "during": {
                    "property": "updated",
                    "value": ["2017-06-10","2017-06-11"]
                }
            }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_25

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_25, Cql.Format.JSON)

        then:
        String cqlJson2 = """
            {
                "during": {
                    "property": "updated",
                    "value": ["2017-06-10T00:00:00Z","2017-06-11T00:00:00Z"]
                }
            }
        """
        JSONAssert.assertEquals(cqlJson2, actual2, true)

    }

    def 'Updated between 7:30am June 10, 2017 and an open end date'() {
        given:
        String cqlJson = """
            {
                "during": {
                    "property": "updated",
                    "value": ["2017-06-10T07:30:00Z","9999-12-31T23:59:59Z"]
                }
            }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_26

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_26, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Updated between an open start date and 10:30am June 11, 2017'() {
        given:
        String cqlJson = """
            {
                "during": {
                    "property": "updated",
                    "value": ["0000-01-01T00:00:00Z","2017-06-11T10:30:00Z"]
                }
            }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_27

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_27, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Open interval on both ends'() {
        given:
        String cqlJson = """
            {
                "during": {
                    "property": "updated",
                    "value": ["0000-01-01T00:00:00Z","9999-12-31T23:59:59Z"]
                }
            }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_28

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_28, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Function with no arguments'() {
        given:
        String cqlJson = """
            {
                "eq": {
                    "function": {
                        "name": "pos",
                        "arguments": []
                    },
                    "value": 1
                }
            } 
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_29

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_29, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Function with multiple arguments'() {
        given:
        String cqlJson = """
            {
                "gte": {
                    "function": {
                        "name": "indexOf",
                        "arguments": ["names", "'Mike'"]
                    },
                    "value": 5
                }
            } 
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_30

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_30, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

    def 'Function with a temporal argument'() {
        given:
        String cqlJson = """
            {
                "eq": {
                    "function": {
                        "name": "year",
                        "arguments": ["2012-06-05T00:00:00Z"]
                    },
                    "value": 2012
                }
            } 
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlFilterExamples.EXAMPLE_31

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlFilterExamples.EXAMPLE_31, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

}
