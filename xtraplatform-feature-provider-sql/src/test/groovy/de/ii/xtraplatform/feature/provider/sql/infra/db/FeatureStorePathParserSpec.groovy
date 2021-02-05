/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db

import de.ii.xtraplatform.cql.app.CqlImpl
import de.ii.xtraplatform.feature.provider.sql.app.FeatureStorePathParserSql
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreRelation
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class FeatureStorePathParserSpec extends Specification {
/*
    @Shared
            pathParser

    def setupSpec() {
        def syntax = ImmutableSqlPathSyntax.builder().build()
        def cql = new CqlImpl()

        pathParser = new FeatureStorePathParserSql(syntax, cql)
    }

    @Unroll
    def 'relations (#description)'() {

        given: "a path to a container"

        when: "parsing relations"

        def actual = pathParser.toRelations(path)

        then:
        actual == expected

        where:
        path << [
                ['biotop', '[id=id]osirisobjekt'],
                ['biotop', '[geom=id]geom'],
                ['biotop', '[id=id]osirisobjekt', '[geom=id]geom'],
                ['biotop', '[id=biotop_id]biotop_2_biotoptyp', '[biotoptyp_id=id]biotoptyp'],
                ['massnahmebb', '[id=massnahmebb_id]massnahmebb_2_massnahme', '[massnahme_id=id]massnahmeangabe', '[id=massnahmeangabe_id]massnahmeangabe_zusatzmerkmal'],
                ['biotop', '[id=id]osirisobjekt', '[id=osirisobjekt_id]osirisobjekt_2_raumreferenz', '[raumreferenz_id=id]raumreferenz', '[id=raumreferenz_id]raumreferenz_2_ortsangabe', '[ortsangabe_id=id]ortsangaben'],
                ['massnahmebb', '[id=massnahmebb_id]massnahmebb_2_massnahme', '[massnahme_id=id]massnahmeangabe', '[id=massnahmeangabe_id]massnahmeangabe_2_zustand', '[zustand_id=id]zustandangaben', '[id=zustandangaben_id]zustandangaben_2_ausgangszustand', '[ausgangszustand_id=id]biotopkurz', '[id=biotopkurz_id]biotopkurz_2_zusatzbezeichnung', '[zusatzbezeichnung_id=id]zcodebt']
        ]
        description << [
                '1:1',
                'n:1',
                '1:1 + n:1',
                'm:n',
                'm:n + 1:n',
                '1:1 + m:n + m:n',
                'm:n + m:n + m:n + m:n'
        ]
        expected << [
                [ImmutableFeatureStoreRelation.builder()
                         .cardinality(ONE_2_ONE)
                         .sourceContainer('biotop')
                         .sourceField('id')
                         .targetContainer('osirisobjekt')
                         .targetField('id')
                         .build()
                ],
                [ImmutableFeatureStoreRelation.builder()
                         .cardinality(ONE_2_ONE)
                         .sourceContainer('biotop')
                         .sourceField('geom')
                         .sourceSortKey("id")
                         .targetContainer('geom')
                         .targetField('id')
                         .build()
                ],
                [ImmutableFeatureStoreRelation.builder()
                         .cardinality(ONE_2_ONE)
                         .sourceContainer('biotop')
                         .sourceField('id')
                         .targetContainer('osirisobjekt')
                         .targetField('id')
                         .build(),
                 ImmutableFeatureStoreRelation.builder()
                         .cardinality(ONE_2_ONE)
                         .sourceContainer('osirisobjekt')
                         .sourceField('geom')
                         .sourceSortKey("id")
                         .targetContainer('geom')
                         .targetField('id')
                         .build()
                ],
                [ImmutableFeatureStoreRelation.builder()
                         .cardinality(M_2_N)
                         .sourceContainer('biotop')
                         .sourceField('id')
                         .junctionSource('biotop_id')
                         .junction('biotop_2_biotoptyp')
                         .junctionTarget('biotoptyp_id')
                         .targetContainer('biotoptyp')
                         .targetField('id')
                         .build()
                ],
                [ImmutableFeatureStoreRelation.builder()
                         .cardinality(M_2_N)
                         .sourceContainer('massnahmebb')
                         .sourceField('id')
                         .junctionSource('massnahmebb_id')
                         .junction('massnahmebb_2_massnahme')
                         .junctionTarget('massnahme_id')
                         .targetContainer('massnahmeangabe')
                         .targetField('id')
                         .build(),
                 ImmutableFeatureStoreRelation.builder()
                         .cardinality(ONE_2_N)
                         .sourceContainer('massnahmeangabe')
                         .sourceField('id')
                         .targetContainer('massnahmeangabe_zusatzmerkmal')
                         .targetField('massnahmeangabe_id')
                         .build(),
                ],
                [ImmutableFeatureStoreRelation.builder()
                         .cardinality(ONE_2_ONE)
                         .sourceContainer('biotop')
                         .sourceField('id')
                         .targetContainer('osirisobjekt')
                         .targetField('id')
                         .build(),
                 ImmutableFeatureStoreRelation.builder()
                         .cardinality(M_2_N)
                         .sourceContainer('osirisobjekt')
                         .sourceField('id')
                         .junctionSource('osirisobjekt_id')
                         .junction('osirisobjekt_2_raumreferenz')
                         .junctionTarget('raumreferenz_id')
                         .targetContainer('raumreferenz')
                         .targetField('id')
                         .build(),
                 ImmutableFeatureStoreRelation.builder()
                         .cardinality(M_2_N)
                         .sourceContainer('raumreferenz')
                         .sourceField('id')
                         .junctionSource('raumreferenz_id')
                         .junction('raumreferenz_2_ortsangabe')
                         .junctionTarget('ortsangabe_id')
                         .targetContainer('ortsangaben')
                         .targetField('id')
                         .build(),
                ],
                [ImmutableFeatureStoreRelation.builder()
                         .cardinality(M_2_N)
                         .sourceContainer('massnahmebb')
                         .sourceField('id')
                         .junctionSource('massnahmebb_id')
                         .junction('massnahmebb_2_massnahme')
                         .junctionTarget('massnahme_id')
                         .targetContainer('massnahmeangabe')
                         .targetField('id')
                         .build(),
                 ImmutableFeatureStoreRelation.builder()
                         .cardinality(M_2_N)
                         .sourceContainer('massnahmeangabe')
                         .sourceField('id')
                         .junctionSource('massnahmeangabe_id')
                         .junction('massnahmeangabe_2_zustand')
                         .junctionTarget('zustand_id')
                         .targetContainer('zustandangaben')
                         .targetField('id')
                         .build(),
                 ImmutableFeatureStoreRelation.builder()
                         .cardinality(M_2_N)
                         .sourceContainer('zustandangaben')
                         .sourceField('id')
                         .junctionSource('zustandangaben_id')
                         .junction('zustandangaben_2_ausgangszustand')
                         .junctionTarget('ausgangszustand_id')
                         .targetContainer('biotopkurz')
                         .targetField('id')
                         .build(),
                 ImmutableFeatureStoreRelation.builder()
                         .cardinality(M_2_N)
                         .sourceContainer('biotopkurz')
                         .sourceField('id')
                         .junctionSource('biotopkurz_id')
                         .junction('biotopkurz_2_zusatzbezeichnung')
                         .junctionTarget('zusatzbezeichnung_id')
                         .targetContainer('zcodebt')
                         .targetField('id')
                         .build(),
                ]
        ]

    }

    def 'relations (errors)'() {

        given: "an invalid path"

        when: "parsing relations"

        def actual = pathParser.toRelations(path)

        then: "should throw"

        thrown(IllegalArgumentException)

        where:
        path << [
                [],
                ['biotop'],
                ['biotop', 'id'],
                ['biotop', '[id=id]osirisobjekt', 'id']
        ]

    }

    */
}
