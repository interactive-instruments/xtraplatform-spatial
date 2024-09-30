/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app


import de.ii.xtraplatform.cql.domain.Eq
import de.ii.xtraplatform.cql.domain.ScalarLiteral
import de.ii.xtraplatform.features.domain.ImmutableTuple
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlPath
import de.ii.xtraplatform.features.sql.domain.SqlPath

class SqlPathFixtures {

    private static baseTable = { it ->
        new ImmutableSqlPath.Builder()
                .sortKey("id")
                .primaryKey("id")
                .junction(false)
    }

    static ROOT_TABLE = baseTable()
            .name("externalprovider")
            .build()

    static BRANCH_TABLE = baseTable()
            .name("explorationsite")
            .join(ImmutableTuple.of("id", "boreholepath_fk"))
            .joinType(SqlPath.JoinType.INNER)
            .build()

    static BRANCH_TABLES = baseTable()
            .name("task")
            .join(ImmutableTuple.of("task_fk", "id"))
            .joinType(SqlPath.JoinType.INNER)
            .parentTables([baseTable()
                                   .name("explorationsite_task")
                                   .join(ImmutableTuple.of("id", "explorationsite_fk"))
                                   .joinType(SqlPath.JoinType.INNER)
                                   .build()])
            .build()

    //TODO
    static SIMPLE_COLUMN = new ImmutableSqlPath.Builder().sortKey("").primaryKey("").junction(false)
            .name("externalprovidername")
            .columns(["externalprovidername"])
            .build()

    //TODO
    static LEAF_TABLE_COLUMN = new ImmutableSqlPath.Builder().sortKey("").primaryKey("").junction(false)
            .name("externalprovidername")
            .columns(["externalprovidername"])
            .parentTables([baseTable()
                                   .name("externalprovider_externalprovidername")
                                   .join(ImmutableTuple.of("id", "externalprovider_fk"))
                                   .joinType(SqlPath.JoinType.INNER)
                                   .build()])
            .build()

    static CUSTOM_SORT_KEY = baseTable()
            .name("externalprovider_externalprovidername")
            .join(ImmutableTuple.of("id", "externalprovider_fk"))
            .joinType(SqlPath.JoinType.INNER)
            .sortKey("oid")
            .build()

    static CUSTOM_PRIMARY_KEY = baseTable()
            .name("externalprovider_externalprovidername")
            .join(ImmutableTuple.of("id", "externalprovider_fk"))
            .joinType(SqlPath.JoinType.INNER)
            .primaryKey("oid")
            .build()

    static CUSTOM_FILTER = baseTable()
            .name("externalprovider_externalprovidername")
            .join(ImmutableTuple.of("id", "externalprovider_fk"))
            .joinType(SqlPath.JoinType.INNER)
            .filter(Eq.of("category", ScalarLiteral.of(1)))
            .filterString("category=1")
            .build()

    static MULTIPLE_FLAGS = baseTable()
            .name("externalprovider_externalprovidername")
            .join(ImmutableTuple.of("id", "externalprovider_fk"))
            .joinType(SqlPath.JoinType.INNER)
            .sortKey("oid")
            .primaryKey("oid")
            .filter(Eq.of("category", ScalarLiteral.of(1)))
            .filterString("category=1")
            .build()
}
