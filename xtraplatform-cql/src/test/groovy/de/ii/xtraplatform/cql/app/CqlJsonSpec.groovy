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
        actual == CqlPredicateExamples.EXAMPLE_1

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_1, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_2

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_2, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_3

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_3, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_4

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_4, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_6

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_6, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_7

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_7, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_8

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_8, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_9

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_9, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_10

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_10, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_11

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_11, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_12

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_12, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_13

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_13, Cql.Format.JSON)

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
        actual == CqlPredicateExamples.EXAMPLE_14

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_14, Cql.Format.JSON)

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
                    "type": "bbox",
                    "coordinates": [ 33.8, -118, 34, -117.9 ]
                }
            }
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlPredicateExamples.EXAMPLE_15

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_15, Cql.Format.JSON)

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
                        "coordinates": [[[-10.0, -10.0],[10.0, -10.0],[10.0, 10.0],[-10.0, -10.0]]]
                    }
                }
            }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlPredicateExamples.EXAMPLE_16

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_16, Cql.Format.JSON)

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
                            "type": "bbox",
                            "coordinates": [ 33.8, -118, 34, -117.9 ]
                        }
                    }
                }
            ]
        }
        """

        when: 'reading json'
        CqlPredicate actual = cql.read(cqlJson, Cql.Format.JSON)

        then:
        actual == CqlPredicateExamples.EXAMPLE_17

        and:

        when: 'writing json'
        String actual2 = cql.write(CqlPredicateExamples.EXAMPLE_17, Cql.Format.JSON)

        then:
        JSONAssert.assertEquals(cqlJson, actual2, true)
    }

}
