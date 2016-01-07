package de.ii.xtraplatform.ogc.api.wfs.parser;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.WFS;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
public class LoggingWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LoggingWfsCapabilitiesAnalyzer.class);

    @Override
    public void analyzeStart() {
        LOGGER.getLogger().debug("analyzeStart");
    }

    @Override
    public void analyzeEnd() {
        LOGGER.getLogger().debug("analyzeEnd");
    }

    @Override
    public void analyzeFailed(Exception ex) {
        LOGGER.getLogger().debug("analyzeFailed", ex);
    }

    @Override
    public void analyzeFailed(String exceptionCode, String exceptionText) {
        LOGGER.getLogger().debug("analyzeFailed {} {}", exceptionCode, exceptionText);
    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {
        LOGGER.getLogger().debug("analyzeNamespace {} {}", prefix, uri);
    }

    @Override
    public void analyzeVersion(String version) {
        LOGGER.getLogger().debug("analyzeVersion {}", version);
    }

    @Override
    public void analyzeTitle(String title) {
        LOGGER.getLogger().debug("analyzeTitle {}", title);
    }

    @Override
    public void analyzeAbstract(String abstrct) {
        LOGGER.getLogger().debug("analyzeAbstract {}", abstrct);
    }

    @Override
    public void analyzeKeywords(String... keywords) {
        LOGGER.getLogger().debug("analyzeKeywords {}", (Object) keywords);
    }

    @Override
    public void analyzeFees(String fees) {
        LOGGER.getLogger().debug("analyzeFees {}", fees);
    }

    @Override
    public void analyzeAccessConstraints(String accessConstraints) {
        LOGGER.getLogger().debug("analyzeAccessConstraints {}", accessConstraints);
    }

    @Override
    public void analyzeProviderName(String providerName) {
        LOGGER.getLogger().debug("analyzeProviderName {}", providerName);
    }

    @Override
    public void analyzeProviderSite(String providerSite) {
        LOGGER.getLogger().debug("analyzeProviderSite {}", providerSite);
    }

    @Override
    public void analyzeServiceContactIndividualName(String individualName) {
        LOGGER.getLogger().debug("analyzeServiceContactIndividualName {}", individualName);
    }

    @Override
    public void analyzeServiceContactOrganizationName(String organizationName) {
        LOGGER.getLogger().debug("analyzeServiceContactOrganizationName {}", organizationName);
    }

    @Override
    public void analyzeServiceContactPositionName(String positionName) {
        LOGGER.getLogger().debug("analyzeServiceContactPositionName {}", positionName);
    }

    @Override
    public void analyzeServiceContactRole(String role) {
        LOGGER.getLogger().debug("analyzeServiceContactRole {}", role);
    }

    @Override
    public void analyzeServiceContactPhone(String phone) {
        LOGGER.getLogger().debug("analyzeServiceContactPhone {}", phone);
    }

    @Override
    public void analyzeServiceContactFacsimile(String facsimile) {
        LOGGER.getLogger().debug("analyzeServiceContactFacsimile {}", facsimile);
    }

    @Override
    public void analyzeServiceContactDeliveryPoint(String deliveryPoint) {
        LOGGER.getLogger().debug("analyzeServiceContactDeliveryPoint {}", deliveryPoint);
    }

    @Override
    public void analyzeServiceContactCity(String city) {
        LOGGER.getLogger().debug("analyzeServiceContactCity {}", city);
    }

    @Override
    public void analyzeServiceContactAdministrativeArea(String administrativeArea) {
        LOGGER.getLogger().debug("analyzeServiceContactAdministrativeArea {}", administrativeArea);
    }

    @Override
    public void analyzeServiceContactPostalCode(String postalCode) {
        LOGGER.getLogger().debug("analyzeServiceContactPostalCode {}", postalCode);
    }

    @Override
    public void analyzeServiceContactCountry(String country) {
        LOGGER.getLogger().debug("analyzeServiceContactCountry {}", country);
    }

    @Override
    public void analyzeServiceContactEmail(String email) {
        LOGGER.getLogger().debug("analyzeServiceContactEmail {}", email);
    }

    @Override
    public void analyzeServiceContactOnlineResource(String onlineResource) {
        LOGGER.getLogger().debug("analyzeServiceContactOnlineResource {}", onlineResource);
    }

    @Override
    public void analyzeServiceContactHoursOfService(String hoursOfService) {
        LOGGER.getLogger().debug("analyzeServiceContactHoursOfService {}", hoursOfService);
    }

    @Override
    public void analyzeServiceContactInstructions(String instructions) {
        LOGGER.getLogger().debug("analyzeServiceContactInstructions {}", instructions);
    }

    @Override
    public void analyzeOperationGetUrl(WFS.OPERATION operation, String url) {
        LOGGER.getLogger().debug("analyzeOperationGetUrl {} {}", operation, url);
    }

    @Override
    public void analyzeOperationPostUrl(WFS.OPERATION operation, String url) {
        LOGGER.getLogger().debug("analyzeOperationPostUrl {} {}", operation, url);
    }

    @Override
    public void analyzeOperationParameter(WFS.OPERATION operation, WFS.VOCABULARY parameterName, String value) {
        LOGGER.getLogger().debug("analyzeOperationParameter {} {} {}", operation, parameterName, value);
    }

    @Override
    public void analyzeOperationConstraint(WFS.OPERATION operation, WFS.VOCABULARY constraintName, String value) {
        LOGGER.getLogger().debug("analyzeOperationConstraint {} {} {}", operation, constraintName, value);
    }

    @Override
    public void analyzeOperationMetadata(WFS.OPERATION operation, String url) {
        LOGGER.getLogger().debug("analyzeOperationMetadata {} {}", operation, url);
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {
        LOGGER.getLogger().debug("analyzeFeatureType {}", featureTypeName);
    }

    @Override
    public void analyzeFeatureTypeTitle(String featureTypeName, String title) {
        LOGGER.getLogger().debug("analyzeFeatureTypeTitle {} {}", featureTypeName, title);
    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {
        LOGGER.getLogger().debug("analyzeFeatureTypeAbstract {} {}", featureTypeName, abstrct);
    }

    @Override
    public void analyzeFeatureTypeKeywords(String featureTypeName, String... keywords) {
        LOGGER.getLogger().debug("analyzeFeatureTypeKeywords {} {}", featureTypeName, (Object) keywords);
    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {
        LOGGER.getLogger().debug("analyzeFeatureTypeBoundingBox {} {} {} {} {}", featureTypeName, xmin, ymin, xmax, ymax);
    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {
        LOGGER.getLogger().debug("analyzeFeatureTypeDefaultCrs {} {}", featureTypeName, crs);
    }

    @Override
    public void analyzeFeatureTypeOtherCrs(String featureTypeName, String crs) {
        LOGGER.getLogger().debug("analyzeFeatureTypeOtherCrs {} {}", featureTypeName, crs);
    }

    @Override
    public void analyzeFeatureTypeMetadataUrl(String featureTypeName, String url) {
        LOGGER.getLogger().debug("analyzeFeatureTypeMetadataUrl {} {}", featureTypeName, url);
    }
}
