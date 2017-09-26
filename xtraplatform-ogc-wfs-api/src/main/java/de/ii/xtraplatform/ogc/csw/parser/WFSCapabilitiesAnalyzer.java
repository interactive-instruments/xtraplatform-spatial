/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xtraplatform.ogc.csw.parser;

import de.ii.xtraplatform.ogc.api.WFS;

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

    void analyzeOperationGetUrl(WFS.OPERATION operation, String url);
    void analyzeOperationPostUrl(WFS.OPERATION operation, String url);
    void analyzeOperationParameter(WFS.OPERATION operation, WFS.VOCABULARY parameterName, String value);
    void analyzeOperationConstraint(WFS.OPERATION operation, WFS.VOCABULARY constraintName, String value);
    void analyzeOperationMetadata(WFS.OPERATION operation, String url);

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
