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
