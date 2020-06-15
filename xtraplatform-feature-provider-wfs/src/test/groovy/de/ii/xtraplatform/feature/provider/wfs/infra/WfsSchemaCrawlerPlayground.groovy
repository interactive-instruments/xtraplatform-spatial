/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.infra

import com.fasterxml.aalto.stax.InputFactoryImpl
import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.feature.provider.wfs.FeatureProviderDataWfsFromMetadata
import de.ii.xtraplatform.feature.provider.wfs.WFSCapabilitiesParser
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.provider.wfs.domain.ImmutableConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector
import de.ii.xtraplatform.features.domain.Metadata
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import de.ii.xtraplatform.ogc.api.WFS
import de.ii.xtraplatform.ogc.api.wfs.GetCapabilities
import de.ii.xtraplatform.ogc.api.wfs.WfsOperation
import de.ii.xtraplatform.ogc.api.wfs.WfsRequestEncoder
import org.codehaus.staxmate.SMInputFactory
import spock.lang.Specification

class WfsSchemaCrawlerPlayground extends Specification {
/*
    @Shared WfsSchemaCrawler wfsSchemaCrawler

    def setupSpec() {

        def connectionInfo = new ImmutableConnectionInfoWfsHttp.Builder()
                .version("2.0.0")
                .gmlVersion("3.2.1")
                .uri(URI.create("https://www.wfs.nrw.de/geobasis/wfs_nw_atkis-basis-dlm_aaa-modell-basiert"))
                .build()

        wfsSchemaCrawler = new WfsSchemaCrawler(connectionInfo);

    }

    def 'parse schema'() {

        when:
        def featureTypeList = wfsSchemaCrawler.parseSchema()

        then:

        featureTypeList.size() == 7
        featureTypeList.get(4).name == "Flurstueck"
        featureTypeList.get(4).path == "/flurstueck"
        featureTypeList.get(4).properties.containsKey("id")
        featureTypeList.get(4).properties.get("id").role.get() == FeaturePropertyV2.Role.ID
        featureTypeList.get(4).properties.get("id").type == FeaturePropertyV2.Type.STRING
        featureTypeList.get(4).properties.get("id").path == "ueboname.@id"
        featureTypeList.get(4).properties.containsKey("kreis")
        featureTypeList.get(4).properties.get("kreis").additionalInfo.get("multiple") == "false"
        featureTypeList.get(4).properties.get("kreis").type == FeaturePropertyV2.Type.STRING
        featureTypeList.get(4).properties.get("kreis").path == "kreis"
        featureTypeList.get(4).properties.containsKey("geometrie")
        featureTypeList.get(4).properties.get("geometrie").type == FeaturePropertyV2.Type.GEOMETRY
        featureTypeList.get(4).properties.get("geometrie").path == "geometrie"
        featureTypeList.get(4).properties.get("geometrie").additionalInfo.get("geometryType") == "MULTI_POLYGON"
        featureTypeList.get(4).properties.get("geometrie").additionalInfo.get("crs") == "25832"
        featureTypeList.get(4).properties.get("geometrie").additionalInfo.get("multiple") == "false"
        featureTypeList.get(4).properties.get("flaeche").type == FeaturePropertyV2.Type.FLOAT
        featureTypeList.get(4).properties.get("aktualit").type == FeaturePropertyV2.Type.DATETIME
        featureTypeList.get(4).properties.get("name").additionalInfo.get("multiple") == "true"

    }

 */


    def 'test complex schema'() {
        given:
        def conn = new ImmutableConnectionInfoWfsHttp.Builder()
                .version("2.0.0")
                .gmlVersion("3.2.1")
                .uri(URI.create("https://www.wfs.nrw.de/geobasis/wfs_nw_inspire-adressen_gebref?SERVICE=WFS&REQUEST=GetCapabilities"))
                .build()

        WfsConnector connector = new MockWfsConnectorHttp(conn)
        def wfsSchemaCrawler = new WfsSchemaCrawler(connector, conn)

        when:
        def featureTypeList = wfsSchemaCrawler.parseSchema()

        then:
        featureTypeList.size() == 4
        featureTypeList.get(0).getPropertyMap().containsKey("id")
        featureTypeList.get(0).getPropertyMap().get("id").getRole().get() == SchemaBase.Role.ID
        featureTypeList.get(0).getPropertyMap().get("id").getType() == SchemaBase.Type.STRING
        featureTypeList.get(0).getPropertyMap().get("id").getSourcePath().get() == "/ad:Address/gml:@id"
        featureTypeList.get(0).getPropertyMap().containsKey("inspireId")
        featureTypeList.get(0).getPropertyMap().get("inspireId").getType() == SchemaBase.Type.OBJECT
        featureTypeList.get(0).getPropertyMap().get("inspireId").getProperties().size() == 3
        featureTypeList.get(0).getPropertyMap().containsKey("validFrom")
        featureTypeList.get(0).getPropertyMap().get("validFrom").getType() == SchemaBase.Type.DATETIME
        featureTypeList.get(0).getPropertyMap().get("validFrom").getSourcePath().get() == "/ad:Address/ad:validFrom"
        featureTypeList.get(0).getPropertyMap().get("position").getType() == SchemaBase.Type.OBJECT_ARRAY
        featureTypeList.get(0).getPropertyMap().get("position").getPropertyMap().get("geometry").getType() == SchemaBase.Type.GEOMETRY
        featureTypeList.get(0).getPropertyMap().get("position").getPropertyMap().get("geometry").getGeometryType().get() == SimpleFeatureGeometry.POINT
        featureTypeList.get(0).getPropertyMap().get("pronunciation").getType() == SchemaBase.Type.OBJECT_ARRAY
        featureTypeList.get(0).getPropertyMap().get("pronunciation").getPropertyMap().get("pronunciationIPA").getType() == SchemaBase.Type.VALUE_ARRAY
        featureTypeList.get(0).getPropertyMap().get("pronunciation").getPropertyMap().get("pronunciationIPA").getValueType().get() == SchemaBase.Type.STRING
        featureTypeList.get(0).getPropertyMap().get("pronunciation").getPropertyMap().get("pronunciationIPA").getSourcePath().get() ==
                "/ad:Address/ad:locator/ad:AddressLocator/ad:name/ad:LocatorName/ad:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciation/http://inspire.ec.europa.eu/schemas/gn/4.0:PronunciationOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciationIPA"

    }

    class MockWfsConnectorHttp extends WfsConnectorHttp {

        private final WfsRequestEncoder wfsRequestEncoder
        private static SMInputFactory staxFactory = new SMInputFactory(new InputFactoryImpl())
        ConnectionInfoWfsHttp connectionInfo;


        MockWfsConnectorHttp(ConnectionInfoWfsHttp connectionInfo) {
            Map<String, Map<WFS.METHOD, URI>> urls = ImmutableMap.of("default", ImmutableMap.of(WFS.METHOD.GET, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()), WFS.METHOD.POST, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri())));
            this.connectionInfo = connectionInfo
            this.wfsRequestEncoder = new WfsRequestEncoder(connectionInfo.getVersion(), connectionInfo.getGmlVersion(), connectionInfo.getNamespaces(), urls)
        }

        @Override
        InputStream runWfsOperation(WfsOperation operation) {
            return new URL(wfsRequestEncoder.getAsUrl(operation)).openConnection().getInputStream()
        }

        @Override
        Optional<Metadata> getMetadata() {
            try {

                InputStream inputStream = runWfsOperation(new GetCapabilities())
                WfsCapabilitiesAnalyzer metadataConsumer = new WfsCapabilitiesAnalyzer()
                WFSCapabilitiesParser gmlSchemaParser = new WFSCapabilitiesParser(metadataConsumer, staxFactory)
                gmlSchemaParser.parse(inputStream)
                return Optional.of(metadataConsumer.getMetadata())

            } catch (Throwable e) {
                e.printStackTrace()
            }

            return Optional.empty();
        }

    }
}
