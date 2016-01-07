package de.ii.xtraplatform.ogc.api.wfs.parser;

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

    void analyzeFeatureType(String featureTypeName);
    void analyzeFeatureTypeTitle(String featureTypeName, String title);
    void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct);
    void analyzeFeatureTypeKeywords(String featureTypeName, String... keywords);
    void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax);
    void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs);
    void analyzeFeatureTypeOtherCrs(String featureTypeName, String crs);
    void analyzeFeatureTypeMetadataUrl(String featureTypeName, String url);
}
