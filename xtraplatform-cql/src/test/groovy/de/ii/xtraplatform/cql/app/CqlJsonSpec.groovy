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
}
