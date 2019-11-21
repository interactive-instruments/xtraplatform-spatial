package de.ii.xtraplatform.feature.provider.sql.infra.db


import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableConnectionInfoSql
import spock.lang.Shared
import spock.lang.Specification

class SqlSchemaCrawlerPlayground extends Specification {

    @Shared SqlSchemaCrawler sqlSchemaCrawler

    def setupSpec() {

        def connectionInfo = new ImmutableConnectionInfoSql.Builder().host("localhost:5433")
                .database("daraa")
                .user("postgres")
                .password("postgres")
                .build()

        sqlSchemaCrawler = new SqlSchemaCrawler(connectionInfo);

    }

    def 'parse schema'() {

        given:

        when:

        sqlSchemaCrawler.parseSchema("public", null)

        then:

        true == true

    }
}
