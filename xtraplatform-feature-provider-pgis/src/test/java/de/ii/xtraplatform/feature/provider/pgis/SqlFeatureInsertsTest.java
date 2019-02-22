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
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * @author zahnen
 */
public class SqlFeatureInsertsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFeatureInsertsTest.class);

    @Test
    public void test2() {
        SqlPathTree queries = new SqlPathTree.Builder()
                .fromPaths("/fundorttiere/[id=id]osirisobjekt/id", "/fundorttiere/[id=id]osirisobjekt/kennung", "/fundorttiere/[id=id]osirisobjekt/bezeichnung", "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam", "/fundorttiere/[id=id]osirisobjekt/verantwortlichestelle", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id", "/fundorttiere/[id=id]osirisobjekt/bemerkung", "/fundorttiere/[id=id]artbeobachtung/anzahl", "/fundorttiere/[id=id]artbeobachtung/begehungsmethode", "/fundorttiere/[id=id]artbeobachtung/bemerkunginformationsquelle", "/fundorttiere/[id=id]artbeobachtung/beobachtetam", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "/fundorttiere/[id=id]artbeobachtung/haeufigkeit", "/fundorttiere/[id=id]artbeobachtung/informationsquelle", "/fundorttiere/[id=id]artbeobachtung/letzteskartierdatum", "/fundorttiere/[id=id]artbeobachtung/unschaerfe", "/fundorttiere/[id=id]artbeobachtung/bemerkungort", "/fundorttiere/tierart", "/fundorttiere/bemerkungtierart", "/fundorttiere/vorkommen", "/fundorttiere/nachweis", "/fundorttiere/erfassungsmethode", "/fundorttiere/bemerkungpopulationsstadium", "/fundorttiere")
                .build();

        boolean stop = true;
    }

    @Test
    public void test() {
        SqlPathTree sqlPathTree = new SqlPathTree.Builder()
                .fromPaths("/fundorttiere/[id=id]osirisobjekt/id", "/fundorttiere/[id=id]osirisobjekt/kennung", "/fundorttiere/[id=id]osirisobjekt/bezeichnung", "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam", "/fundorttiere/[id=id]osirisobjekt/verantwortlichestelle", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id", "/fundorttiere/[id=id]osirisobjekt/bemerkung", "/fundorttiere/[id=id]artbeobachtung/anzahl", "/fundorttiere/[id=id]artbeobachtung/begehungsmethode", "/fundorttiere/[id=id]artbeobachtung/bemerkunginformationsquelle", "/fundorttiere/[id=id]artbeobachtung/beobachtetam", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "/fundorttiere/[id=id]artbeobachtung/haeufigkeit", "/fundorttiere/[id=id]artbeobachtung/informationsquelle", "/fundorttiere/[id=id]artbeobachtung/letzteskartierdatum", "/fundorttiere/[id=id]artbeobachtung/unschaerfe", "/fundorttiere/[id=id]artbeobachtung/bemerkungort", "/fundorttiere/tierart", "/fundorttiere/bemerkungtierart", "/fundorttiere/vorkommen", "/fundorttiere/nachweis", "/fundorttiere/erfassungsmethode", "/fundorttiere/bemerkungpopulationsstadium", "/fundorttiere")
                .build();

        ImmutableSqlFeatureInserts inserts = ImmutableSqlFeatureInserts.builder()
                                                                       .sqlPaths(sqlPathTree)
                                                                       //.addPaths("/fundorttiere/[id=id]osirisobjekt/id", "/fundorttiere/[id=id]osirisobjekt/kennung", "/fundorttiere/[id=id]osirisobjekt/bezeichnung", "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam", "/fundorttiere/[id=id]osirisobjekt/verantwortlichestelle", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id", "/fundorttiere/[id=id]osirisobjekt/bemerkung", "/fundorttiere/[id=id]artbeobachtung/anzahl", "/fundorttiere/[id=id]artbeobachtung/begehungsmethode", "/fundorttiere/[id=id]artbeobachtung/bemerkunginformationsquelle", "/fundorttiere/[id=id]artbeobachtung/beobachtetam", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "/fundorttiere/[id=id]artbeobachtung/haeufigkeit", "/fundorttiere/[id=id]artbeobachtung/informationsquelle", "/fundorttiere/[id=id]artbeobachtung/letzteskartierdatum", "/fundorttiere/[id=id]artbeobachtung/unschaerfe", "/fundorttiere/[id=id]artbeobachtung/bemerkungort", "/fundorttiere/tierart", "/fundorttiere/bemerkungtierart", "/fundorttiere/vorkommen", "/fundorttiere/nachweis", "/fundorttiere/erfassungsmethode", "/fundorttiere/bemerkungpopulationsstadium", "/fundorttiere")
                                                                       .build();

        List<String> queries = inserts.getQueries(ROWS)
                                      .stream()
                                      .map(query -> query.apply(VALUES2)
                                                         .first())
                                      .collect(Collectors.toList());

        LOGGER.debug("SQL \n{}", Joiner.on('\n')
                                       .join(queries));

        assertEquals(queries, EXPECTED);
    }

    @Test
    public void testWithId() {
        SqlPathTree sqlPathTree = new SqlPathTree.Builder()
                .fromPaths("/fundorttiere/[id=id]osirisobjekt/id", "/fundorttiere/[id=id]osirisobjekt/kennung", "/fundorttiere/[id=id]osirisobjekt/bezeichnung", "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam", "/fundorttiere/[id=id]osirisobjekt/verantwortlichestelle", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id", "/fundorttiere/[id=id]osirisobjekt/bemerkung", "/fundorttiere/[id=id]artbeobachtung/anzahl", "/fundorttiere/[id=id]artbeobachtung/begehungsmethode", "/fundorttiere/[id=id]artbeobachtung/bemerkunginformationsquelle", "/fundorttiere/[id=id]artbeobachtung/beobachtetam", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "/fundorttiere/[id=id]artbeobachtung/haeufigkeit", "/fundorttiere/[id=id]artbeobachtung/informationsquelle", "/fundorttiere/[id=id]artbeobachtung/letzteskartierdatum", "/fundorttiere/[id=id]artbeobachtung/unschaerfe", "/fundorttiere/[id=id]artbeobachtung/bemerkungort", "/fundorttiere/tierart", "/fundorttiere/bemerkungtierart", "/fundorttiere/vorkommen", "/fundorttiere/nachweis", "/fundorttiere/erfassungsmethode", "/fundorttiere/bemerkungpopulationsstadium", "/fundorttiere")
                .build();

        ImmutableSqlFeatureInserts inserts = ImmutableSqlFeatureInserts.builder()
                                                                       .withId(true)
                                                                       .sqlPaths(sqlPathTree)
                                                                       //.addPaths("/fundorttiere/[id=id]osirisobjekt/id", "/fundorttiere/[id=id]osirisobjekt/kennung", "/fundorttiere/[id=id]osirisobjekt/bezeichnung", "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam", "/fundorttiere/[id=id]osirisobjekt/verantwortlichestelle", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id", "/fundorttiere/[id=id]osirisobjekt/bemerkung", "/fundorttiere/[id=id]artbeobachtung/anzahl", "/fundorttiere/[id=id]artbeobachtung/begehungsmethode", "/fundorttiere/[id=id]artbeobachtung/bemerkunginformationsquelle", "/fundorttiere/[id=id]artbeobachtung/beobachtetam", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "/fundorttiere/[id=id]artbeobachtung/haeufigkeit", "/fundorttiere/[id=id]artbeobachtung/informationsquelle", "/fundorttiere/[id=id]artbeobachtung/letzteskartierdatum", "/fundorttiere/[id=id]artbeobachtung/unschaerfe", "/fundorttiere/[id=id]artbeobachtung/bemerkungort", "/fundorttiere/tierart", "/fundorttiere/bemerkungtierart", "/fundorttiere/vorkommen", "/fundorttiere/nachweis", "/fundorttiere/erfassungsmethode", "/fundorttiere/bemerkungpopulationsstadium", "/fundorttiere")
                                                                       .build();

        List<String> queries = inserts.getQueries(ROWS)
                                      .stream()
                                      .map(query -> query.apply(VALUES2)
                                                         .first())
                                      .collect(Collectors.toList());

        LOGGER.debug("SQL \n{}", Joiner.on('\n')
                                       .join(queries));

        assertEquals(queries, EXPECTED_WITH_ID);
    }

    static final List<String> EXPECTED = ImmutableList.<String>builder()
            .add("INSERT INTO osirisobjekt (kennung,bezeichnung,veroeffentlichtam,verantwortlichestelle,bemerkung) VALUES (1,2,3,4,5) RETURNING id;")
            .add("INSERT INTO foto (fotoverweis,hauptfoto,aufnahmezeitpunkt,bemerkung) VALUES (6,7,8,9) RETURNING id;")
            .add("INSERT INTO osirisobjekt_2_foto (osirisobjekt_id,foto_id) VALUES (100,200) RETURNING null;")
            .add("INSERT INTO foto (fotoverweis,hauptfoto,aufnahmezeitpunkt,bemerkung) VALUES (60,70,80,90) RETURNING id;")
            .add("INSERT INTO osirisobjekt_2_foto (osirisobjekt_id,foto_id) VALUES (100,201) RETURNING null;")
            .add("INSERT INTO raumreferenz (datumabgleich) VALUES (10) RETURNING id;")
            .add("INSERT INTO osirisobjekt_2_raumreferenz (osirisobjekt_id,raumreferenz_id) VALUES (100,300) RETURNING null;")
            .add("INSERT INTO ortsangaben (kreisschluessel,verbandsgemeindeschluessel,gemeindeschluessel) VALUES (11,12,13) RETURNING id;")
            .add("INSERT INTO raumreferenz_2_ortsangabe (raumreferenz_id,ortsangabe_id) VALUES (300,400) RETURNING null;")
            .add("INSERT INTO ortsangaben_flurstueckskennzeichen (ortsangaben_id,flurstueckskennzeichen) VALUES (400,14) RETURNING null;")
            .add("INSERT INTO ortsangaben (kreisschluessel,verbandsgemeindeschluessel,gemeindeschluessel) VALUES (36,37,38) RETURNING id;")
            .add("INSERT INTO raumreferenz_2_ortsangabe (raumreferenz_id,ortsangabe_id) VALUES (300,900) RETURNING null;")
            .add("INSERT INTO ortsangaben_flurstueckskennzeichen (ortsangaben_id,flurstueckskennzeichen) VALUES (900,39) RETURNING null;")
            .add("INSERT INTO ortsangaben_flurstueckskennzeichen (ortsangaben_id,flurstueckskennzeichen) VALUES (900,40) RETURNING null;")
            .add("INSERT INTO raumreferenz_2_fachreferenz (raumreferenz_id,objektart,fachreferenz_id) VALUES (300,fundorttiere,101) RETURNING null;")
            .add("INSERT INTO artbeobachtung (id,anzahl,begehungsmethode,bemerkunginformationsquelle,beobachtetam,haeufigkeit,informationsquelle,letzteskartierdatum,unschaerfe,bemerkungort) VALUES (100,22,23,24,25,26,27,28,29,30) RETURNING id;")
            .add("INSERT INTO geom (geom) VALUES (ST_ForcePolygonCW(ST_GeomFromText(31,25832))) RETURNING id;")
            .add("UPDATE artbeobachtung SET geom=800 WHERE id=100 RETURNING null;")
            .add("INSERT INTO erfasser (name,bemerkung) VALUES (32,33) RETURNING id;")
            .add("INSERT INTO artbeobachtung_2_erfasser (artbeobachtung_id,erfasser_id) VALUES (100,600) RETURNING null;")
            .add("INSERT INTO erfasser (name,bemerkung) VALUES (34,35) RETURNING id;")
            .add("INSERT INTO artbeobachtung_2_erfasser (artbeobachtung_id,erfasser_id) VALUES (100,700) RETURNING null;")
            .add("INSERT INTO fundorttiere (id,tierart,bemerkungtierart,vorkommen,nachweis,erfassungsmethode,bemerkungpopulationsstadium) VALUES (100,16,17,18,19,20,21) RETURNING id;")
            .build();

    static final List<String> EXPECTED_WITH_ID = ImmutableList.<String>builder()
            .add("INSERT INTO osirisobjekt (id,kennung,bezeichnung,veroeffentlichtam,verantwortlichestelle,bemerkung) VALUES (100,1,2,3,4,5) RETURNING id;")
            .add("INSERT INTO foto (fotoverweis,hauptfoto,aufnahmezeitpunkt,bemerkung) VALUES (6,7,8,9) RETURNING id;")
            .add("INSERT INTO osirisobjekt_2_foto (osirisobjekt_id,foto_id) VALUES (100,200) RETURNING null;")
            .add("INSERT INTO foto (fotoverweis,hauptfoto,aufnahmezeitpunkt,bemerkung) VALUES (60,70,80,90) RETURNING id;")
            .add("INSERT INTO osirisobjekt_2_foto (osirisobjekt_id,foto_id) VALUES (100,201) RETURNING null;")
            .add("INSERT INTO raumreferenz (datumabgleich) VALUES (10) RETURNING id;")
            .add("INSERT INTO osirisobjekt_2_raumreferenz (osirisobjekt_id,raumreferenz_id) VALUES (100,300) RETURNING null;")
            .add("INSERT INTO ortsangaben (kreisschluessel,verbandsgemeindeschluessel,gemeindeschluessel) VALUES (11,12,13) RETURNING id;")
            .add("INSERT INTO raumreferenz_2_ortsangabe (raumreferenz_id,ortsangabe_id) VALUES (300,400) RETURNING null;")
            .add("INSERT INTO ortsangaben_flurstueckskennzeichen (ortsangaben_id,flurstueckskennzeichen) VALUES (400,14) RETURNING null;")
            .add("INSERT INTO ortsangaben (kreisschluessel,verbandsgemeindeschluessel,gemeindeschluessel) VALUES (36,37,38) RETURNING id;")
            .add("INSERT INTO raumreferenz_2_ortsangabe (raumreferenz_id,ortsangabe_id) VALUES (300,900) RETURNING null;")
            .add("INSERT INTO ortsangaben_flurstueckskennzeichen (ortsangaben_id,flurstueckskennzeichen) VALUES (900,39) RETURNING null;")
            .add("INSERT INTO ortsangaben_flurstueckskennzeichen (ortsangaben_id,flurstueckskennzeichen) VALUES (900,40) RETURNING null;")
            .add("INSERT INTO raumreferenz_2_fachreferenz (raumreferenz_id,objektart,fachreferenz_id) VALUES (300,fundorttiere,101) RETURNING null;")
            .add("INSERT INTO artbeobachtung (id,anzahl,begehungsmethode,bemerkunginformationsquelle,beobachtetam,haeufigkeit,informationsquelle,letzteskartierdatum,unschaerfe,bemerkungort) VALUES (100,22,23,24,25,26,27,28,29,30) RETURNING id;")
            .add("INSERT INTO geom (geom) VALUES (ST_ForcePolygonCW(ST_GeomFromText(31,25832))) RETURNING id;")
            .add("UPDATE artbeobachtung SET geom=800 WHERE id=100 RETURNING null;")
            .add("INSERT INTO erfasser (name,bemerkung) VALUES (32,33) RETURNING id;")
            .add("INSERT INTO artbeobachtung_2_erfasser (artbeobachtung_id,erfasser_id) VALUES (100,600) RETURNING null;")
            .add("INSERT INTO erfasser (name,bemerkung) VALUES (34,35) RETURNING id;")
            .add("INSERT INTO artbeobachtung_2_erfasser (artbeobachtung_id,erfasser_id) VALUES (100,700) RETURNING null;")
            .add("INSERT INTO fundorttiere (id,tierart,bemerkungtierart,vorkommen,nachweis,erfassungsmethode,bemerkungpopulationsstadium) VALUES (100,16,17,18,19,20,21) RETURNING id;")
            .build();

    //TODO: fullPaths
    static final Map<String, List<Integer>> ROWS = ImmutableMap.<String, List<Integer>>builder()

            //.put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto", 1)
            //.put("/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz", 1)


            //.put("raumreferenz", ImmutableList.of(2))
            //.put("ortsangaben", ImmutableList.of(1, 2))
            //1,1 2.1 2.2
            //.put("ortsangaben_flurstueckskennzeichen", ImmutableList.of(3, 1, 2))
            // ImmutableList.of(ImmutableList.of(1, 1, 3), ImmutableList.of(2, 1, 1), ImmutableList.of(2, 2, 2))


            .put("foto", ImmutableList.of(2))
            .put("raumreferenz", ImmutableList.of(1))

            .put("raumreferenz_2_fachreferenz", ImmutableList.of(1))

            .put("ortsangaben", ImmutableList.of(2))
            .put("ortsangaben_flurstueckskennzeichen", ImmutableList.of(1, 2))

            .put("erfasser", ImmutableList.of(2))

            .build();

    //inserts.getValueContainer(ROWS)
    static final NestedSqlInsertRow VALUES2 = new NestedSqlInsertRow("/fundorttiere",
            ImmutableMap.<String, String>builder()
                    .put("/fundorttiere/tierart", "16")
                    .put("/fundorttiere/bemerkungtierart", "17")
                    .put("/fundorttiere/vorkommen", "18")
                    .put("/fundorttiere/nachweis", "19")
                    .put("/fundorttiere/erfassungsmethode", "20")
                    .put("/fundorttiere/bemerkungpopulationsstadium", "21")
                    .build(),
            ImmutableListMultimap.<String, NestedSqlInsertRow>builder()
                    .put("/[id=id]osirisobjekt", new NestedSqlInsertRow("/[id=id]osirisobjekt",
                            ImmutableMap.<String, String>builder()
                                    .put("/[id=id]osirisobjekt/id", "100")
                                    .put("/[id=id]osirisobjekt/kennung", "1")
                                    .put("/[id=id]osirisobjekt/bezeichnung", "2")
                                    .put("/[id=id]osirisobjekt/veroeffentlichtam", "3")
                                    .put("/[id=id]osirisobjekt/verantwortlichestelle", "4")
                                    .put("/[id=id]osirisobjekt/bemerkung", "5")
                                    .build(),
                            ImmutableListMultimap.<String, NestedSqlInsertRow>builder()
                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto", new NestedSqlInsertRow("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto",
                                            ImmutableMap.<String, String>builder()
                                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "6")
                                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "7")
                                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "8")
                                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "9")
                                                    .build(),
                                            ImmutableListMultimap.of(), ImmutableMap.of("foto.id", "200")))
                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto", new NestedSqlInsertRow("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto",
                                            ImmutableMap.<String, String>builder()
                                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "60")
                                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "70")
                                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "80")
                                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "90")
                                                    .build(),
                                            ImmutableListMultimap.of(), ImmutableMap.of("foto.id", "201")))
                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz", new NestedSqlInsertRow("/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz",
                                            ImmutableMap.<String, String>builder()
                                                    .put("/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "10")
                                                    .build(),
                                            ImmutableListMultimap.<String, NestedSqlInsertRow>builder()
                                                    .put("/[id=raumreferenz_id]raumreferenz_2_fachreferenz", new NestedSqlInsertRow("/[id=raumreferenz_id]raumreferenz_2_fachreferenz",
                                                            ImmutableMap.<String, String>builder()
                                                                    .put("/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart", "fundorttiere")
                                                                    .put("/[id=raumreferenz_id]raumreferenz_2_fachreferenz/fachreferenz_id", "101")
                                                                    .build(),
                                                            ImmutableListMultimap.of(), ImmutableMap.of()))
                                                    .put("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben", new NestedSqlInsertRow("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben",
                                                            ImmutableMap.<String, String>builder()
                                                                    .put("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "11")
                                                                    .put("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "12")
                                                                    .put("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "13")
                                                                    .build(),
                                                            ImmutableListMultimap.<String, NestedSqlInsertRow>builder()
                                                                    .put("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen", new NestedSqlInsertRow("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen",
                                                                            ImmutableMap.<String, String>builder()
                                                                                    .put("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "14")
                                                                                    .build(),
                                                                            ImmutableListMultimap.of(), ImmutableMap.of()))
                                                                    .build(), ImmutableMap.of("ortsangaben.id", "400")))
                                                    .put("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben", new NestedSqlInsertRow("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben",
                                                            ImmutableMap.<String, String>builder()
                                                                    .put("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "36")
                                                                    .put("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "37")
                                                                    .put("/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "38")
                                                                    .build(),
                                                            ImmutableListMultimap.<String, NestedSqlInsertRow>builder()
                                                                    .put("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen", new NestedSqlInsertRow("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen",
                                                                            ImmutableMap.<String, String>builder()
                                                                                    .put("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "39")
                                                                                    .build(),
                                                                            ImmutableListMultimap.of(), ImmutableMap.of()))
                                                                    .put("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen", new NestedSqlInsertRow("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen",
                                                                            ImmutableMap.<String, String>builder()
                                                                                    .put("/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "40")
                                                                                    .build(),
                                                                            ImmutableListMultimap.of(), ImmutableMap.of()))
                                                                    .build(), ImmutableMap.of("ortsangaben.id", "900")))
                                                    .build(), ImmutableMap.of("raumreferenz.id", "300")))
                                    .build(), ImmutableMap.of("osirisobjekt.id", "100")))
                    .put("/[id=id]artbeobachtung", new NestedSqlInsertRow("/[id=id]artbeobachtung",
                            ImmutableMap.<String, String>builder()
                                    .put("/[id=id]artbeobachtung/anzahl", "22")
                                    .put("/[id=id]artbeobachtung/begehungsmethode", "23")
                                    .put("/[id=id]artbeobachtung/bemerkunginformationsquelle", "24")
                                    .put("/[id=id]artbeobachtung/beobachtetam", "25")
                                    .put("/[id=id]artbeobachtung/haeufigkeit", "26")
                                    .put("/[id=id]artbeobachtung/informationsquelle", "27")
                                    .put("/[id=id]artbeobachtung/letzteskartierdatum", "28")
                                    .put("/[id=id]artbeobachtung/unschaerfe", "29")
                                    .put("/[id=id]artbeobachtung/bemerkungort", "30")
                                    .build(),
                            ImmutableListMultimap.<String, NestedSqlInsertRow>builder()
                                    .put("/[geom=id]geom", new NestedSqlInsertRow("/[geom=id]geom",
                                            ImmutableMap.<String, String>builder()
                                                    .put("/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "31")
                                                    .build(),
                                            ImmutableListMultimap.of(), ImmutableMap.of("geom.id", "800")))
                                    .put("/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser", new NestedSqlInsertRow("/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser",
                                            ImmutableMap.<String, String>builder()
                                                    .put("/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "32")
                                                    .put("/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "33")
                                                    .build(),
                                            ImmutableListMultimap.of(), ImmutableMap.of("erfasser.id", "600")))
                                    .put("/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser", new NestedSqlInsertRow("/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser",
                                            ImmutableMap.<String, String>builder()
                                                    .put("/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "34")
                                                    .put("/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "35")
                                                    .build(),
                                            ImmutableListMultimap.of(), ImmutableMap.of("erfasser.id", "700")))
                                    .build(), ImmutableMap.of("artbeobachtung.id", "100")))
                    .build(), ImmutableMap.of("fundorttiere.id", "100"));

    //TODO: fullPaths
    static final ListMultimap<String, String> VALUES = ImmutableListMultimap.<String, String>builder()


            //TODO


            .build();
}