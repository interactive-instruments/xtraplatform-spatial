/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import com.google.common.base.Joiner
import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.features.domain.FeatureStoreRelation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Ignore
import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.Stream

import static de.ii.xtraplatform.feature.provider.sql.app.SqlInsertsFixtures.*

/**
 * @author zahnen
 */
@Ignore
class SqlInsertGeneratorSpec extends Specification {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlInsertGeneratorSpec.class);

    def 'createInsert: #casename'() {

        given:

        SqlInsertGenerator inserts = new SqlInsertGenerator();

        when:

        List<FeatureStoreRelation> relation = schema.hasProperty("instanceConnection")?.getProperty(schema) ?: [] as List<FeatureStoreRelation>

        String query = inserts.createInsert(schema, relation, indices, withId)
                              .apply(feature)
                              .first()

        LOGGER.debug("SQL \n{}", query);

        then:

        query == expected

        where:

        casename                    | schema                           | feature                           | withId | indices                   || expected
        "instance with nested main" | MERGE_SCHEMA2 | MERGE_FEATURE | false | ImmutableList.of(0) || MERGE_EXPECTED
        "nested main"               | MAIN_SCHEMA | MAIN_FEATURE | false | ImmutableList.of(0) || MAIN_EXPECTED
        "nested main with id"       | MAIN_SCHEMA | MAIN_FEATURE | true  | ImmutableList.of(0) || MAIN_WITH_ID_EXPECTED
        "m:n 1"                     | NESTED_MAIN_M_2_N_SCHEMA         | MAIN_M_2_N_FEATURE | false | ImmutableList.of(0, 0, 0) || MAIN_M_2_N_EXPECTED[0]
        "m:n 2"                     | NESTED_MAIN_M_2_N_SCHEMA         | MAIN_M_2_N_FEATURE | false | ImmutableList.of(0, 0, 1) || MAIN_M_2_N_EXPECTED[1]
        "merge"                     | MERGE_MERGE_SCHEMA2 | MERGE_MERGE_FEATURE | false | ImmutableList.of(0, 0) || MERGE_MERGE_EXPECTED
        "merge + 1:1"               | MERGE_ONE_2_ONE_SCHEMA           | MERGE_MERGE_ONE_2_ONE_FEATURE | false | ImmutableList.of(0, 0) || MERGE_MERGE_ONE_2_ONE_EXPECTED
        "merge + m:n 1"             | MERGE_M_2_N_SCHEMA               | MERGE_M_2_N_FEATURE               | false  | ImmutableList.of(0, 0, 0) || MERGE_MERGE_M_2_N_EXPECTED[0]
        "merge + m:n 2"             | MERGE_M_2_N_SCHEMA               | MERGE_M_2_N_FEATURE               | false  | ImmutableList.of(0, 0, 1) || MERGE_MERGE_M_2_N_EXPECTED[1]
    }

    def 'createForeignKeyUpdate'() {

        given:

        SqlInsertGenerator inserts = new SqlInsertGenerator();
        List<FeatureStoreRelation> relationPath = MERGE_ONE_2_ONE_SCHEMA.instanceConnection

        when:

        List<String> queries = inserts.createForeignKeyUpdate(relationPath, ImmutableList.of(0, 0))
                                      .stream()
                                      .map({ query -> query.apply(MERGE_MERGE_ONE_2_ONE_FEATURE).first() })
                                      .collect(Collectors.toList());

        LOGGER.debug("SQL \n{}", Joiner.on('\n')
                                       .join(queries));

        then:

        queries.get(0) == MERGE_MERGE_ONE_2_ONE_FOREIGN_KEY_EXPECTED
    }

    def 'createJunctionInsert'() {

        given:

        SqlInsertGenerator inserts = new SqlInsertGenerator();
        List<FeatureStoreRelation> relationPath = MERGE_M_2_N_SCHEMA.instanceConnection

        when:

        List<String> queries = Stream.concat(
                inserts.createJunctionInsert(relationPath, ImmutableList.of(0, 0, 0))
                       .stream(),
                inserts.createJunctionInsert(relationPath, ImmutableList.of(0, 0, 1))
                       .stream()
        )
                                     .map({ query -> query.apply(MERGE_M_2_N_FEATURE).first() })
                                     .collect(Collectors.toList());

        LOGGER.debug("SQL \n{}", Joiner.on('\n')
                                       .join(queries));

        then:

        queries == MERGE_MERGE_M_2_N_JUNCTION_EXPECTED
    }

}