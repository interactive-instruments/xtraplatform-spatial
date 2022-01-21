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
import de.ii.xtraplatform.feature.provider.sql.domain.SqlPathParser
import spock.lang.Shared
import spock.lang.Specification

class SqlPathParserSpec extends Specification {

    @Shared
    SqlPathParser pathParser

    def setupSpec() {
        def defaults = new ImmutableSqlPathDefaults.Builder().build()
        def cql = new CqlImpl()

        pathParser = new SqlPathParser(defaults, cql)
    }

    def 'root table'() {

        given:

        def path = "/externalprovider"

        when:

        def actual = pathParser.parseTablePath(path)

        then:

        actual == SqlPathFixtures.ROOT_TABLE

        actual.isRoot() && !actual.isBranch() && !actual.isLeaf()
    }

    def 'branch table'() {

        given:

        def path = "[id=boreholepath_fk]explorationsite"

        when:

        def actual = pathParser.parseTablePath(path)

        then:

        actual == SqlPathFixtures.BRANCH_TABLE

        !actual.isRoot() && actual.isBranch() && !actual.isLeaf()
    }

    def 'branch tables'() {

        given:

        def path = "[id=explorationsite_fk]explorationsite_task/[task_fk=id]task"

        when:

        def actual = pathParser.parseTablePath(path)

        then:

        actual == SqlPathFixtures.BRANCH_TABLES

        !actual.isRoot() && actual.isBranch() && !actual.isLeaf()
    }

    def 'leaf column'() {

        given:

        def path = "externalprovidername"

        when:

        def actual = pathParser.parseColumnPath(path)

        then:

        actual == SqlPathFixtures.SIMPLE_COLUMN

        !actual.isRoot() && !actual.isBranch() && actual.isLeaf()
    }

    def 'leaf table + column'() {

        given:

        def path = "[id=externalprovider_fk]externalprovider_externalprovidername/externalprovidername"

        when:

        def actual = pathParser.parseColumnPath(path)

        then:

        actual == SqlPathFixtures.LEAF_TABLE_COLUMN

        !actual.isRoot() && !actual.isBranch() && actual.isLeaf()
    }

    def 'custom sort key'() {

        given:

        def path = "[id=externalprovider_fk]externalprovider_externalprovidername{sortKey=oid}"

        when:

        def actual = pathParser.parseTablePath(path)

        then:

        actual == SqlPathFixtures.CUSTOM_SORT_KEY
    }

    def 'custom primary key'() {

        given:

        def path = "[id=externalprovider_fk]externalprovider_externalprovidername{primaryKey=oid}"

        when:

        def actual = pathParser.parseTablePath(path)

        then:

        actual == SqlPathFixtures.CUSTOM_PRIMARY_KEY
    }

    def 'custom filter'() {

        given:

        def path = "[id=externalprovider_fk]externalprovider_externalprovidername{filter=category=1}"

        when:

        def actual = pathParser.parseTablePath(path)

        then:

        actual == SqlPathFixtures.CUSTOM_FILTER
    }

    def 'multiple flags'() {

        given:

        def path = "[id=externalprovider_fk]externalprovider_externalprovidername{sortKey=oid}{primaryKey=oid}{filter=category=1}"

        when:

        def actual = pathParser.parseTablePath(path)

        then:

        actual == SqlPathFixtures.MULTIPLE_FLAGS
    }

}
