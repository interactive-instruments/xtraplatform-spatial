/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testng.Assert.*;

/**
 * @author zahnen
 */
public class SqlFeatureQueriesTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFeatureQueriesTest.class);

    @Test
    public void test() {

        SqlPathTree sqlPathTree = new SqlPathTree.Builder()
                .fromPaths("/fundorttiere/[id=id]osirisobjekt/id", "/fundorttiere/[id=id]osirisobjekt/kennung", "/fundorttiere/[id=id]osirisobjekt/bezeichnung", "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam", "/fundorttiere/[id=id]osirisobjekt/verantwortlichestelle", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id", "/fundorttiere/[id=id]osirisobjekt/bemerkung", "/fundorttiere/[id=id]artbeobachtung/anzahl", "/fundorttiere/[id=id]artbeobachtung/begehungsmethode", "/fundorttiere/[id=id]artbeobachtung/bemerkunginformationsquelle", "/fundorttiere/[id=id]artbeobachtung/beobachtetam", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "/fundorttiere/[id=id]artbeobachtung/haeufigkeit", "/fundorttiere/[id=id]artbeobachtung/informationsquelle", "/fundorttiere/[id=id]artbeobachtung/letzteskartierdatum", "/fundorttiere/[id=id]artbeobachtung/unschaerfe", "/fundorttiere/[id=id]artbeobachtung/bemerkungort", "/fundorttiere/tierart", "/fundorttiere/bemerkungtierart", "/fundorttiere/vorkommen", "/fundorttiere/nachweis", "/fundorttiere/erfassungsmethode", "/fundorttiere/bemerkungpopulationsstadium", "/fundorttiere")
                .build();

        SqlPathTree.depthFirst(sqlPathTree).forEach(s -> LOGGER.debug(s.getPath()));
LOGGER.debug("\n\n");

sqlPathTree.findChild("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen").forEach(s -> LOGGER.debug(s.getPath()));
        LOGGER.debug("\n\n");

        sqlPathTree.getAdditionalSortKeys("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen").forEach(s -> LOGGER.debug(s));
        LOGGER.debug("\n\n");

        ImmutableSqlFeatureQueries queries = ImmutableSqlFeatureQueries.builder()
                                                                       .sqlPaths(sqlPathTree)
                                                                       .addPaths("/fundorttiere/[id=id]osirisobjekt/id", "/fundorttiere/[id=id]osirisobjekt/kennung", "/fundorttiere/[id=id]osirisobjekt/bezeichnung", "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam", "/fundorttiere/[id=id]osirisobjekt/verantwortlichestelle", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id", "/fundorttiere/[id=id]osirisobjekt/bemerkung", "/fundorttiere/[id=id]artbeobachtung/anzahl", "/fundorttiere/[id=id]artbeobachtung/begehungsmethode", "/fundorttiere/[id=id]artbeobachtung/bemerkunginformationsquelle", "/fundorttiere/[id=id]artbeobachtung/beobachtetam", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "/fundorttiere/[id=id]artbeobachtung/haeufigkeit", "/fundorttiere/[id=id]artbeobachtung/informationsquelle", "/fundorttiere/[id=id]artbeobachtung/letzteskartierdatum", "/fundorttiere/[id=id]artbeobachtung/unschaerfe", "/fundorttiere/[id=id]artbeobachtung/bemerkungort", "/fundorttiere/tierart", "/fundorttiere/bemerkungtierart", "/fundorttiere/vorkommen", "/fundorttiere/nachweis", "/fundorttiere/erfassungsmethode", "/fundorttiere/bemerkungpopulationsstadium", "/fundorttiere")
                                                                       .build();

        Map<Integer, List<Integer>> dependencies = getDependencies(queries);

        List<String> selects = queries.getQueries()
                                      .stream()
                                      .map(q -> q.toSql("x=y", 10, 1))
                                      .collect(Collectors.toList());

        LOGGER.debug("SQL \n{}", Joiner.on('\n')
                                       .join(selects));

        assertEquals(selects, EXPECTED);
    }

    Map<Integer, List<Integer>> getDependencies(SqlFeatureQueries queries) {
        int mainQueryIndex = queries.getQueries()
                                    .indexOf(queries.getMainQuery());
        List<Integer> all = IntStream.range(0, queries.getQueries().size()).boxed().collect(Collectors.toList());
        all.remove(mainQueryIndex);

        Map<Integer, List<Integer>> dependencies = new LinkedHashMap<>();
        Map<String, List<Integer>> pdependencies = new LinkedHashMap<>();
        Map<String, Integer> parents = new LinkedHashMap<>();
        dependencies.put(mainQueryIndex, all);

        List<SqlFeatureQuery> queries1 = queries.getQueries();
        for (int i = 0; i < queries1.size(); i++) {
            SqlFeatureQuery q = queries1.get(i);
            if (q.getSqlPath()
                 .getType() == SqlPathTree.TYPE.ID_M_N) {
                //all.remove(i);
                //dependencies.put(i, new ArrayList<>());
                parents.putIfAbsent(q.getSqlPath().getPath(), i);
            }
            if (q.getSqlPathParent().isPresent() && q.getSqlPathParent().get().getType() == SqlPathTree.TYPE.ID_M_N) {
                pdependencies.putIfAbsent(q.getSqlPathParent().get().getPath(), new ArrayList<>());
                pdependencies.get(q.getSqlPathParent().get().getPath()).add(i);
            }
        }

        pdependencies.keySet().forEach(p -> {
            if (p.equals("/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz")) return;
            Integer index = parents.get(p);
            if (index != null) {
                all.removeAll(pdependencies.get(p));
                dependencies.putIfAbsent(index, new ArrayList<>());
                dependencies.get(index).addAll(pdependencies.get(p));
            }
        });

        return dependencies;
    }

    static final List<String> EXPECTED = ImmutableList.<String>builder()
            .add("SELECT fundorttiere.id AS SKEY, osirisobjekt.id, osirisobjekt.kennung, osirisobjekt.bezeichnung, osirisobjekt.veroeffentlichtam, osirisobjekt.verantwortlichestelle, osirisobjekt.bemerkung FROM fundorttiere JOIN osirisobjekt ON fundorttiere.id=osirisobjekt.id WHERE x=y ORDER BY 1 LIMIT 10 OFFSET 1")
            .add("SELECT fundorttiere.id AS SKEY, foto.fotoverweis, foto.hauptfoto, foto.aufnahmezeitpunkt, foto.bemerkung FROM fundorttiere JOIN osirisobjekt ON fundorttiere.id=osirisobjekt.id JOIN osirisobjekt_2_foto ON osirisobjekt.id=osirisobjekt_2_foto.osirisobjekt_id JOIN foto ON osirisobjekt_2_foto.foto_id=foto.id WHERE x=y ORDER BY 1 LIMIT 10 OFFSET 1")
            .add("SELECT fundorttiere.id AS SKEY, raumreferenz.id AS SKEY_1, ortsangaben.kreisschluessel, ortsangaben.verbandsgemeindeschluessel, ortsangaben.gemeindeschluessel FROM fundorttiere JOIN osirisobjekt ON fundorttiere.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id JOIN raumreferenz_2_ortsangabe ON raumreferenz.id=raumreferenz_2_ortsangabe.raumreferenz_id JOIN ortsangaben ON raumreferenz_2_ortsangabe.ortsangabe_id=ortsangaben.id WHERE x=y ORDER BY 1,2 LIMIT 10 OFFSET 1")
            .add("SELECT fundorttiere.id AS SKEY, raumreferenz.id AS SKEY_1, ortsangaben.id AS SKEY_2, ortsangaben_flurstueckskennzeichen.flurstueckskennzeichen FROM fundorttiere JOIN osirisobjekt ON fundorttiere.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id JOIN raumreferenz_2_ortsangabe ON raumreferenz.id=raumreferenz_2_ortsangabe.raumreferenz_id JOIN ortsangaben ON raumreferenz_2_ortsangabe.ortsangabe_id=ortsangaben.id JOIN ortsangaben_flurstueckskennzeichen ON ortsangaben.id=ortsangaben_flurstueckskennzeichen.ortsangaben_id WHERE x=y ORDER BY 1,2,3 LIMIT 10 OFFSET 1")
            .add("SELECT fundorttiere.id AS SKEY, raumreferenz.datumabgleich FROM fundorttiere JOIN osirisobjekt ON fundorttiere.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id WHERE x=y ORDER BY 1 LIMIT 10 OFFSET 1")
            .add("SELECT fundorttiere.id AS SKEY, raumreferenz.id AS SKEY_1, raumreferenz_2_fachreferenz.objektart, raumreferenz_2_fachreferenz.fachreferenz_id FROM fundorttiere JOIN osirisobjekt ON fundorttiere.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id JOIN raumreferenz_2_fachreferenz ON raumreferenz.id=raumreferenz_2_fachreferenz.raumreferenz_id WHERE x=y ORDER BY 1,2 LIMIT 10 OFFSET 1")
            .add("SELECT fundorttiere.id AS SKEY, artbeobachtung.anzahl, artbeobachtung.begehungsmethode, artbeobachtung.bemerkunginformationsquelle, artbeobachtung.beobachtetam, artbeobachtung.haeufigkeit, artbeobachtung.informationsquelle, artbeobachtung.letzteskartierdatum, artbeobachtung.unschaerfe, artbeobachtung.bemerkungort FROM fundorttiere JOIN artbeobachtung ON fundorttiere.id=artbeobachtung.id WHERE x=y ORDER BY 1 LIMIT 10 OFFSET 1")
            .add("SELECT fundorttiere.id AS SKEY, erfasser.name, erfasser.bemerkung FROM fundorttiere JOIN artbeobachtung ON fundorttiere.id=artbeobachtung.id JOIN artbeobachtung_2_erfasser ON artbeobachtung.id=artbeobachtung_2_erfasser.artbeobachtung_id JOIN erfasser ON artbeobachtung_2_erfasser.erfasser_id=erfasser.id WHERE x=y ORDER BY 1 LIMIT 10 OFFSET 1")
            .add("SELECT fundorttiere.id AS SKEY, ST_AsText(ST_ForcePolygonCCW(geom.geom)) AS geom FROM fundorttiere JOIN artbeobachtung ON fundorttiere.id=artbeobachtung.id JOIN geom ON artbeobachtung.geom=geom.id WHERE x=y ORDER BY 1 LIMIT 10 OFFSET 1")
            .add("SELECT fundorttiere.id AS SKEY, fundorttiere.tierart, fundorttiere.bemerkungtierart, fundorttiere.vorkommen, fundorttiere.nachweis, fundorttiere.erfassungsmethode, fundorttiere.bemerkungpopulationsstadium FROM fundorttiere WHERE x=y ORDER BY 1 LIMIT 10 OFFSET 1")
            .build();


}