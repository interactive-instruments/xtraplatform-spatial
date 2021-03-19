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
import de.ii.xtraplatform.features.domain.FeatureSchemaToTypeVisitor
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

        SchemaSql actual = source.accept(schemaDeriver)
        def ft = source.accept(new FeatureSchemaToTypeVisitor("test"))
        def old = pathParserOld.parse(ft)

        then:

        [actual] == expected

        where:

        casename       | source                             || expected
        //"value array"  | FeatureSchemaFixtures.VALUE_ARRAY  || QuerySchemaFixtures.VALUE_ARRAY
        //"object array" | FeatureSchemaFixtures.OBJECT_ARRAY || QuerySchemaFixtures.OBJECT_ARRAY
        "merge"        | FeatureSchemaFixtures.MERGE        || QuerySchemaFixtures.MERGE
    }

/*

  eignungsflaeche:
    sourcePath: /eignungsflaeche
    type: OBJECT
    properties:
      id:
        sourcePath: '[id=id]osirisobjekt/id'
        type: STRING
        role: ID
      kennung:
        sourcePath: '[id=id]osirisobjekt/kennung'
        type: STRING
      bezeichnung:
        sourcePath: '[id=id]osirisobjekt/bezeichnung'
        type: STRING
      veroeffentlichtAm:
        sourcePath: '[id=id]osirisobjekt/veroeffentlichtam'
        type: DATETIME
      verantwortlicheStelle:
        sourcePath: '[id=id]osirisobjekt/verantwortlichestelle'
        type: STRING
      raumreferenz:
        sourcePath: '[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz'
        type: OBJECT_ARRAY
        properties:
          ortsangabe:
            sourcePath: '[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben'
            type: OBJECT_ARRAY
            properties:
              kreisSchluessel:
                sourcePath: kreisschluessel
                type: STRING
              verbandsgemeindeSchluessel:
                sourcePath: verbandsgemeindeschluessel
                type: STRING
              gemeindeSchluessel:
                sourcePath: gemeindeschluessel
                type: STRING
              flurstuecksKennzeichen:
                sourcePath: '[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen'
                type: VALUE_ARRAY
                valueType: STRING
          datumAbgleich:
            sourcePath: datumabgleich
            type: DATETIME
          fachreferenz:
            sourcePath: '[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id'
            type: VALUE_ARRAY
            valueType: STRING


 */

}
