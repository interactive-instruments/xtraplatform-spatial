/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.crs.domain.OgcCrs
import de.ii.xtraplatform.feature.provider.sql.ImmutableOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

import java.util.function.Function

import static de.ii.xtraplatform.feature.provider.sql.app.SqlInsertsFixtures.*

/**
 * @author zahnen
 */
class FeatureMutationsSqlSpec extends Specification {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMutationsSqlSpec.class);

    def 'createInserts'() {

        given:

        FeatureMutationsSql inserts = Spy(new FeatureMutationsSql(null, new SqlInsertGenerator2(OgcCrs.CRS84, null, new ImmutableOptions.Builder().build())))

        Map<List<String>, List<Integer>> rows = ImmutableMap.<List<String>, List<Integer>> builder()
                .put(MAIN_M_2_N_SCHEMA.getFullPath(), ImmutableList.of(3))
                .put(MERGE_MERGE_M_2_N_SCHEMA.getFullPath(), ImmutableList.of(2))
                .build()
        RowCursor rowCursor = new RowCursor(FULL.getPath())
        boolean withId = false

        when:

        //List<Function<FeatureSql, Pair<String, Consumer<String>>>> accept = FULL.accept(new FeatureMutationsSql.StatementsVisitor(rows, rowCursor, false));

        inserts.createInstanceInserts(FULL, rows, rowCursor, withId, Optional.empty())

        then:
        1 * inserts.createAttributesInserts(FULL, [0], withId)

        //TODO: after merge_merge
        then:
        1 * inserts.createAttributesInserts(MERGE_WITH_CHILDREN, [0,0], withId)

        then:
        1 * inserts.createAttributesInserts(MERGE_MERGE_WITH_CHILDREN, [0,0,0], withId)

        then:
        1 * inserts.createAttributesInserts(MERGE_MERGE_ONE_2_ONE_SCHEMA, [0,0,0,0], withId)

        then:
        1 * inserts.createAttributesInserts(MERGE_MERGE_M_2_N_SCHEMA, [0,0,0,0], withId)

        then:
        1 * inserts.createAttributesInserts(MERGE_MERGE_M_2_N_SCHEMA, [0,0,0,1], withId)

        then:
        1 * inserts.createAttributesInserts(MAIN_M_2_N_SCHEMA, [0,0], withId)

        then:
        1 * inserts.createAttributesInserts(MAIN_M_2_N_SCHEMA, [0,1], withId)

        then:
        1 * inserts.createAttributesInserts(MAIN_M_2_N_SCHEMA, [0,2], withId)

        then:
        0 * inserts.createAttributesInserts(_, _, _)

    }

    def 'createInserts: merge'() {

        given:

        FeatureStoreInsertGenerator generator = Mock();
        FeatureMutationsSql inserts = new FeatureMutationsSql(null, generator)
        List<Integer> rows = ImmutableList.of(0, 0, 1)

        when:

        inserts.createAttributesInserts(MERGE_MERGE_SCHEMA, rows, false)

        then:

        1 * generator.createInsert(MERGE_MERGE_SCHEMA, rows, false) >> Mock(Function)
        0 * _

    }

    def 'createInserts: merge + 1:1'() {

        given:

        FeatureStoreInsertGenerator generator = Mock();
        FeatureMutationsSql inserts = new FeatureMutationsSql(null, generator)
        List<Integer> rows = ImmutableList.of(0, 0, 0, 1)

        when:

        inserts.createAttributesInserts(MERGE_MERGE_ONE_2_ONE_SCHEMA, rows, false)

        then:
        1 * generator.createInsert(MERGE_MERGE_ONE_2_ONE_SCHEMA, rows, false) >> Mock(Function)

        then:
        1 * generator.createForeignKeyUpdate(MERGE_MERGE_ONE_2_ONE_SCHEMA, rows) >> Mock(Function)

        then:
        0 * _

    }

    def 'createInserts: merge + m:n'() {

        given:

        FeatureStoreInsertGenerator generator = Mock();
        FeatureMutationsSql inserts = new FeatureMutationsSql(null, generator)
        List<Integer> rows = ImmutableList.of(0, 0, 0, 1)

        when:

        inserts.createAttributesInserts(MERGE_MERGE_M_2_N_SCHEMA, rows, false)

        then:
        1 * generator.createInsert(MERGE_MERGE_M_2_N_SCHEMA, rows, false) >> Mock(Function)

        then:
        1 * generator.createJunctionInsert(MERGE_MERGE_M_2_N_SCHEMA, rows) >> Mock(Function)

        then:
        0 * _

    }

}
