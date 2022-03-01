/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.features.gml.app.parser;

import de.ii.xtraplatform.ogc.api.OWS;

/**
 * @author zahnen
 */
public class MultiWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {

    private final WFSCapabilitiesAnalyzer[] analyzers;

    public MultiWfsCapabilitiesAnalyzer(WFSCapabilitiesAnalyzer... analyzers) {
        this.analyzers = analyzers;
    }

    @Override
    public void analyzeStart() {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeStart();
        }
    }

    @Override
    public void analyzeEnd() {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeEnd();
        }
    }

    @Override
    public void analyzeFailed(Exception ex) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFailed(ex);
        }
    }

    @Override
    public void analyzeFailed(String exceptionCode, String exceptionText) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFailed(exceptionCode, exceptionText);
        }
    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeNamespace(prefix, uri);
        }
    }

    @Override
    public void analyzeVersion(String version) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeVersion(version);
        }
    }

    @Override
    public void analyzeTitle(String title) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeTitle(title);
        }
    }

    @Override
    public void analyzeAbstract(String abstrct) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeAbstract(abstrct);
        }
    }

    @Override
    public void analyzeKeywords(String... keywords) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeKeywords(keywords);
        }
    }

    @Override
    public void analyzeFees(String fees) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFees(fees);
        }
    }

    @Override
    public void analyzeAccessConstraints(String accessConstraints) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeAccessConstraints(accessConstraints);
        }
    }

    @Override
    public void analyzeProviderName(String providerName) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeProviderName(providerName);
        }
    }

    @Override
    public void analyzeProviderSite(String providerSite) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeProviderSite(providerSite);
        }
    }

    @Override
    public void analyzeServiceContactIndividualName(String individualName) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactIndividualName(individualName);
        }
    }

    @Override
    public void analyzeServiceContactOrganizationName(String organizationName) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactOrganizationName(organizationName);
        }
    }

    @Override
    public void analyzeServiceContactPositionName(String positionName) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactPositionName(positionName);
        }
    }

    @Override
    public void analyzeServiceContactRole(String role) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactRole(role);
        }
    }

    @Override
    public void analyzeServiceContactPhone(String phone) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactPhone(phone);
        }
    }

    @Override
    public void analyzeServiceContactFacsimile(String facsimile) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactFacsimile(facsimile);
        }
    }

    @Override
    public void analyzeServiceContactDeliveryPoint(String deliveryPoint) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactDeliveryPoint(deliveryPoint);
        }
    }

    @Override
    public void analyzeServiceContactCity(String city) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactCity(city);
        }
    }

    @Override
    public void analyzeServiceContactAdministrativeArea(String administrativeArea) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactAdministrativeArea(administrativeArea);
        }
    }

    @Override
    public void analyzeServiceContactPostalCode(String postalCode) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactPostalCode(postalCode);
        }
    }

    @Override
    public void analyzeServiceContactCountry(String country) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactCountry(country);
        }
    }

    @Override
    public void analyzeServiceContactEmail(String email) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactEmail(email);
        }
    }

    @Override
    public void analyzeServiceContactOnlineResource(String onlineResource) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactOnlineResource(onlineResource);
        }
    }

    @Override
    public void analyzeServiceContactHoursOfService(String hoursOfService) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactHoursOfService(hoursOfService);
        }
    }

    @Override
    public void analyzeServiceContactInstructions(String instructions) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceContactInstructions(instructions);
        }
    }

    @Override
    public void analyzeOperationGetUrl(OWS.OPERATION operation, String url) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeOperationGetUrl(operation, url);
        }
    }

    @Override
    public void analyzeOperationPostUrl(OWS.OPERATION operation, String url) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeOperationPostUrl(operation, url);
        }
    }

    @Override
    public void analyzeOperationParameter(OWS.OPERATION operation, OWS.VOCABULARY parameterName, String value) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeOperationParameter(operation, parameterName, value);
        }
    }

    @Override
    public void analyzeOperationConstraint(OWS.OPERATION operation, OWS.VOCABULARY constraintName, String value) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeOperationConstraint(operation, constraintName, value);
        }
    }

    @Override
    public void analyzeOperationMetadata(OWS.OPERATION operation, String url) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeOperationMetadata(operation, url);
        }
    }

    @Override
    public void analyzeInspireMetadataUrl(String metadataUrl) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeInspireMetadataUrl(metadataUrl);
        }
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureType(featureTypeName);
        }
    }

    @Override
    public void analyzeFeatureTypeTitle(String featureTypeName, String title) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureTypeTitle(featureTypeName, title);
        }
    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureTypeAbstract(featureTypeName, abstrct);
        }
    }

    @Override
    public void analyzeFeatureTypeKeywords(String featureTypeName, String... keywords) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureTypeKeywords(featureTypeName, keywords);
        }
    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureTypeBoundingBox(featureTypeName, xmin, ymin, xmax, ymax);
        }
    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureTypeDefaultCrs(featureTypeName, crs);
        }
    }

    @Override
    public void analyzeFeatureTypeOtherCrs(String featureTypeName, String crs) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureTypeOtherCrs(featureTypeName, crs);
        }
    }

    @Override
    public void analyzeFeatureTypeMetadataUrl(String featureTypeName, String url) {
        for (WFSCapabilitiesAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureTypeMetadataUrl(featureTypeName, url);
        }
    }
}
