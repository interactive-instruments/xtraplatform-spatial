/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.infra

import de.ii.xtraplatform.feature.provider.wfs.domain.ImmutableConnectionInfoWfsHttp
import de.ii.xtraplatform.features.domain.FeatureProperty
import spock.lang.Shared
import spock.lang.Specification

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

        when:
        def featureTypeList = wfsSchemaCrawler.parseSchema()

        then:

        featureTypeList.size() == 6
        featureTypeList.get(4).name == "Flurstueck"
        featureTypeList.get(4).properties.containsKey("id")
        featureTypeList.get(4).properties.get("id").role.get() == FeatureProperty.Role.ID
        featureTypeList.get(4).properties.get("id").type == FeatureProperty.Type.STRING
        featureTypeList.get(4).properties.get("id").path == "ueboname.@id"
        featureTypeList.get(4).properties.containsKey("kreis")
        featureTypeList.get(4).properties.get("kreis").additionalInfo.get("multiple") == "false"
        featureTypeList.get(4).properties.get("kreis").type == FeatureProperty.Type.STRING
        featureTypeList.get(4).properties.get("kreis").path == "kreis"
        featureTypeList.get(4).properties.containsKey("geometrie")
        featureTypeList.get(4).properties.get("geometrie").type == FeatureProperty.Type.GEOMETRY
        featureTypeList.get(4).properties.get("geometrie").path == "geometrie"
        featureTypeList.get(4).properties.get("geometrie").additionalInfo.get("geometryType") == "MULTI_POLYGON"
        featureTypeList.get(4).properties.get("geometrie").additionalInfo.get("crs") == "25832"
        featureTypeList.get(4).properties.get("geometrie").additionalInfo.get("multiple") == "false"
        featureTypeList.get(4).properties.get("flaeche").type == FeatureProperty.Type.FLOAT
        featureTypeList.get(4).properties.get("aktualit").type == FeatureProperty.Type.DATETIME
        featureTypeList.get(4).properties.get("name").additionalInfo.get("multiple") == "true"

    }
}
