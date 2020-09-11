/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zahnen
 */
public class SqlFeatureCreatorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFeatureCreatorTest.class);

    static ActorSystem system;
    static ActorMaterializer materializer;

    @BeforeClass(groups = {"default"})
    public static void setup() {
        system = ActorSystem.create();
        materializer = ActorMaterializer.create(system);
    }

    @AfterClass(groups = {"default"})
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void test3() {
        Matcher matcher = Pattern.compile("(?<serviceUrl>.+)\\/collections\\/(?<objektart>.+)\\/items\\/(?<fachreferenzxyzid>.+)").matcher("http://localhost:7080/rest/services/oneo/collections/fundorttiere/items/74");
        boolean matches = matcher.matches();
        String i = matcher.group("objektart");
        String j = matcher.group("fachreferenzxyzid");
        boolean stop = true;
    }

    @Test
    public void test2() {
        ImmutableSqlFeatureInserts inserts = ImmutableSqlFeatureInserts.builder()
                                                                       //.addPaths("/fundorttiere/[id=id]osirisobjekt/id", "/fundorttiere/[id=id]osirisobjekt/kennung", "/fundorttiere/[id=id]osirisobjekt/bezeichnung", "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam", "/fundorttiere/[id=id]osirisobjekt/verantwortlichestelle", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id", "/fundorttiere/[id=id]osirisobjekt/bemerkung", "/fundorttiere/[id=id]artbeobachtung/anzahl", "/fundorttiere/[id=id]artbeobachtung/begehungsmethode", "/fundorttiere/[id=id]artbeobachtung/bemerkunginformationsquelle", "/fundorttiere/[id=id]artbeobachtung/beobachtetam", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "/fundorttiere/[id=id]artbeobachtung/haeufigkeit", "/fundorttiere/[id=id]artbeobachtung/informationsquelle", "/fundorttiere/[id=id]artbeobachtung/letzteskartierdatum", "/fundorttiere/[id=id]artbeobachtung/unschaerfe", "/fundorttiere/[id=id]artbeobachtung/bemerkungort", "/fundorttiere/tierart", "/fundorttiere/bemerkungtierart", "/fundorttiere/vorkommen", "/fundorttiere/nachweis", "/fundorttiere/erfassungsmethode", "/fundorttiere/bemerkungpopulationsstadium", "/fundorttiere")
                                                                       .build();

        NestedSqlInsertRow valueContainer = inserts.getValueContainer(ImmutableMap.of()/*ImmutableMap.of("foto", ImmutableList.of(2), "ortsangaben", ImmutableList.of(3), "ortsangaben_flurstueckskennzeichen", ImmutableList.of(2,0,1))*/);

        boolean bla = false;
    }

    @Test
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
        new TestKit(system) {
            {
                TestKit probe = new TestKit(system);
                ActorMaterializer materializer = ActorMaterializer.create(probe.getSystem());

                Config config = ConfigFactory.parseString("{\n" +
                        "  profile = \"slick.jdbc.PostgresProfile$\"\n" +
                        "  db {\n" +
                        "    user = \"postgres\"\n" +
                        "    password = \"postgres\"\n" +
                        "    url = \"jdbc:postgresql://localhost/artendaten\"\n" +
                        "  }\n" +
                        "}");

                SlickSession session = SlickSession.forConfig(config);
                system.registerOnTermination(session::close);

                SqlFeatureCreator sqlFeatureCreator = getFeatureCreator(session, materializer);

                addRows(sqlFeatureCreator);

                CompletionStage<String> completionStage = sqlFeatureCreator.runQueries();

                String id = completionStage.toCompletableFuture()
                             .get(10, TimeUnit.SECONDS);

                LOGGER.debug("FEATURE ID {}", id);


            }
        };
    }

    //TODO: add crs in FeatureTransformer
    private void addRows(SqlFeatureCreator sqlFeatureCreator) {
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/kennung", "1");
        sqlFeatureCreator.property("/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "MULTIPOINT(327049.3625 5536335.4475)");
        sqlFeatureCreator.property("/fundorttiere/tierart", "17");
        sqlFeatureCreator.property("/fundorttiere/[id=id]artbeobachtung/bemerkungort", "da und da");
        sqlFeatureCreator.property("/fundorttiere/[id=id]artbeobachtung/anzahl", "5");

        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto", ImmutableList.of(1));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "t");

        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto", ImmutableList.of(2));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "f");

        sqlFeatureCreator.row("/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser", ImmutableList.of(1));
        sqlFeatureCreator.property("/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "AZ");

        sqlFeatureCreator.row("/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser", ImmutableList.of(2));
        sqlFeatureCreator.property("/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "ZA");

        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz", ImmutableList.of(1));

        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz", ImmutableList.of(1,1));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart", "fundorttiere");
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/fachreferenz_id", "73");

        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben", ImmutableList.of(1,1));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "11");

        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen", ImmutableList.of(1,1,1));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "33");
        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen", ImmutableList.of(1,1,2));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "34");

        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben", ImmutableList.of(1,2));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "12");

        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen", ImmutableList.of(1,2,1));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "35");
        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen", ImmutableList.of(1,2,2));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "36");
        sqlFeatureCreator.row("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen", ImmutableList.of(1,2,3));
        sqlFeatureCreator.property("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "37");

    }

    private SqlFeatureCreator getFeatureCreator(SlickSession session, ActorMaterializer materializer) {
        ImmutableSqlFeatureInserts inserts = ImmutableSqlFeatureInserts.builder()
                                                                       .sqlPaths(new SqlPathTree.Builder().fromPaths("/fundorttiere/[id=id]osirisobjekt/id", "/fundorttiere/[id=id]osirisobjekt/kennung", "/fundorttiere/[id=id]osirisobjekt/bezeichnung", "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam", "/fundorttiere/[id=id]osirisobjekt/verantwortlichestelle", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/hauptfoto", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/aufnahmezeitpunkt", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/bemerkung", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/verbandsgemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/gemeindeschluessel", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich", "/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id", "/fundorttiere/[id=id]osirisobjekt/bemerkung", "/fundorttiere/[id=id]artbeobachtung/anzahl", "/fundorttiere/[id=id]artbeobachtung/begehungsmethode", "/fundorttiere/[id=id]artbeobachtung/bemerkunginformationsquelle", "/fundorttiere/[id=id]artbeobachtung/beobachtetam", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name", "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/bemerkung", "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/ST_AsText(ST_ForcePolygonCCW(geom))", "/fundorttiere/[id=id]artbeobachtung/haeufigkeit", "/fundorttiere/[id=id]artbeobachtung/informationsquelle", "/fundorttiere/[id=id]artbeobachtung/letzteskartierdatum", "/fundorttiere/[id=id]artbeobachtung/unschaerfe", "/fundorttiere/[id=id]artbeobachtung/bemerkungort", "/fundorttiere/tierart", "/fundorttiere/bemerkungtierart", "/fundorttiere/vorkommen", "/fundorttiere/nachweis", "/fundorttiere/erfassungsmethode", "/fundorttiere/bemerkungpopulationsstadium", "/fundorttiere").build())
                                                                       .build();

        return new SqlFeatureCreator(session, materializer, inserts);
    }

}