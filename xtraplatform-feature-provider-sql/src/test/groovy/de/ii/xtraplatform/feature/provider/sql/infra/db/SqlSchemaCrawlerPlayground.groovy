package de.ii.xtraplatform.feature.provider.sql.infra.db

import de.ii.xtraplatform.features.domain.FeatureProperty
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableConnectionInfoSql
import schemacrawler.crawl.MutableCatalog
import schemacrawler.crawl.MutableColumn
import schemacrawler.crawl.MutableColumnDataType
import schemacrawler.crawl.MutableTable
import schemacrawler.schema.Catalog
import schemacrawler.schema.Column
import schemacrawler.schema.ColumnDataType
import schemacrawler.schema.JavaSqlType
import schemacrawler.schema.JavaSqlTypeGroup
import schemacrawler.schema.SchemaReference
import schemacrawler.schema.Table
import spock.lang.Shared
import spock.lang.Specification

import java.sql.Array
import java.sql.JDBCType
import java.sql.Timestamp

class SqlSchemaCrawlerPlayground extends Specification {

    @Shared SqlSchemaCrawler sqlSchemaCrawler

    def setupSpec() {

        def connectionInfo = new ImmutableConnectionInfoSql.Builder().host("ldproxy02:5433")
                .database("daraa")
                .user("postgres")
                .password("postgres")
                .build()

        sqlSchemaCrawler = new SqlSchemaCrawler(connectionInfo);

    }

    def 'parse schema'() {

        given:
            String schemaName = "public"

        when:
            def featureTypeList = sqlSchemaCrawler.parseSchema(schemaName, null)

        then:

        featureTypeList.size() > 0
        featureTypeList.get(0).name == "aeronauticcrv"
        featureTypeList.get(0).properties.size() > 0
        featureTypeList.get(0).properties.get(0).name == "id"
        featureTypeList.get(0).properties.get(0).type == FeatureProperty.Type.INTEGER
        featureTypeList.get(0).properties.get(1).name == "ara"
        featureTypeList.get(0).properties.get(1).type == FeatureProperty.Type.FLOAT
        featureTypeList.get(0).properties.get(3).name == "ben"
        featureTypeList.get(0).properties.get(3).type == FeatureProperty.Type.STRING

    }

    def 'mock catalog'() {
        given: 'mock catalog'
        Catalog mockCatalog = createCatalog()

        when: 'crawl the catalog'
        def featureTypeList = sqlSchemaCrawler.getFeatureTypes(mockCatalog)


        then: 'check results'
        featureTypeList.size() == 2

        featureTypeList.get(0).name == "table1"
        featureTypeList.get(0).properties.size() == 3
        featureTypeList.get(0).properties.any{ it.name == "column1" && it.type == FeatureProperty.Type.INTEGER }
        featureTypeList.get(0).properties.any{ it.name == "column2" && it.type == FeatureProperty.Type.STRING }
        featureTypeList.get(0).properties.any{ it.name == "column3" && it.type == FeatureProperty.Type.GEOMETRY }

        featureTypeList.get(1).name == "table2"
        featureTypeList.get(1).properties.size() == 4
        featureTypeList.get(1).properties.any{ it.name == "column5" && it.type == FeatureProperty.Type.INTEGER}
        featureTypeList.get(1).properties.any{ it.name == "column6" && it.type == FeatureProperty.Type.FLOAT}
        featureTypeList.get(1).properties.any{ it.name == "column7" && it.type == FeatureProperty.Type.BOOLEAN}
        featureTypeList.get(1).properties.any{ it.name == "column9" && it.type == FeatureProperty.Type.DATETIME}
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
