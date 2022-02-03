/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import de.ii.xtraplatform.cql.app.CqlImpl
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlPathDefaults
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql
import de.ii.xtraplatform.feature.provider.sql.domain.SqlPathParser
import spock.lang.Shared
import spock.lang.Specification

class QuerySchemaDeriverSpec extends Specification {

    @Shared
    QuerySchemaDeriver schemaDeriver

    def setupSpec() {
        def defaults = new ImmutableSqlPathDefaults.Builder().build()
        def cql = new CqlImpl()
        def pathParser = new SqlPathParser(defaults, cql)
        schemaDeriver = new QuerySchemaDeriver(pathParser)
    }

    def 'query schema: #casename'() {

        when:

        List<SchemaSql> actual = source.accept(schemaDeriver)

        then:

        actual == expected

        where:

        casename                               | source                                                    || expected
        "value array"                          | FeatureSchemaFixtures.VALUE_ARRAY                         || QuerySchemaFixtures.VALUE_ARRAY
        "object array"                         | FeatureSchemaFixtures.OBJECT_ARRAY                        || QuerySchemaFixtures.OBJECT_ARRAY
        "merge"                                | FeatureSchemaFixtures.MERGE                               || QuerySchemaFixtures.MERGE
        "self joins"                           | FeatureSchemaFixtures.SELF_JOINS                          || QuerySchemaFixtures.SELF_JOINS
        //"self joins with filters"              | FeatureSchemaFixtures.SELF_JOINS_FILTER                   || QuerySchemaFixtures.SELF_JOINS_FILTER
        "self join with nested duplicate join" | FeatureSchemaFixtures.SELF_JOIN_NESTED_DUPLICATE          || QuerySchemaFixtures.SELF_JOIN_NESTED_DUPLICATE
        "object without sourcePath"            | FeatureSchemaFixtures.OBJECT_WITHOUT_SOURCE_PATH          || QuerySchemaFixtures.OBJECT_WITHOUT_SOURCE_PATH
        "multiple sourcePaths"                 | FeatureSchemaFixtures.PROPERTY_WITH_MULTIPLE_SOURCE_PATHS || QuerySchemaFixtures.PROPERTY_WITH_MULTIPLE_SOURCE_PATHS
        "nested joins"                         | FeatureSchemaFixtures.NESTED_JOINS                        || QuerySchemaFixtures.NESTED_JOINS
        "nested value array"                   | FeatureSchemaFixtures.NESTED_VALUE_ARRAY                  || QuerySchemaFixtures.NESTED_VALUE_ARRAY
    }


}
