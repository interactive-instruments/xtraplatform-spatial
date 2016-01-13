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
package de.ii.xtraplatform.ogc.api.wfs.parser;

import de.ii.xtraplatform.ogc.api.WFS;

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
    public void analyzeOperationGetUrl(WFS.OPERATION operation, String url) {

    }

    @Override
    public void analyzeOperationPostUrl(WFS.OPERATION operation, String url) {

    }

    @Override
    public void analyzeOperationParameter(WFS.OPERATION operation, WFS.VOCABULARY parameterName, String value) {

    }

    @Override
    public void analyzeOperationConstraint(WFS.OPERATION operation, WFS.VOCABULARY constraintName, String value) {

    }

    @Override
    public void analyzeOperationMetadata(WFS.OPERATION operation, String url) {

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
