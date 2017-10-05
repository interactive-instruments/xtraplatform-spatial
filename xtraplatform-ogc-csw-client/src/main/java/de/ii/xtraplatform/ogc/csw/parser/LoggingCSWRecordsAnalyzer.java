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
package de.ii.xtraplatform.ogc.csw.parser;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.CSW;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.xml.stream.XMLStreamException;

/**
 * @author zahnen
 */
public class LoggingCSWRecordsAnalyzer implements CSWRecordsAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LoggingCSWRecordsAnalyzer.class);

    @Override
    public void analyzeStart(SMInputCursor searchResults) {
        try {
            LOGGER.getLogger().debug("analyzeStart: numberMatched {}, numberReturned {}, nextRecord {}", searchResults.getAttrValue(CSW.getWord(CSW.VOCABULARY.NUMBER_OF_RECORDS_MATCHED)), searchResults.getAttrValue(CSW.getWord(CSW.VOCABULARY.NUMBER_OF_RECORDS_RETURNED)), searchResults.getAttrValue(CSW.getWord(CSW.VOCABULARY.NEXT_RECORD)));
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
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
    public void analyzeRecordStart() {
        LOGGER.getLogger().debug("analyzeRecordStart");
    }

    @Override
    public void analyzeRecordEnd() {
        LOGGER.getLogger().debug("analyzeRecordEnd");
    }

    @Override
    public void analyzeServiceType(String serviceType) {
        LOGGER.getLogger().debug("analyzeServiceType {}", serviceType);
    }

    @Override
    public void analyzeServiceTypeVersion(String serviceTypeVersion) {
        LOGGER.getLogger().debug("analyzeServiceTypeVersion {}", serviceTypeVersion);
    }

    @Override
    public void analyzeOperationName(String operationName) {
        LOGGER.getLogger().debug("analyzeOperationName {}", operationName);
    }

    @Override
    public void analyzeOperationUrl(String operationUrl) {
        LOGGER.getLogger().debug("analyzeOperationUrl {}", operationUrl);
    }
}
