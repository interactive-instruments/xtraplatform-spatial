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
package de.ii.xtraplatform.feature.provider.wfs.app.parser;

import de.ii.xtraplatform.ogc.api.OWS;

/**
 *
 * @author zahnen
 */
public interface WFSCapabilitiesAnalyzer {

    void analyzeStart();
    void analyzeEnd();
    void analyzeFailed(Exception ex);
    void analyzeFailed(String exceptionCode, String exceptionText);

    void analyzeNamespace(String prefix, String uri);

    void analyzeTitle(String title);
    void analyzeAbstract(String abstrct);
    void analyzeKeywords(String... keywords);
    void analyzeFees(String fees);
    void analyzeAccessConstraints(String accessConstraints);
    void analyzeVersion(String version);

    void analyzeProviderName(String providerName);
    void analyzeProviderSite(String providerSite);
    void analyzeServiceContactIndividualName(String individualName);
    void analyzeServiceContactOrganizationName(String organizationName);
    void analyzeServiceContactPositionName(String positionName);
    void analyzeServiceContactRole(String role);
    void analyzeServiceContactPhone(String phone);
    void analyzeServiceContactFacsimile(String facsimile);
    void analyzeServiceContactDeliveryPoint(String deliveryPoint);
    void analyzeServiceContactCity(String city);
    void analyzeServiceContactAdministrativeArea(String administrativeArea);
    void analyzeServiceContactPostalCode(String postalCode);
    void analyzeServiceContactCountry(String country);
    void analyzeServiceContactEmail(String email);
    void analyzeServiceContactOnlineResource(String onlineResource);
    void analyzeServiceContactHoursOfService(String hoursOfService);
    void analyzeServiceContactInstructions(String instructions);

    void analyzeOperationGetUrl(OWS.OPERATION operation, String url);
    void analyzeOperationPostUrl(OWS.OPERATION operation, String url);
    void analyzeOperationParameter(OWS.OPERATION operation, OWS.VOCABULARY parameterName, String value);
    void analyzeOperationConstraint(OWS.OPERATION operation, OWS.VOCABULARY constraintName, String value);
    void analyzeOperationMetadata(OWS.OPERATION operation, String url);

    void analyzeInspireMetadataUrl(String metadataUrl);

    void analyzeFeatureType(String featureTypeName);
    void analyzeFeatureTypeTitle(String featureTypeName, String title);
    void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct);
    void analyzeFeatureTypeKeywords(String featureTypeName, String... keywords);
    void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax);
    void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs);
    void analyzeFeatureTypeOtherCrs(String featureTypeName, String crs);
    void analyzeFeatureTypeMetadataUrl(String featureTypeName, String url);
}
