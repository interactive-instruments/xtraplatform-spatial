package de.ii.xtraplatform.feature.provider.sql.infra.db

import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax
import de.ii.xtraplatform.feature.provider.sql.SqlMappingParser
import de.ii.xtraplatform.feature.provider.sql.app.FeatureStorePathParser
import de.ii.xtraplatform.feature.provider.sql.domain.FilterEncoderSql
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureStoreInstanceContainer
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableMetaQueryResult
import de.ii.xtraplatform.feature.transformer.api.MappingTestUtil
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Paths
import java.util.stream.Collectors

class FeatureStoreQueryGeneratorSqlSpec extends Specification {

    @Shared
            queryGenerator
    @Shared
            featureTypeToTest = "biotop";
    @Shared
            sortKey = "id";
    @Shared
            instanceContainer


    def setupSpec() {
        def serviceFile = Paths.get("/home/zahnen/development/ldproxy-pgis/build/data/store/entities/services/oneo_0619");

        def featureTypeMappings = MappingTestUtil.readFeatureTypeMappings(serviceFile);

        FilterEncoderSql filterEncoder = { filter -> "_FILTER_" };

        queryGenerator = new FeatureStoreQueryGeneratorSql(filterEncoder);

        def syntax = ImmutableSqlPathSyntax.builder().build()

        def mappingParser = new SqlMappingParser(syntax)

        def sqlPaths = mappingParser.parse(featureTypeMappings.get(featureTypeToTest).getMappings())

        def pathParser = new FeatureStorePathParser(syntax)

        instanceContainer = pathParser.parse(sqlPaths).get(0)
    }

    def 'meta query'() {

        given: "a main table "

        ImmutableFeatureStoreInstanceContainer instance = ImmutableFeatureStoreInstanceContainer.builder()
                .name(featureTypeToTest)
                .path(["/$featureTypeToTest"])
                .sortKey(sortKey)
                .build()

        when: "query is generated"

        def actual = queryGenerator.getMetaQuery(instance, limit, offset, filter, computeNumberMatched);

        then:
        actual == expected

        where:
        limit | offset | filter | computeNumberMatched || expected
        0     | 0      | null   | false                || String.format('SELECT *,-1 FROM (SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2, count(*) AS col3 FROM (SELECT %1$s AS SKEY FROM %2$s ORDER BY 1) AS A) AS B', sortKey, featureTypeToTest)
        10    | 0      | null   | false                || String.format('SELECT *,-1 FROM (SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2, count(*) AS col3 FROM (SELECT %1$s AS SKEY FROM %2$s ORDER BY 1 LIMIT 10) AS A) AS B', sortKey, featureTypeToTest)
        0     | 10     | null   | false                || String.format('SELECT *,-1 FROM (SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2, count(*) AS col3 FROM (SELECT %1$s AS SKEY FROM %2$s ORDER BY 1 OFFSET 10) AS A) AS B', sortKey, featureTypeToTest)
        10    | 10     | null   | false                || String.format('SELECT *,-1 FROM (SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2, count(*) AS col3 FROM (SELECT %1$s AS SKEY FROM %2$s ORDER BY 1 LIMIT 10 OFFSET 10) AS A) AS B', sortKey, featureTypeToTest)
        0     | 0      | null   | true                 || String.format('SELECT * FROM (SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2, count(*) AS col3 FROM (SELECT %1$s AS SKEY FROM %2$s ORDER BY 1) AS A) AS C, (SELECT count(*) AS col4 FROM (SELECT %1$s AS SKEY FROM %2$s ORDER BY 1) AS B) AS D', sortKey, featureTypeToTest)
        0     | 0      | "ignr" | false                || String.format('SELECT *,-1 FROM (SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2, count(*) AS col3 FROM (SELECT %1$s AS SKEY FROM %2$s WHERE _FILTER_ ORDER BY 1) AS A) AS B', sortKey, featureTypeToTest)
        0     | 0      | "ignr" | true                 || String.format('SELECT * FROM (SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2, count(*) AS col3 FROM (SELECT %1$s AS SKEY FROM %2$s WHERE _FILTER_ ORDER BY 1) AS A) AS C, (SELECT count(*) AS col4 FROM (SELECT %1$s AS SKEY FROM %2$s WHERE _FILTER_ ORDER BY 1) AS B) AS D', sortKey, featureTypeToTest)
        10    | 0      | "ignr" | true                 || String.format('SELECT * FROM (SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2, count(*) AS col3 FROM (SELECT %1$s AS SKEY FROM %2$s WHERE _FILTER_ ORDER BY 1 LIMIT 10) AS A) AS C, (SELECT count(*) AS col4 FROM (SELECT %1$s AS SKEY FROM %2$s WHERE _FILTER_ ORDER BY 1) AS B) AS D', sortKey, featureTypeToTest)

    }

    @Unroll
    def 'feature queries (#description)'() {

        given: "a type"

        def type = instanceContainer

        when:

        def actual = queryGenerator.getInstanceQueries(type, filter, minKey, maxKey).collect(Collectors.toList());

        then:

        actual == expected

        where:

        description  | minKey  | maxKey  | filter                   || expected
        'no filter'  | 2000000 | 2000009 | null                     || ['SELECT biotop.id AS SKEY, osirisobjekt.id AS SKEY_1, osirisobjekt.id, osirisobjekt.kennung, osirisobjekt.bezeichnung, osirisobjekt.veroeffentlichtam, osirisobjekt.verantwortlichestelle, osirisobjekt.bemerkung, osirisobjekt.lanis FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, foto.id AS SKEY_1, foto.fotoverweis, foto.hauptfoto, foto.aufnahmezeitpunkt, foto.bemerkung FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_foto ON osirisobjekt.id=osirisobjekt_2_foto.osirisobjekt_id JOIN foto ON osirisobjekt_2_foto.foto_id=foto.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, raumreferenz.id AS SKEY_1, ortsangaben.id AS SKEY_2, ortsangaben.kreisschluessel, ortsangaben.verbandsgemeindeschluessel, ortsangaben.gemeindeschluessel FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id JOIN raumreferenz_2_ortsangabe ON raumreferenz.id=raumreferenz_2_ortsangabe.raumreferenz_id JOIN ortsangaben ON raumreferenz_2_ortsangabe.ortsangabe_id=ortsangaben.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, raumreferenz.id AS SKEY_1, ortsangaben.id AS SKEY_2, ortsangaben_flurstueckskennzeichen.id AS SKEY_3, ortsangaben_flurstueckskennzeichen.flurstueckskennzeichen FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id JOIN raumreferenz_2_ortsangabe ON raumreferenz.id=raumreferenz_2_ortsangabe.raumreferenz_id JOIN ortsangaben ON raumreferenz_2_ortsangabe.ortsangabe_id=ortsangaben.id JOIN ortsangaben_flurstueckskennzeichen ON ortsangaben.id=ortsangaben_flurstueckskennzeichen.ortsangaben_id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2,3,4', 'SELECT biotop.id AS SKEY, raumreferenz.id AS SKEY_1, raumreferenz.datumabgleich FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, raumreferenz.id AS SKEY_1, raumreferenz_2_fachreferenz.fachreferenz_id AS SKEY_2, raumreferenz_2_fachreferenz.objektart, raumreferenz_2_fachreferenz.fachreferenz_id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id JOIN raumreferenz_2_fachreferenz ON raumreferenz.id=raumreferenz_2_fachreferenz.raumreferenz_id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, geom.id AS SKEY_1, ST_AsText(ST_ForcePolygonCCW(geom.geom)) AS geom FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN geom ON biotopangaben.geom=geom.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, biotoptyp.id AS SKEY_1, biotoptyp.typ, biotoptyp.bemerkungtyp FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_biotoptyp ON biotopangaben.id=biotopangaben_2_biotoptyp.biotopangaben_id JOIN biotoptyp ON biotopangaben_2_biotoptyp.biotoptyp_id=biotoptyp.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, biotoptyp.id AS SKEY_1, zcodebt.id AS SKEY_2, zcodebt.zusatzcode, zcodebt.bemerkung FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_biotoptyp ON biotopangaben.id=biotopangaben_2_biotoptyp.biotopangaben_id JOIN biotoptyp ON biotopangaben_2_biotoptyp.biotoptyp_id=biotoptyp.id JOIN biotoptyp_2_zusatzbezeichnung ON biotoptyp.id=biotoptyp_2_zusatzbezeichnung.biotoptyp_id JOIN zcodebt ON biotoptyp_2_zusatzbezeichnung.zusatzbezeichnung_id=zcodebt.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, inspirebiotopehabitate.id AS SKEY_1, inspirebiotopehabitate.inspireid FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_inspire ON biotopangaben.id=biotopangaben_2_inspire.biotopangaben_id JOIN inspirebiotopehabitate ON biotopangaben_2_inspire.inspire_id=inspirebiotopehabitate.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, inspirebiotopehabitate.id AS SKEY_1, habitattypecovertype.id AS SKEY_2, habitattypecovertype.referencehabitattypeid, habitattypecovertype.referenceeunistypeid, habitattypecovertype.referencehabitatscheme FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_inspire ON biotopangaben.id=biotopangaben_2_inspire.biotopangaben_id JOIN inspirebiotopehabitate ON biotopangaben_2_inspire.inspire_id=inspirebiotopehabitate.id JOIN inspirebiotopehabitate_2_habitat ON inspirebiotopehabitate.id=inspirebiotopehabitate_2_habitat.inspirebiotopehabitate_id JOIN habitattypecovertype ON inspirebiotopehabitate_2_habitat.habitat_id=habitattypecovertype.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, erfassung.id AS SKEY_1, erfassung.datum, erfassung.erfassungsart, erfassung.kampagne, erfassung.name, erfassung.bemerkung FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_erfassung ON biotopangaben.id=biotopangaben_2_erfassung.biotopangaben_id JOIN erfassung ON biotopangaben_2_erfassung.erfassung_id=erfassung.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, pflanzengesellschaft.id AS SKEY_1, pflanzengesellschaft.art, pflanzengesellschaft.deckungprozentual, pflanzengesellschaft.bemerkung FROM biotop JOIN biotop_2_pflanzengesellschaft ON biotop.id=biotop_2_pflanzengesellschaft.biotop_id JOIN pflanzengesellschaft ON biotop_2_pflanzengesellschaft.pflanzengesellschaft_id=pflanzengesellschaft.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, pflanzengesellschaft.id AS SKEY_1, schichtbt.id AS SKEY_2, schichtbt.art, schichtbt.deckungprozentual, schichtbt.hoeheinmeter, schichtbt.bemerkung FROM biotop JOIN biotop_2_pflanzengesellschaft ON biotop.id=biotop_2_pflanzengesellschaft.biotop_id JOIN pflanzengesellschaft ON biotop_2_pflanzengesellschaft.pflanzengesellschaft_id=pflanzengesellschaft.id JOIN pflanzengesellschaft_2_pflanzenschicht ON pflanzengesellschaft.id=pflanzengesellschaft_2_pflanzenschicht.pflanzengesellschaft_id JOIN schichtbt ON pflanzengesellschaft_2_pflanzenschicht.pflanzenschicht_id=schichtbt.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, pflanzengesellschaft.id AS SKEY_1, schichtbt.id AS SKEY_2, pflanzebt.id AS SKEY_3, pflanzebt.art, pflanzebt.anz, pflanzebt.haeufigkeit, pflanzebt.bemerkung FROM biotop JOIN biotop_2_pflanzengesellschaft ON biotop.id=biotop_2_pflanzengesellschaft.biotop_id JOIN pflanzengesellschaft ON biotop_2_pflanzengesellschaft.pflanzengesellschaft_id=pflanzengesellschaft.id JOIN pflanzengesellschaft_2_pflanzenschicht ON pflanzengesellschaft.id=pflanzengesellschaft_2_pflanzenschicht.pflanzengesellschaft_id JOIN schichtbt ON pflanzengesellschaft_2_pflanzenschicht.pflanzenschicht_id=schichtbt.id JOIN schichtbt_2_pflanzenart ON schichtbt.id=schichtbt_2_pflanzenart.schichtbt_id JOIN pflanzebt ON schichtbt_2_pflanzenart.pflanzenart_id=pflanzebt.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2,3,4', 'SELECT biotop.id AS SKEY, biotop.id AS SKEY_1, biotop.qs, biotop.status, biotop.bedeutung, biotop.bemerkungbedeutung, biotop.beeintraechtigung, biotop.bemerkungbeeintaechtigung, biotop.bemerkungschutzstatus, biotop.entwicklungstendenz, biotop.schutzstatus, biotop.bemerkungentwicklungstendenz, biotop.lebensraumtyp, biotop.gesetzlichgeschuetztesbt, biotop.erstelltvon, biotop.erstelltam, biotop.geaendertvon, biotop.geaendertam FROM biotop WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, erhaltungszustandsbewertung.id AS SKEY_1, erhaltungszustandsbewertung.erhaltungszustandartenkombination, erhaltungszustandsbewertung.bemerkungezartenkombination, erhaltungszustandsbewertung.erhaltungszustandstoerung, erhaltungszustandsbewertung.bemerkungezstoerung, erhaltungszustandsbewertung.erhaltungszustandstruktur, erhaltungszustandsbewertung.bemerkungezstruktur, erhaltungszustandsbewertung.erhaltungszustandgesamt, erhaltungszustandsbewertung.bemerkungezgesamt FROM biotop JOIN biotop_2_bewertungerhaltungszustand ON biotop.id=biotop_2_bewertungerhaltungszustand.biotop_id JOIN erhaltungszustandsbewertung ON biotop_2_bewertungerhaltungszustand.bewertungerhaltungszustand_id=erhaltungszustandsbewertung.id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, biotop_2_btkomplex.btkomplex_id AS SKEY_1, biotop_2_btkomplex.objektart, biotop_2_btkomplex.btkomplex_id FROM biotop JOIN biotop_2_btkomplex ON biotop.id=biotop_2_btkomplex.biotop_id WHERE (biotop.id >= 2000000 AND biotop.id <= 2000009) ORDER BY 1,2']
        'filter'     | 2000010 | 2000010 | 'bezeichnung=6108-2-23'  || ['SELECT biotop.id AS SKEY, osirisobjekt.id AS SKEY_1, osirisobjekt.id, osirisobjekt.kennung, osirisobjekt.bezeichnung, osirisobjekt.veroeffentlichtam, osirisobjekt.verantwortlichestelle, osirisobjekt.bemerkung, osirisobjekt.lanis FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, foto.id AS SKEY_1, foto.fotoverweis, foto.hauptfoto, foto.aufnahmezeitpunkt, foto.bemerkung FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_foto ON osirisobjekt.id=osirisobjekt_2_foto.osirisobjekt_id JOIN foto ON osirisobjekt_2_foto.foto_id=foto.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, raumreferenz.id AS SKEY_1, ortsangaben.id AS SKEY_2, ortsangaben.kreisschluessel, ortsangaben.verbandsgemeindeschluessel, ortsangaben.gemeindeschluessel FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id JOIN raumreferenz_2_ortsangabe ON raumreferenz.id=raumreferenz_2_ortsangabe.raumreferenz_id JOIN ortsangaben ON raumreferenz_2_ortsangabe.ortsangabe_id=ortsangaben.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, raumreferenz.id AS SKEY_1, ortsangaben.id AS SKEY_2, ortsangaben_flurstueckskennzeichen.id AS SKEY_3, ortsangaben_flurstueckskennzeichen.flurstueckskennzeichen FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id JOIN raumreferenz_2_ortsangabe ON raumreferenz.id=raumreferenz_2_ortsangabe.raumreferenz_id JOIN ortsangaben ON raumreferenz_2_ortsangabe.ortsangabe_id=ortsangaben.id JOIN ortsangaben_flurstueckskennzeichen ON ortsangaben.id=ortsangaben_flurstueckskennzeichen.ortsangaben_id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2,3,4', 'SELECT biotop.id AS SKEY, raumreferenz.id AS SKEY_1, raumreferenz.datumabgleich FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, raumreferenz.id AS SKEY_1, raumreferenz_2_fachreferenz.fachreferenz_id AS SKEY_2, raumreferenz_2_fachreferenz.objektart, raumreferenz_2_fachreferenz.fachreferenz_id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id JOIN osirisobjekt_2_raumreferenz ON osirisobjekt.id=osirisobjekt_2_raumreferenz.osirisobjekt_id JOIN raumreferenz ON osirisobjekt_2_raumreferenz.raumreferenz_id=raumreferenz.id JOIN raumreferenz_2_fachreferenz ON raumreferenz.id=raumreferenz_2_fachreferenz.raumreferenz_id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, geom.id AS SKEY_1, ST_AsText(ST_ForcePolygonCCW(geom.geom)) AS geom FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN geom ON biotopangaben.geom=geom.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, biotoptyp.id AS SKEY_1, biotoptyp.typ, biotoptyp.bemerkungtyp FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_biotoptyp ON biotopangaben.id=biotopangaben_2_biotoptyp.biotopangaben_id JOIN biotoptyp ON biotopangaben_2_biotoptyp.biotoptyp_id=biotoptyp.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, biotoptyp.id AS SKEY_1, zcodebt.id AS SKEY_2, zcodebt.zusatzcode, zcodebt.bemerkung FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_biotoptyp ON biotopangaben.id=biotopangaben_2_biotoptyp.biotopangaben_id JOIN biotoptyp ON biotopangaben_2_biotoptyp.biotoptyp_id=biotoptyp.id JOIN biotoptyp_2_zusatzbezeichnung ON biotoptyp.id=biotoptyp_2_zusatzbezeichnung.biotoptyp_id JOIN zcodebt ON biotoptyp_2_zusatzbezeichnung.zusatzbezeichnung_id=zcodebt.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, inspirebiotopehabitate.id AS SKEY_1, inspirebiotopehabitate.inspireid FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_inspire ON biotopangaben.id=biotopangaben_2_inspire.biotopangaben_id JOIN inspirebiotopehabitate ON biotopangaben_2_inspire.inspire_id=inspirebiotopehabitate.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, inspirebiotopehabitate.id AS SKEY_1, habitattypecovertype.id AS SKEY_2, habitattypecovertype.referencehabitattypeid, habitattypecovertype.referenceeunistypeid, habitattypecovertype.referencehabitatscheme FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_inspire ON biotopangaben.id=biotopangaben_2_inspire.biotopangaben_id JOIN inspirebiotopehabitate ON biotopangaben_2_inspire.inspire_id=inspirebiotopehabitate.id JOIN inspirebiotopehabitate_2_habitat ON inspirebiotopehabitate.id=inspirebiotopehabitate_2_habitat.inspirebiotopehabitate_id JOIN habitattypecovertype ON inspirebiotopehabitate_2_habitat.habitat_id=habitattypecovertype.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, erfassung.id AS SKEY_1, erfassung.datum, erfassung.erfassungsart, erfassung.kampagne, erfassung.name, erfassung.bemerkung FROM biotop JOIN biotopangaben ON biotop.id=biotopangaben.id JOIN biotopangaben_2_erfassung ON biotopangaben.id=biotopangaben_2_erfassung.biotopangaben_id JOIN erfassung ON biotopangaben_2_erfassung.erfassung_id=erfassung.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, pflanzengesellschaft.id AS SKEY_1, pflanzengesellschaft.art, pflanzengesellschaft.deckungprozentual, pflanzengesellschaft.bemerkung FROM biotop JOIN biotop_2_pflanzengesellschaft ON biotop.id=biotop_2_pflanzengesellschaft.biotop_id JOIN pflanzengesellschaft ON biotop_2_pflanzengesellschaft.pflanzengesellschaft_id=pflanzengesellschaft.id WHERE (bio                                                                                                                                                                                                top.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, pflanzengesellschaft.id AS SKEY_1, schichtbt.id AS SKEY_2, schichtbt.art, schichtbt.deckungprozentual, schichtbt.hoeheinmeter, schichtbt.bemerkung FROM biotop JOIN biotop_2_pflanzengesellschaft ON biotop.id=biotop_2_pflanzengesellschaft.biotop_id JOIN pflanzengesellschaft ON biotop_2_pflanzengesellschaft.pflanzengesellschaft_id=pflanzengesellschaft.id JOIN pflanzengesellschaft_2_pflanzenschicht ON pflanzengesellschaft.id=pflanzengesellschaft_2_pflanzenschicht.pflanzengesellschaft_id JOIN schichtbt ON pflanzengesellschaft_2_pflanzenschicht.pflanzenschicht_id=schichtbt.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2,3', 'SELECT biotop.id AS SKEY, pflanzengesellschaft.id AS SKEY_1, schichtbt.id AS SKEY_2, pflanzebt.id AS SKEY_3, pflanzebt.art, pflanzebt.anz, pflanzebt.haeufigkeit, pflanzebt.bemerkung FROM biotop JOIN biotop_2_pflanzengesellschaft ON biotop.id=biotop_2_pflanzengesellschaft.biotop_id JOIN pflanzengesellschaft ON biotop_2_pflanzengesellschaft.pflanzengesellschaft_id=pflanzengesellschaft.id JOIN pflanzengesellschaft_2_pflanzenschicht ON pflanzengesellschaft.id=pflanzengesellschaft_2_pflanzenschicht.pflanzengesellschaft_id JOIN schichtbt ON pflanzengesellschaft_2_pflanzenschicht.pflanzenschicht_id=schichtbt.id JOIN schichtbt_2_pflanzenart ON schichtbt.id=schichtbt_2_pflanzenart.schichtbt_id JOIN pflanzebt ON schichtbt_2_pflanzenart.pflanzenart_id=pflanzebt.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2,3,4', 'SELECT biotop.id AS SKEY, biotop.id AS SKEY_1, biotop.qs, biotop.status, biotop.bedeutung, biotop.bemerkungbedeutung, biotop.beeintraechtigung, biotop.bemerkungbeeintaechtigung, biotop.bemerkungschutzstatus, biotop.entwicklungstendenz, biotop.schutzstatus, biotop.bemerkungentwicklungstendenz, biotop.lebensraumtyp, biotop.gesetzlichgeschuetztesbt, biotop.erstelltvon, biotop.erstelltam, biotop.geaendertvon, biotop.geaendertam FROM biotop WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, erhaltungszustandsbewertung.id AS SKEY_1, erhaltungszustandsbewertung.erhaltungszustandartenkombination, erhaltungszustandsbewertung.bemerkungezartenkombination, erhaltungszustandsbewertung.erhaltungszustandstoerung, erhaltungszustandsbewertung.bemerkungezstoerung, erhaltungszustandsbewertung.erhaltungszustandstruktur, erhaltungszustandsbewertung.bemerkungezstruktur, erhaltungszustandsbewertung.erhaltungszustandgesamt, erhaltungszustandsbewertung.bemerkungezgesamt FROM biotop JOIN biotop_2_bewertungerhaltungszustand ON biotop.id=biotop_2_bewertungerhaltungszustand.biotop_id JOIN erhaltungszustandsbewertung ON biotop_2_bewertungerhaltungszustand.bewertungerhaltungszustand_id=erhaltungszustandsbewertung.id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2', 'SELECT biotop.id AS SKEY, biotop_2_btkomplex.btkomplex_id AS SKEY_1, biotop_2_btkomplex.objektart, biotop_2_btkomplex.btkomplex_id FROM biotop JOIN biotop_2_btkomplex ON biotop.id=biotop_2_btkomplex.biotop_id WHERE (biotop.id >= 2000010 AND biotop.id <= 2000010) AND ((biotop.id IN (SELECT biotop.id FROM biotop JOIN osirisobjekt ON biotop.id=osirisobjekt.id WHERE osirisobjekt.bezeichnung = \'6108-2-23\'))) ORDER BY 1,2']

    }
}
