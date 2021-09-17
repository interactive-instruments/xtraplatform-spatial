/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import de.ii.xtraplatform.cql.app.CqlImpl
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlPathDefaults
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql
import de.ii.xtraplatform.feature.provider.sql.domain.SqlPathParser
import de.ii.xtraplatform.features.domain.*
import spock.lang.Shared
import spock.lang.Specification

class QuerySchemaDeriverSpec extends Specification {

    @Shared
    FeatureStorePathParserSql pathParserOld
    @Shared
    QuerySchemaDeriver schemaDeriver

    def setupSpec() {
        def syntax = ImmutableSqlPathSyntax.builder().build()
        def defaults = new ImmutableSqlPathDefaults.Builder().build()
        def cql = new CqlImpl()

        def pathParser = new SqlPathParser(defaults, cql)
        pathParserOld = new FeatureStorePathParserSql(syntax, cql)
        schemaDeriver = new QuerySchemaDeriver(pathParser)
    }

    //TODO: ignorable objects
    //TODO: nested objects with properties on all levels
    def 'query schema: #casename'() {

        when:

        List<SchemaSql> actual = source.accept(schemaDeriver)
        //def ft = source.accept(new FeatureSchemaToTypeVisitor("test"))
        //def old = pathParserOld.parse(source)

        then:

        //[toInstanceContainer(actual)] == old

        actual == expected

        where:

        casename                                | source                                                    || expected
        "value array"                           | FeatureSchemaFixtures.VALUE_ARRAY                         || QuerySchemaFixtures.VALUE_ARRAY
        "object array"                          | FeatureSchemaFixtures.OBJECT_ARRAY                        || QuerySchemaFixtures.OBJECT_ARRAY
        "merge"                                 | FeatureSchemaFixtures.MERGE                               || QuerySchemaFixtures.MERGE
        "self joins"                            | FeatureSchemaFixtures.SELF_JOINS                          || QuerySchemaFixtures.SELF_JOINS
        //"self joins with filters"               | FeatureSchemaFixtures.SELF_JOINS_FILTER                   || QuerySchemaFixtures.SELF_JOINS_FILTER
        //"self join with nested duplicate join" | FeatureSchemaFixtures.SELF_JOIN_NESTED_DUPLICATE          || QuerySchemaFixtures.SELF_JOIN_NESTED_DUPLICATE
        "object without sourcePath"             | FeatureSchemaFixtures.OBJECT_WITHOUT_SOURCE_PATH          || QuerySchemaFixtures.OBJECT_WITHOUT_SOURCE_PATH
        "multiple sourcePaths"                  | FeatureSchemaFixtures.PROPERTY_WITH_MULTIPLE_SOURCE_PATHS || QuerySchemaFixtures.PROPERTY_WITH_MULTIPLE_SOURCE_PATHS
    }

    static FeatureStoreInstanceContainer toInstanceContainer(SchemaSql schema) {
        def builder = ImmutableFeatureStoreInstanceContainer.builder()
                .name(schema.getName())

        schema.getSortKey().ifPresent(builder::sortKey)

        schema.getProperties().forEach(property -> {
            if (property.isValue()) {
                builder.addAttributes(toAttribute(property, ""))
            } else {
                def related = ImmutableFeatureStoreRelatedContainer.builder()
                        .name(property.getName())

                related.sortKey(property.getSortKey().orElse(property.getRelation().get(property.getRelation().size() - 1).getTargetField()))

                property.getProperties().forEach(childProperty -> {
                    related.addAttributes(toAttribute(childProperty, property.getSourcePath().orElse("")))
                })
                //TODO: filter
                property.getRelation().forEach(sqlRelation -> {
                    def rel = ImmutableFeatureStoreRelation.builder()
                            .sourceContainer(sqlRelation.getSourceContainer())
                            .sourceField(sqlRelation.getSourceField())
                            .sourceSortKey(sqlRelation.getSourceSortKey())
                            .targetContainer(sqlRelation.getTargetContainer())
                            .targetField(sqlRelation.getTargetField())
                            .junctionSource(sqlRelation.getJunctionSource())
                            .junction(sqlRelation.getJunction())
                            .junctionTarget(sqlRelation.getJunctionTarget())
                            .cardinality(FeatureStoreRelation.CARDINALITY.valueOf(sqlRelation.getCardinality().toString()))
                            .build()
                    related.addInstanceConnection(rel)
                })

                builder.addRelatedContainers(related.build())
            }
        })

        return builder.build()
    }

    static FeatureStoreAttribute toAttribute(SchemaSql property, String parentPath) {
        return ImmutableFeatureStoreAttribute.builder()
                .name(property.getName())
                .queryable(parentPath.isEmpty() ? property.getSourcePath().orElse("") : parentPath + "." + property.getSourcePath().orElse(""))
                .path(property.getFullPath())
                .isId(property.isId())
                .isSpatial(property.isGeometry())
                .isTemporal(property.isTemporal())
                .constantValue(property.getConstantValue())
                .build()
    }

}
