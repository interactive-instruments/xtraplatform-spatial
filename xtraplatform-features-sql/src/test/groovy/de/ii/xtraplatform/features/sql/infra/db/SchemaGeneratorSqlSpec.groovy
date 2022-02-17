/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql
import de.ii.xtraplatform.features.sql.domain.SqlClient
import schemacrawler.crawl.MutableCatalog
import schemacrawler.crawl.MutableColumn
import schemacrawler.crawl.MutableColumnDataType
import schemacrawler.crawl.MutablePrimaryKey
import schemacrawler.crawl.MutableTable
import schemacrawler.crawl.MutableTableConstraintColumn
import schemacrawler.schema.Catalog
import schemacrawler.schema.Column
import schemacrawler.schema.ColumnDataType
import schemacrawler.schema.JavaSqlType
import schemacrawler.schema.JavaSqlTypeGroup
import schemacrawler.schema.PrimaryKey
import schemacrawler.schema.Table
import schemacrawler.schemacrawler.SchemaReference
import spock.lang.Shared
import spock.lang.Specification

import java.sql.Array
import java.sql.JDBCType
import java.sql.Timestamp

class SchemaGeneratorSqlSpec extends Specification {

    @Shared SchemaGeneratorSql sqlSchemaCrawler

    def setupSpec() {

        def connectionInfo = new ImmutableConnectionInfoSql.Builder().host("localhost:5433")
                .database("daraa")
                .user("postgres")
                .password("postgres")
                .build()

        sqlSchemaCrawler = new SchemaGeneratorSql(Mock(SqlClient), [], [], new SqlDialectPostGis());

    }

    def 'mock catalog'() {
        given: 'mock catalog'
        Catalog mockCatalog = createCatalog()

        when: 'crawl the catalog'
        def featureTypeList = sqlSchemaCrawler.getFeatureTypes(mockCatalog, ImmutableList.of(), ImmutableMap.of())


        then: 'check results'
        featureTypeList.size() == 2

        featureTypeList.get(0).name == "table1"
        featureTypeList.get(0).sourcePath.get() == "/table1"
        featureTypeList.get(0).propertyMap.size() == 2
        featureTypeList.get(0).propertyMap.containsKey("column1")
        featureTypeList.get(0).propertyMap.get("column1").type == SchemaBase.Type.INTEGER
        featureTypeList.get(0).propertyMap.get("column1").role.isPresent()
        featureTypeList.get(0).propertyMap.get("column1").role.get() == SchemaBase.Role.ID
        featureTypeList.get(0).propertyMap.containsKey("column2")
        featureTypeList.get(0).propertyMap.get("column2").type == SchemaBase.Type.STRING
//        featureTypeList.get(0).propertyMap.containsKey("column3")
//        featureTypeList.get(0).propertyMap.get("column3").type == SchemaBase.Type.GEOMETRY

        featureTypeList.get(1).name == "table2"
        featureTypeList.get(1).sourcePath.get() == "/table2"
        featureTypeList.get(1).propertyMap.size() == 4
        featureTypeList.get(1).propertyMap.containsKey("column5")
        featureTypeList.get(1).propertyMap.get("column5").type == SchemaBase.Type.INTEGER
        featureTypeList.get(1).propertyMap.get("column5").role.isPresent()
        featureTypeList.get(1).propertyMap.get("column5").role.get() == SchemaBase.Role.ID
        featureTypeList.get(1).propertyMap.containsKey("column6")
        featureTypeList.get(1).propertyMap.get("column6").type == SchemaBase.Type.FLOAT
        featureTypeList.get(1).propertyMap.containsKey("column7")
        featureTypeList.get(1).propertyMap.get("column7").type == SchemaBase.Type.BOOLEAN
        featureTypeList.get(1).propertyMap.containsKey("column9")
        featureTypeList.get(1).propertyMap.get("column9").type == SchemaBase.Type.DATETIME
    }


    static def createCatalog() {

        Catalog catalog = new MutableCatalog("mockCatalog")
        SchemaReference schema = new SchemaReference("mockCatalog", "mockSchema")
        catalog.addSchema(schema)

        // Add 2 tables with different column types to test every possible mapping
        // Table 1: integer, string, geometry, unknown object
        Table table = new MutableTable(schema, "table1")
        catalog.addTable(table)

        Column column = new MutableColumn(table, "column1")
        ColumnDataType columnDataType = new MutableColumnDataType(schema, "serial")
        columnDataType.setJavaSqlType(new JavaSqlType(JDBCType.INTEGER, Integer.class, JavaSqlTypeGroup.integer))
        column.setColumnDataType(columnDataType)
        table.addColumn(column)

        PrimaryKey primaryKey = new MutablePrimaryKey(table, "pkey1")
        primaryKey.addColumn(new MutableTableConstraintColumn(null, column))
        table.setPrimaryKey(primaryKey)

        column = new MutableColumn(table, "column2")
        columnDataType = new MutableColumnDataType(schema, "varchar")
        columnDataType.setJavaSqlType(new JavaSqlType(JDBCType.VARCHAR, String.class, JavaSqlTypeGroup.character))
        column.setColumnDataType(columnDataType)
        table.addColumn(column)

        column = new MutableColumn(table, "column3")
        columnDataType = new MutableColumnDataType(schema, "geometry")
        columnDataType.setJavaSqlType(new JavaSqlType(JDBCType.OTHER, Object.class, JavaSqlTypeGroup.object))
        column.setColumnDataType(columnDataType)
        table.addColumn(column)

        column = new MutableColumn(table, "column4")
        columnDataType = new MutableColumnDataType(schema, "_text")
        columnDataType.setJavaSqlType(new JavaSqlType(JDBCType.ARRAY, Array.class, JavaSqlTypeGroup.object))
        column.setColumnDataType(columnDataType)
        table.addColumn(column)

        // Table 2: integer, float, boolean, unknown object, temporal
        table = new MutableTable(schema, "table2")
        catalog.addTable(table)

        column = new MutableColumn(table, "column5")
        columnDataType = new MutableColumnDataType(schema, "serial")
        columnDataType.setJavaSqlType(new JavaSqlType(JDBCType.INTEGER, Integer.class, JavaSqlTypeGroup.integer))
        column.setColumnDataType(columnDataType)
        table.addColumn(column)

        primaryKey = new MutablePrimaryKey(table, "pkey2")
        primaryKey.addColumn(new MutableTableConstraintColumn(null, column))
        table.setPrimaryKey(primaryKey)

        column = new MutableColumn(table, "column6")
        columnDataType = new MutableColumnDataType(schema, "numeric")
        columnDataType.setJavaSqlType(new JavaSqlType(JDBCType.NUMERIC, BigDecimal.class, JavaSqlTypeGroup.real))
        column.setColumnDataType(columnDataType)
        table.addColumn(column)

        column = new MutableColumn(table, "column7")
        columnDataType = new MutableColumnDataType(schema, "bool")
        columnDataType.setJavaSqlType(new JavaSqlType(JDBCType.BIT, Boolean.class, JavaSqlTypeGroup.bit))
        column.setColumnDataType(columnDataType)
        table.addColumn(column)

        column = new MutableColumn(table, "column8")
        columnDataType = new MutableColumnDataType(schema, "_float8")
        columnDataType.setJavaSqlType(new JavaSqlType(JDBCType.ARRAY, Array.class, JavaSqlTypeGroup.object))
        column.setColumnDataType(columnDataType)
        table.addColumn(column)

        column = new MutableColumn(table, "column9")
        columnDataType = new MutableColumnDataType(schema, "datetime")
        columnDataType.setJavaSqlType(new JavaSqlType(JDBCType.TIMESTAMP, Timestamp.class, JavaSqlTypeGroup.temporal))
        column.setColumnDataType(columnDataType)
        table.addColumn(column)

        return catalog
    }
}
