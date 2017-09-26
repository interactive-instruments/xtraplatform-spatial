/**
 * Copyright 2016 interactive instruments GmbH
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xtraplatform.ogc.csw.parser;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.ogc.api.WFS;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

/**
 *
 * @author zahnen
 */
public class CSWRecordsParser {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(CSWRecordsParser.class);

    private final CSWRecordsAnalyzer analyzer;
    private final SMInputFactory staxFactory;

    public CSWRecordsParser(CSWRecordsAnalyzer analyzer, SMInputFactory staxFactory) {
        this.analyzer = analyzer;
        this.staxFactory = staxFactory;
    }

    public void parse(HttpEntity entity) {
        try {
            InputSource is = new InputSource(entity.getContent());
            parse(is);
        } catch (IOException ex) {
            // TODO: move to analyzer for XtraProxy
            //LOGGER.error(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);
            //throw new SchemaParseException(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);

            analyzer.analyzeFailed(ex);
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
    }

    public void parse(InputSource is) {

        SMInputCursor root = null;

        try {
            root = staxFactory.rootElementCursor(is.getByteStream()).advance();

            if (checkForExceptionReport(root)) {
                return;
            }

            parseNamespaces(root);

            //analyzer.analyzeVersion(root.getAttrValue(WFS.getWord(WFS.VOCABULARY.VERSION)));

            SMInputCursor recordsResponseChild = root.childElementCursor().advance();

            while (recordsResponseChild.readerAccessible()) {

                switch (CSW.findKey(recordsResponseChild.getLocalName())) {
                    case SEARCH_RESULTS:
                        parseSearchResults(recordsResponseChild);
                        break;
                }

                recordsResponseChild = recordsResponseChild.advance();
            }
        } catch (XMLStreamException ex) {
            // TODO: move to analyzer for XtraProxy
            //LOGGER.error(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);
            //throw new ParseError(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);

            analyzer.analyzeFailed(ex);
        } finally {
            if (root != null) {
                try {
                    root.getStreamReader().closeCompletely();
                } catch (XMLStreamException ex) {
                    // ignore
                }
            }
        }
    }

    private void parseNamespaces(SMInputCursor cursor) throws XMLStreamException {
        XMLStreamReader xml = cursor.getStreamReader();

        for (int i = 0; i < xml.getNamespaceCount(); i++) {
            analyzer.analyzeNamespace(xml.getNamespacePrefix(i), xml.getNamespaceURI(i));
        }
    }

    private boolean checkForExceptionReport(SMInputCursor cursor) throws XMLStreamException {

        boolean exceptionFound = false;

        // TODO: make version agnostic
        if (WFS.findKey(cursor.getLocalName()) == WFS.VOCABULARY.EXCEPTION_REPORT) {

            exceptionFound = true;

            SMInputCursor exceptionReportChild = cursor.childElementCursor().advance();

            while (exceptionReportChild.readerAccessible()) {

                switch (WFS.findKey(exceptionReportChild.getLocalName())) {
                    case EXCEPTION:
                        parseException(exceptionReportChild);
                        break;
                }

                exceptionReportChild = exceptionReportChild.advance();

                // TODO: move to analyzer for XtraProxy
                /*LOGGER.error(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionCode + " " + exceptionText + "");
                WFSException wfse = new WFSException(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionText);
                wfse.addDetail(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionCode);
                throw wfse;
                */
            }
        }

        return exceptionFound;
    }

    private void parseException(SMInputCursor cursor) throws XMLStreamException {

        String exceptionCode = cursor.getAttrValue(WFS.getWord(WFS.VERSION._1_1_0, WFS.VOCABULARY.EXCEPTION_CODE));

        if (exceptionCode != null) {

            SMInputCursor exceptionChild = cursor.childElementCursor().advance();

            while (exceptionChild.readerAccessible()) {

                switch (WFS.findKey(exceptionChild.getLocalName())) {
                    case EXCEPTION_TEXT:
                        analyzer.analyzeFailed(exceptionCode, exceptionChild.collectDescendantText());
                        break;
                }

                exceptionChild = exceptionChild.advance();
            }

        } else {
            exceptionCode = cursor.getAttrValue(WFS.getWord(WFS.VERSION._1_0_0, WFS.VOCABULARY.EXCEPTION_CODE));

            analyzer.analyzeFailed(exceptionCode, cursor.collectDescendantText());
        }

    }

    private void parseSearchResults(SMInputCursor cursor) throws XMLStreamException {

        analyzer.analyzeStart(cursor);

        SMInputCursor searchResultsChild = cursor.childElementCursor().advance();

        while (searchResultsChild.readerAccessible()) {

            switch (CSW.findKey(searchResultsChild.getLocalName())) {
                case MD_METADATA:
                    parseRecord(searchResultsChild);
                    break;
            }

            searchResultsChild = searchResultsChild.advance();
        }

        analyzer.analyzeEnd();
    }

    private void parseRecord(SMInputCursor cursor) throws XMLStreamException {

        parseNamespaces(cursor);
        analyzer.analyzeRecordStart();

        SMInputCursor recordChild = cursor.childElementCursor().advance();

        while (recordChild.readerAccessible()) {

            switch (CSW.findKey(recordChild.getLocalName())) {
                case IDENTIFICATION_INFO:
                    parseIdentificationInfo(recordChild);
                    break;
            }

            recordChild = recordChild.advance();
        }

        analyzer.analyzeRecordEnd();
    }

    private void parseIdentificationInfo(SMInputCursor cursor) throws XMLStreamException {

        SMInputCursor identificationInfoChild = cursor.childElementCursor().advance();

        while (identificationInfoChild.readerAccessible()) {

            switch (CSW.findKey(identificationInfoChild.getLocalName())) {
                case SV_SERVICE_IDENTIFICATION:
                    parseServiceIdentification(identificationInfoChild);
                    break;
            }

            identificationInfoChild = identificationInfoChild.advance();
        }
    }

    private void parseServiceIdentification(SMInputCursor cursor) throws XMLStreamException {

        //analyzer.analyzeStart(cursor);

        SMInputCursor serviceIdentificationChild = cursor.childElementCursor().advance();

        while (serviceIdentificationChild.readerAccessible()) {

            switch (CSW.findKey(serviceIdentificationChild.getLocalName())) {
                case SERVICE_TYPE:
                    analyzer.analyzeServiceType(serviceIdentificationChild.collectDescendantText().trim());
                    break;
                case SERVICE_TYPE_VERSION:
                    analyzer.analyzeServiceTypeVersion(serviceIdentificationChild.collectDescendantText().trim());
                    break;
                case CONTAINS_OPERATIONS:
                    parseOperation(serviceIdentificationChild);
                    break;
            }

            serviceIdentificationChild = serviceIdentificationChild.advance();
        }
    }

    private void parseOperation(SMInputCursor cursor) throws XMLStreamException {

        //analyzer.analyzeStart(cursor);

        SMInputCursor containsOperationChild = cursor.childElementCursor().advance();

        while (containsOperationChild.readerAccessible()) {

            switch (CSW.findKey(containsOperationChild.getLocalName())) {
                case SV_OPERATION_METADATA:
                    parseOperationMetadata(containsOperationChild);
                    break;
            }

            containsOperationChild = containsOperationChild.advance();
        }
    }

    private void parseOperationMetadata(SMInputCursor cursor) throws XMLStreamException {

        SMInputCursor operationMetadataChild = cursor.childElementCursor().advance();

        while (operationMetadataChild.readerAccessible()) {

            switch (CSW.findKey(operationMetadataChild.getLocalName())) {
                case OPERATION_NAME:
                    analyzer.analyzeOperationName(operationMetadataChild.collectDescendantText().trim());
                    break;
                case CONNECT_POINT:
                    parseOperationConnectPoint(operationMetadataChild);
                    break;
            }

            operationMetadataChild = operationMetadataChild.advance();
        }
    }

    private void parseOperationConnectPoint(SMInputCursor cursor) throws XMLStreamException {

        SMInputCursor connectPointChild = cursor.childElementCursor().advance();
        String url = null;
        String protocol = null;

        while (connectPointChild.readerAccessible()) {

            switch (CSW.findKey(connectPointChild.getLocalName())) {
                case CI_ONLINE_RESOURCE:
                    connectPointChild = connectPointChild.childElementCursor();
                    break;
                case LINKAGE:
                    url = connectPointChild.collectDescendantText().trim();
                    break;
                case PROTOCOL:
                    protocol = connectPointChild.collectDescendantText().trim();
                    break;
            }

            connectPointChild = connectPointChild.advance();
        }

        if (url != null) {
            if (protocol != null) {
                analyzer.analyzeServiceTypeVersion(protocol);
            }
            analyzer.analyzeOperationUrl(url);
        }

    }


}
