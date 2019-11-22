package de.ii.xtraplatform.feature.provider.sql.infra.db

import de.ii.xtraplatform.feature.provider.api.FeatureProperty
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableConnectionInfoSql
import spock.lang.Shared
import spock.lang.Specification

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
}
