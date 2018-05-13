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

import de.ii.xtraplatform.ogc.api.CSW;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;

/**
 * @author zahnen
 */
public class LoggingCSWRecordsAnalyzer implements CSWRecordsAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCSWRecordsAnalyzer.class);

    @Override
    public void analyzeStart(SMInputCursor searchResults) {
        try {
            LOGGER.debug("analyzeStart: numberMatched {}, numberReturned {}, nextRecord {}", searchResults.getAttrValue(CSW.getWord(CSW.VOCABULARY.NUMBER_OF_RECORDS_MATCHED)), searchResults.getAttrValue(CSW.getWord(CSW.VOCABULARY.NUMBER_OF_RECORDS_RETURNED)), searchResults.getAttrValue(CSW.getWord(CSW.VOCABULARY.NEXT_RECORD)));
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void analyzeEnd() {
        LOGGER.debug("analyzeEnd");
    }

    @Override
    public void analyzeFailed(Exception ex) {
        LOGGER.debug("analyzeFailed", ex);
    }

    @Override
    public void analyzeFailed(String exceptionCode, String exceptionText) {
        LOGGER.debug("analyzeFailed {} {}", exceptionCode, exceptionText);
    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {
        LOGGER.debug("analyzeNamespace {} {}", prefix, uri);
    }

    @Override
    public void analyzeRecordStart() {
        LOGGER.debug("analyzeRecordStart");
    }

    @Override
    public void analyzeRecordEnd() {
        LOGGER.debug("analyzeRecordEnd");
    }

    @Override
    public void analyzeServiceType(String serviceType) {
        LOGGER.debug("analyzeServiceType {}", serviceType);
    }

    @Override
    public void analyzeServiceTypeVersion(String serviceTypeVersion) {
        LOGGER.debug("analyzeServiceTypeVersion {}", serviceTypeVersion);
    }

    @Override
    public void analyzeOperationName(String operationName) {
        LOGGER.debug("analyzeOperationName {}", operationName);
    }

    @Override
    public void analyzeOperationUrl(String operationUrl) {
        LOGGER.debug("analyzeOperationUrl {}", operationUrl);
    }
}
