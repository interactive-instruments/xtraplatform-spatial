package de.ii.xtraplatform.feature.provider.sql.app

import de.ii.xtraplatform.cql.app.CqlFilterExamples
import de.ii.xtraplatform.crs.domain.OgcCrs
import de.ii.xtraplatform.crs.infra.GeoToolsCrsTransformerFactory
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis
import spock.lang.Shared
import spock.lang.Specification

import java.util.stream.Collectors

class FeatureStoreQueryGeneratorSqlSpec extends Specification {


    @Shared
    FeatureStoreQueryGeneratorSql queryGeneratorSql


    def setupSpec() {
        queryGeneratorSql = new FeatureStoreQueryGeneratorSql(new SqlDialectPostGis(), OgcCrs.CRS84, new GeoToolsCrsTransformerFactory())
    }


    def 'Floors greater than 5'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.FLOORS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_1), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.FLOORS, Optional.of(CqlFilterExamples.EXAMPLE_1), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5)) ORDER BY 1"
    }

    def 'Taxes less than or equal to 500'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.TAXES, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_2), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.TAXES, Optional.of(CqlFilterExamples.EXAMPLE_2), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.taxes <= 500) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.taxes <= 500) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.taxes FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.taxes <= 500)) ORDER BY 1"
    }

    def 'Owner name contains "Jones"'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.OWNER, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_3), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.OWNER, Optional.of(CqlFilterExamples.EXAMPLE_3), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE '% jones %') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE '% jones %') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.owner FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE '% jones %')) ORDER BY 1"
    }

    def 'Owner name starts with "Mike"'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.OWNER, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_4), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.OWNER, Optional.of(CqlFilterExamples.EXAMPLE_4), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE 'mike%') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE 'mike%') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.owner FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE 'mike%')) ORDER BY 1"
    }

    def 'Owner name does not contain "Mike"'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.OWNER, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_5), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.OWNER, Optional.of(CqlFilterExamples.EXAMPLE_5), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) NOT LIKE '% mike %') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) NOT LIKE '% mike %') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.owner FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) NOT LIKE '% mike %')) ORDER BY 1"

    }

    def 'A swimming pool'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.SWIMMING_POOL, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_6), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.SWIMMING_POOL, Optional.of(CqlFilterExamples.EXAMPLE_6), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.swimming_pool FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true)) ORDER BY 1"
    }

    def 'More than 5 floors and a swimming pool'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.SCALAR_OPERATIONS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_7), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.SCALAR_OPERATIONS, Optional.of(CqlFilterExamples.EXAMPLE_7), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors, A.owner, A.swimming_pool, A.material, A.geometry, A.height FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true)) ORDER BY 1"
    }

    def 'A swimming pool and (more than five floors or material is brick)'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.SCALAR_OPERATIONS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_8), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.SCALAR_OPERATIONS, Optional.of(CqlFilterExamples.EXAMPLE_8), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) OR A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.material::varchar) LIKE 'brick%') OR A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.material::varchar) LIKE '%brick')) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) OR A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.material::varchar) LIKE 'brick%') OR A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.material::varchar) LIKE '%brick')) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors, A.owner, A.swimming_pool, A.material, A.geometry, A.height FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) OR A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.material::varchar) LIKE 'brick%') OR A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.material::varchar) LIKE '%brick'))) ORDER BY 1"
    }

    def '[More than five floors and material is brick] or swimming pool is true'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.SCALAR_OPERATIONS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_9), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.SCALAR_OPERATIONS, Optional.of(CqlFilterExamples.EXAMPLE_9), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM container AA  WHERE AA.material = 'brick')) OR A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM container AA  WHERE AA.material = 'brick')) OR A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors, A.owner, A.swimming_pool, A.material, A.geometry, A.height FROM container A WHERE (A.id >= null AND A.id <= null) AND ((A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM container AA  WHERE AA.material = 'brick')) OR A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true)) ORDER BY 1"
    }

    def 'Not under 5 floors or a swimming pool'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.SCALAR_OPERATIONS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_10), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.SCALAR_OPERATIONS, Optional.of(CqlFilterExamples.EXAMPLE_10), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE NOT (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors < 5)) OR A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE NOT (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors < 5)) OR A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors, A.owner, A.swimming_pool, A.material, A.geometry, A.height FROM container A WHERE (A.id >= null AND A.id <= null) AND (NOT (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors < 5)) OR A.id IN (SELECT AA.id FROM container AA  WHERE AA.swimming_pool = true)) ORDER BY 1"
    }

    def 'Owner name starts with "mike" or "Mike" and is less than 4 floors'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.SCALAR_OPERATIONS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_11), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.SCALAR_OPERATIONS, Optional.of(CqlFilterExamples.EXAMPLE_11), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE (A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE 'mike%') OR A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE 'mike%')) AND A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors < 4) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE (A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE 'mike%') OR A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE 'mike%')) AND A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors < 4) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors, A.owner, A.swimming_pool, A.material, A.geometry, A.height FROM container A WHERE (A.id >= null AND A.id <= null) AND ((A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE 'mike%') OR A.id IN (SELECT AA.id FROM container AA  WHERE LOWER(AA.owner::varchar) LIKE 'mike%')) AND A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors < 4)) ORDER BY 1"
    }

    def 'Built before 2015'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.BUILT, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_12), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.BUILT, Optional.of(CqlFilterExamples.EXAMPLE_12), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.built < TIMESTAMP '2015-01-01T00:00:00Z') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.built < TIMESTAMP '2015-01-01T00:00:00Z') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.built FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.built < TIMESTAMP '2015-01-01T00:00:00Z')) ORDER BY 1"
    }

    def 'Built after June 5, 2012'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.BUILT, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_13), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.BUILT, Optional.of(CqlFilterExamples.EXAMPLE_13), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.built > TIMESTAMP '2012-06-05T00:00:00Z') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.built > TIMESTAMP '2012-06-05T00:00:00Z') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.built FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.built > TIMESTAMP '2012-06-05T00:00:00Z')) ORDER BY 1"
    }

    def 'Updated between 7:30am June 10, 2017 and 10:30am June 11, 2017'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.UPDATED, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_14), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.UPDATED, Optional.of(CqlFilterExamples.EXAMPLE_14), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '2017-06-10T07:30:00Z' AND TIMESTAMP '2017-06-11T10:30:00Z') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '2017-06-10T07:30:00Z' AND TIMESTAMP '2017-06-11T10:30:00Z') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.updated FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '2017-06-10T07:30:00Z' AND TIMESTAMP '2017-06-11T10:30:00Z')) ORDER BY 1"
    }

    def 'Location in the box between -118,33.8 and -117.9,34 in long/lat (geometry 1)'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.LOCATION, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_15), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.LOCATION, Optional.of(CqlFilterExamples.EXAMPLE_15), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Within(AA.location, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Within(AA.location, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.location FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE ST_Within(AA.location, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326)))) ORDER BY 1"
    }

    def 'Location that intersects with geometry'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.LOCATION, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_16), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.LOCATION, Optional.of(CqlFilterExamples.EXAMPLE_16), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Intersects(AA.location, ST_GeomFromText('POLYGON((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0))',4326))) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Intersects(AA.location, ST_GeomFromText('POLYGON((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0))',4326))) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.location FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE ST_Intersects(AA.location, ST_GeomFromText('POLYGON((-10.0 -10.0,10.0 -10.0,10.0 10.0,-10.0 -10.0))',4326)))) ORDER BY 1"
    }

    def 'More than 5 floors and is within geometry 1 (below)'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.SCALAR_OPERATIONS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_17), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.SCALAR_OPERATIONS, Optional.of(CqlFilterExamples.EXAMPLE_17), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM container AA  WHERE ST_Within(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM container AA  WHERE ST_Within(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors, A.owner, A.swimming_pool, A.material, A.geometry, A.height FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors > 5) AND A.id IN (SELECT AA.id FROM container AA  WHERE ST_Within(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326)))) ORDER BY 1"
    }

    def 'Number of floors between 4 and 8'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.FLOORS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_18), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.FLOORS, Optional.of(CqlFilterExamples.EXAMPLE_18), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors BETWEEN 4 AND 8) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors BETWEEN 4 AND 8) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors BETWEEN 4 AND 8)) ORDER BY 1"
    }

    def 'Owner name is either Mike, John or Tom'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.OWNER, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_19), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.OWNER, Optional.of(CqlFilterExamples.EXAMPLE_19), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner IN ('Mike', 'John', 'Tom')) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner IN ('Mike', 'John', 'Tom')) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.owner FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner IN ('Mike', 'John', 'Tom')) ORDER BY 1"
    }

    def 'owner is NULL'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.OWNER, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_20), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.OWNER, Optional.of(CqlFilterExamples.EXAMPLE_20), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner IS NULL) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner IS NULL) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.owner FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner IS NULL)) ORDER BY 1"
    }

    def 'owner is not NULL'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.OWNER, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_21), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.OWNER, Optional.of(CqlFilterExamples.EXAMPLE_21), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner IS NOT NULL) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner IS NOT NULL) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.owner FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner IS NOT NULL)) ORDER BY 1"
    }

    def 'Built before 2015 (only date, no time information)'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.BUILT, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_24), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.BUILT, Optional.of(CqlFilterExamples.EXAMPLE_24), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.built < TIMESTAMP '2015-01-01T00:00:00Z') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.built < TIMESTAMP '2015-01-01T00:00:00Z') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.built FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.built < TIMESTAMP '2015-01-01T00:00:00Z')) ORDER BY 1"
    }

    def 'Updated between June 10, 2017 and June 11, 2017'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.UPDATED, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_25), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.UPDATED, Optional.of(CqlFilterExamples.EXAMPLE_25), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '2017-06-10T00:00:00Z' AND TIMESTAMP '2017-06-11T23:59:59Z') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '2017-06-10T00:00:00Z' AND TIMESTAMP '2017-06-11T23:59:59Z') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.updated FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '2017-06-10T00:00:00Z' AND TIMESTAMP '2017-06-11T23:59:59Z')) ORDER BY 1"
    }

    def 'Updated between 7:30am June 10, 2017 and open end date'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.UPDATED, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_26), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.UPDATED, Optional.of(CqlFilterExamples.EXAMPLE_26), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '2017-06-10T07:30:00Z' AND TIMESTAMP 'infinity') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '2017-06-10T07:30:00Z' AND TIMESTAMP 'infinity') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.updated FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '2017-06-10T07:30:00Z' AND TIMESTAMP 'infinity')) ORDER BY 1"
    }

    def 'Updated between open start date and 10:30am June 11, 2017'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.UPDATED, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_27), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.UPDATED, Optional.of(CqlFilterExamples.EXAMPLE_27), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '-infinity' AND TIMESTAMP '2017-06-11T10:30:00Z') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '-infinity' AND TIMESTAMP '2017-06-11T10:30:00Z') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.updated FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '-infinity' AND TIMESTAMP '2017-06-11T10:30:00Z')) ORDER BY 1"
    }

    def 'Open interval on both ends'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.UPDATED, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_28), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.UPDATED, Optional.of(CqlFilterExamples.EXAMPLE_28), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '-infinity' AND TIMESTAMP 'infinity') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '-infinity' AND TIMESTAMP 'infinity') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.updated FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.updated BETWEEN TIMESTAMP '-infinity' AND TIMESTAMP 'infinity')) ORDER BY 1"
    }

    def 'Function with no arguments'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.LOCATION, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_29), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.LOCATION, Optional.of(CqlFilterExamples.EXAMPLE_29), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE pos() ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE pos() ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.location FROM container A WHERE (A.id >= null AND A.id <= null) AND (pos()) ORDER BY 1"
    }

    def 'Function with multiple arguments'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.NAMES, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_30), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.NAMES, Optional.of(CqlFilterExamples.EXAMPLE_30), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE indexOf(A.id IN (SELECT AA.id FROM container AA  WHERE AA.names >= 5),'Mike') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE indexOf(A.id IN (SELECT AA.id FROM container AA  WHERE AA.names >= 5),'Mike') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.names FROM container A WHERE (A.id >= null AND A.id <= null) AND (indexOf(A.id IN (SELECT AA.id FROM container AA  WHERE AA.names >= 5),'Mike')) ORDER BY 1"
    }

    def 'Function with a temporal argument'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.BUILT, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_31), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.BUILT, Optional.of(CqlFilterExamples.EXAMPLE_31), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE year('2012-06-05T00:00:00Z') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE year('2012-06-05T00:00:00Z') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.built FROM container A WHERE (A.id >= null AND A.id <= null) AND (year('2012-06-05T00:00:00Z')) ORDER BY 1"
    }

    def 'Property with a nested filter'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.MEASURE, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_32), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.MEASURE, Optional.of(CqlFilterExamples.EXAMPLE_32), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.filterValues.measure > 0.1) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.filterValues.measure > 0.1) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.filterValues.measure FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.filterValues.measure > 0.1)) ORDER BY 1"
    }

    def 'Property with two nested filters'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.MEASURE_2, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_33), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.MEASURE_2, Optional.of(CqlFilterExamples.EXAMPLE_33), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.filterValues1.filterValues2.measure > 0.1) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.filterValues1.filterValues2.measure > 0.1) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.filterValues1.filterValues2.measure FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.filterValues1.filterValues2.measure > 0.1)) ORDER BY 1"
    }

    def 'Find the Landsat scene with identifier "LC82030282019133LGN00"'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.SCENE_ID, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_34), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.SCENE_ID, Optional.of(CqlFilterExamples.EXAMPLE_34), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.landsat:scene_id = 'LC82030282019133LGN00') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.landsat:scene_id = 'LC82030282019133LGN00') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.landsat:scene_id FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.landsat:scene_id = 'LC82030282019133LGN00')) ORDER BY 1"
    }

    def 'LIKE operator modifiers'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.NAME, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_35), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.NAME, Optional.of(CqlFilterExamples.EXAMPLE_35), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.name::varchar LIKE 'Smith_') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.name::varchar LIKE 'Smith_') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.name FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.name::varchar LIKE 'Smith_')) ORDER BY 1"
    }

    def 'ANYINTERACTS temporal operator'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.EVENT_DATE, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_36), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.EVENT_DATE, Optional.of(CqlFilterExamples.EXAMPLE_36), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE (AA.event_date,AA.event_date) OVERLAPS (TIMESTAMP '1969-07-16T05:32:00Z', TIMESTAMP '1969-07-24T16:50:36Z')) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE (AA.event_date,AA.event_date) OVERLAPS (TIMESTAMP '1969-07-16T05:32:00Z', TIMESTAMP '1969-07-24T16:50:36Z')) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.event_date FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE (AA.event_date,AA.event_date) OVERLAPS (TIMESTAMP '1969-07-16T05:32:00Z', TIMESTAMP '1969-07-24T16:50:36Z'))) ORDER BY 1"
    }

    def 'TEquals temporal operator'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.BUILT, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_TEQUALS), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.BUILT, Optional.of(CqlFilterExamples.EXAMPLE_TEQUALS), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.built = TIMESTAMP '2012-06-05T00:00:00Z') ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.built = TIMESTAMP '2012-06-05T00:00:00Z') ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.built FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.built = TIMESTAMP '2012-06-05T00:00:00Z')) ORDER BY 1"
    }

    def 'Both operands are property references'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.SCALAR_OPERATIONS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_37), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.SCALAR_OPERATIONS, Optional.of(CqlFilterExamples.EXAMPLE_37), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.height < AA.floors) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.height < AA.floors) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors, A.owner, A.swimming_pool, A.material, A.geometry, A.height FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.height < AA.floors)) ORDER BY 1"
    }

    def 'Number of floors NOT between 4 and 8'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.FLOORS, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_39), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.FLOORS, Optional.of(CqlFilterExamples.EXAMPLE_39), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors NOT BETWEEN 4 AND 8) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors NOT BETWEEN 4 AND 8) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.floors FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.floors NOT BETWEEN 4 AND 8)) ORDER BY 1"
    }

    def 'Owner name is NOT Mike, John, Tom'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.OWNER, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_40), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.OWNER, Optional.of(CqlFilterExamples.EXAMPLE_40), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner NOT IN ('Mike', 'John', 'Tom')) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner NOT IN ('Mike', 'John', 'Tom')) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.owner FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE AA.owner NOT IN ('Mike', 'John', 'Tom'))) ORDER BY 1"
    }

    def 'DISJOINT spatial operator'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.GEOMETRY, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_DISJOINT), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.GEOMETRY, Optional.of(CqlFilterExamples.EXAMPLE_DISJOINT), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Disjoint(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Disjoint(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.geometry FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE ST_Disjoint(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326)))) ORDER BY 1"
    }

    def 'EQUALS spatial operator'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.GEOMETRY, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_EQUALS), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.GEOMETRY, Optional.of(CqlFilterExamples.EXAMPLE_EQUALS), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Equals(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Equals(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.geometry FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE ST_Equals(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326)))) ORDER BY 1"
    }

    def 'TOUCHES spatial operator'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.GEOMETRY, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_TOUCHES), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.GEOMETRY, Optional.of(CqlFilterExamples.EXAMPLE_TOUCHES), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Touches(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Touches(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.geometry FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE ST_Touches(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326)))) ORDER BY 1"
    }

    def 'OVERLAPS spatial operator'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.GEOMETRY, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_OVERLAPS), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.GEOMETRY, Optional.of(CqlFilterExamples.EXAMPLE_OVERLAPS), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Overlaps(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Overlaps(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.geometry FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE ST_Overlaps(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326)))) ORDER BY 1"
    }

    def 'CROSSES spatial operator'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.GEOMETRY, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_CROSSES), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.GEOMETRY, Optional.of(CqlFilterExamples.EXAMPLE_CROSSES), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Crosses(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Crosses(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.geometry FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE ST_Crosses(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326)))) ORDER BY 1"
    }

    def 'CONTAINS spatial operator'() {
        when:
        String metaQuery = queryGeneratorSql.getMetaQuery(FeatureStoreFixtures.GEOMETRY, 10, 0, Optional.of(CqlFilterExamples.EXAMPLE_CONTAINS), Collections.emptyList(), true)
        String instanceQuery = queryGeneratorSql.getInstanceQueries(FeatureStoreFixtures.GEOMETRY, Optional.of(CqlFilterExamples.EXAMPLE_CONTAINS), Collections.emptyList(), null, null, Collections.emptyList(), Collections.emptyList()).collect(Collectors.toList()).get(0)
        then:
        metaQuery == "SELECT * FROM (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Contains(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY SKEY LIMIT 10) AS NR) AS NR2, (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM container A WHERE A.id IN (SELECT AA.id FROM container AA  WHERE ST_Contains(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326))) ORDER BY 1) AS NM) AS NM2"
        instanceQuery == "SELECT A.id AS SKEY, A.geometry FROM container A WHERE (A.id >= null AND A.id <= null) AND (A.id IN (SELECT AA.id FROM container AA  WHERE ST_Contains(AA.geometry, ST_GeomFromText('POLYGON((-118.0 33.8,-117.9 33.8,-117.9 34.0,-118.0 34.0,-118.0 33.8))',4326)))) ORDER BY 1"
    }

}
