package de.ii.xtraplatform.feature.provider.wfs.infra

import de.ii.xtraplatform.feature.provider.wfs.domain.ImmutableConnectionInfoWfsHttp
import de.ii.xtraplatform.features.domain.FeatureProperty
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.namespace.QName

class WfsSchemaCrawlerPlayground extends Specification {

    @Shared WfsSchemaCrawler wfsSchemaCrawler

    def setupSpec() {

        def connectionInfo = new ImmutableConnectionInfoWfsHttp.Builder()
                .version("2.0.0")
                .gmlVersion("3.2.1")
                .uri(URI.create("https://www.wfs.nrw.de/geobasis/wfs_nw_alkis_vereinfacht"))
                .build()

        wfsSchemaCrawler = new WfsSchemaCrawler(connectionInfo);

    }

    def 'parse schema'() {

        given:
        Map<String, QName> featureTypes = [
                "flurstueck": new QName("http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0", "Flurstueck"),
                "flurstueckPunkt": new QName("http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0", "FlurstueckPunkt"),
                "gebaeudeBauwerk": new QName("http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0", "GebaeudeBauwerk"),
                "katasterBezirk": new QName("http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0", "KatasterBezirk"),
                "nutzung": new QName("http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0", "Nutzung"),
                "verwaltungsEinheit": new QName("http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0", "VerwaltungsEinheit")
        ]

        when:
        def featureTypeList = wfsSchemaCrawler.parseSchema(featureTypes)

        then:

        featureTypeList.size() == 6
        featureTypeList.get(0).name == "Flurstueck"
        featureTypeList.get(0).properties.containsKey("id")
        featureTypeList.get(0).properties.get("id").role.get() == FeatureProperty.Role.ID
        featureTypeList.get(0).properties.get("id").type == FeatureProperty.Type.STRING
        featureTypeList.get(0).properties.get("id").path == "@id"
        featureTypeList.get(0).properties.containsKey("kreis")
        featureTypeList.get(0).properties.get("kreis").additionalInfo.get("multiple") == "false"
        featureTypeList.get(0).properties.get("kreis").type == FeatureProperty.Type.STRING
        featureTypeList.get(0).properties.get("kreis").path == "kreis"
        featureTypeList.get(0).properties.containsKey("geometrie")
        featureTypeList.get(0).properties.get("geometrie").type == FeatureProperty.Type.GEOMETRY
        featureTypeList.get(0).properties.get("geometrie").path == "geometrie"
        featureTypeList.get(0).properties.get("geometrie").additionalInfo.get("geometryType") == "MULTI_POLYGON"
        featureTypeList.get(0).properties.get("geometrie").additionalInfo.get("crs") == "25832"
        featureTypeList.get(0).properties.get("geometrie").additionalInfo.get("multiple") == "false"
    }
}
