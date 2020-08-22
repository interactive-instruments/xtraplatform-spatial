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
package de.ii.xtraplatform.ogc.api.parser;

import de.ii.xtraplatform.ogc.api.OWS;

/**
 * @author zahnen
 */
public class AbstractWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {
    @Override
    public void analyzeStart() {

    }

    @Override
    public void analyzeEnd() {

    }

    @Override
    public void analyzeFailed(Exception ex) {

    }

    @Override
    public void analyzeFailed(String exceptionCode, String exceptionText) {

    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {

    }

    @Override
    public void analyzeVersion(String version) {

    }

    @Override
    public void analyzeTitle(String title) {

    }

    @Override
    public void analyzeAbstract(String abstrct) {

    }

    @Override
    public void analyzeKeywords(String... keywords) {

    }

    @Override
    public void analyzeFees(String fees) {

    }

    @Override
    public void analyzeAccessConstraints(String accessConstraints) {

    }

    @Override
    public void analyzeProviderName(String providerName) {

    }

    @Override
    public void analyzeProviderSite(String providerSite) {

    }

    @Override
    public void analyzeServiceContactIndividualName(String individualName) {

    }

    @Override
    public void analyzeServiceContactOrganizationName(String organizationName) {

    }

    @Override
    public void analyzeServiceContactPositionName(String positionName) {

    }

    @Override
    public void analyzeServiceContactRole(String role) {

    }

    @Override
    public void analyzeServiceContactPhone(String phone) {

    }

    @Override
    public void analyzeServiceContactFacsimile(String facsimile) {

    }

    @Override
    public void analyzeServiceContactDeliveryPoint(String deliveryPoint) {

    }

    @Override
    public void analyzeServiceContactCity(String city) {

    }

    @Override
    public void analyzeServiceContactAdministrativeArea(String administrativeArea) {

    }

    @Override
    public void analyzeServiceContactPostalCode(String postalCode) {

    }

    @Override
    public void analyzeServiceContactCountry(String country) {

    }

    @Override
    public void analyzeServiceContactEmail(String email) {

    }

    @Override
    public void analyzeServiceContactOnlineResource(String onlineResource) {

    }

    @Override
    public void analyzeServiceContactHoursOfService(String hoursOfService) {

    }

    @Override
    public void analyzeServiceContactInstructions(String instructions) {

    }

    @Override
    public void analyzeOperationGetUrl(OWS.OPERATION operation, String url) {

    }

    @Override
    public void analyzeOperationPostUrl(OWS.OPERATION operation, String url) {

    }

    @Override
    public void analyzeOperationParameter(OWS.OPERATION operation, OWS.VOCABULARY parameterName, String value) {

    }

    @Override
    public void analyzeOperationConstraint(OWS.OPERATION operation, OWS.VOCABULARY constraintName, String value) {

    }

    @Override
    public void analyzeOperationMetadata(OWS.OPERATION operation, String url) {

    }

    @Override
    public void analyzeInspireMetadataUrl(String metadataUrl) {

    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {

    }

    @Override
    public void analyzeFeatureTypeTitle(String featureTypeName, String title) {

    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {

    }

    @Override
    public void analyzeFeatureTypeKeywords(String featureTypeName, String... keywords) {

    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {

    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {

    }

    @Override
    public void analyzeFeatureTypeOtherCrs(String featureTypeName, String crs) {

    }

    @Override
    public void analyzeFeatureTypeMetadataUrl(String featureTypeName, String url) {

    }
}
