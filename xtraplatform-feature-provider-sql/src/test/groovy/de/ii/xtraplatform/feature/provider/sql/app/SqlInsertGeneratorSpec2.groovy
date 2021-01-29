/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import com.google.common.base.Joiner
import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.feature.provider.sql.ImmutableOptions
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.Stream

import static de.ii.xtraplatform.feature.provider.sql.app.SqlInsertsFixtures.*

/**
 * @author zahnen
 */
class SqlInsertGeneratorSpec2 extends Specification {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlInsertGeneratorSpec2.class);
    private static final SqlPathSyntax.Options sqlPathDefaults = new ImmutableOptions.Builder().build();

    def 'createInsert: #casename'() {

        given:

        SqlInsertGenerator2 inserts = new SqlInsertGenerator2(null, null, sqlPathDefaults);

        when:

        String query = inserts.createInsert(schema, indices, withId)
                .apply(feature)
                .first()

        LOGGER.debug("SQL \n{}", query);

        then:

        query == expected

        where:

        casename                | schema                       | feature                       | withId | indices                      || expected
        "main"                  | MAIN_SCHEMA                  | MAIN_FEATURE                  | false  | ImmutableList.<Integer> of() || MAIN_EXPECTED
        "main with id"          | MAIN_SCHEMA                  | MAIN_FEATURE                  | true   | ImmutableList.<Integer> of() || MAIN_WITH_ID_EXPECTED
        "merge"                 | MERGE_SCHEMA                 | MERGE_FEATURE                 | false  | ImmutableList.of(0)          || MERGE_EXPECTED
        "m:n 1"                 | MAIN_M_2_N_SCHEMA            | MAIN_M_2_N_FEATURE            | false  | ImmutableList.of(0)          || MAIN_M_2_N_EXPECTED[0]
        "m:n 2"                 | MAIN_M_2_N_SCHEMA            | MAIN_M_2_N_FEATURE            | false  | ImmutableList.of(1)          || MAIN_M_2_N_EXPECTED[1]
        "merge + merge"         | MERGE_MERGE_SCHEMA           | MERGE_MERGE_FEATURE           | false  | ImmutableList.of(0, 0)       || MERGE_MERGE_EXPECTED
        "merge + merge + 1:1"   | MERGE_MERGE_ONE_2_ONE_SCHEMA | MERGE_MERGE_ONE_2_ONE_FEATURE | false  | ImmutableList.of(0, 0)       || MERGE_MERGE_ONE_2_ONE_EXPECTED
        "merge + merge + m:n 1" | MERGE_MERGE_M_2_N_SCHEMA     | MERGE_MERGE_M_2_N_FEATURE     | false  | ImmutableList.of(0, 0, 0)    || MERGE_MERGE_M_2_N_EXPECTED[0]
        "merge + merge + m:n 2" | MERGE_MERGE_M_2_N_SCHEMA     | MERGE_MERGE_M_2_N_FEATURE     | false  | ImmutableList.of(0, 0, 1)    || MERGE_MERGE_M_2_N_EXPECTED[1]
    }

    def 'createForeignKeyUpdate'() {

        given:

        SqlInsertGenerator2 inserts = new SqlInsertGenerator2(null, null, sqlPathDefaults);
        SchemaSql schema = MERGE_MERGE_ONE_2_ONE_SCHEMA

        when:

        String query = inserts.createForeignKeyUpdate(schema, ImmutableList.of(0, 0)).apply(MERGE_MERGE_ONE_2_ONE_FEATURE).first()

        LOGGER.debug("SQL \n{}", query);

        then:

        query == MERGE_MERGE_ONE_2_ONE_FOREIGN_KEY_EXPECTED
    }

    def 'createJunctionInsert'() {

        given:

        SqlInsertGenerator2 inserts = new SqlInsertGenerator2(null, null, sqlPathDefaults);
        SchemaSql schema = MERGE_MERGE_M_2_N_SCHEMA

        when:

        List<String> queries = Stream.of(
                inserts.createJunctionInsert(schema, ImmutableList.of(0, 0, 0)),
                inserts.createJunctionInsert(schema, ImmutableList.of(0, 0, 1))
        )
                .map({ query -> query.apply(MERGE_MERGE_M_2_N_FEATURE).first() })
                .collect(Collectors.toList());

        LOGGER.debug("SQL \n{}", Joiner.on('\n')
                .join(queries));

        then:

        queries == MERGE_MERGE_M_2_N_JUNCTION_EXPECTED
    }

}