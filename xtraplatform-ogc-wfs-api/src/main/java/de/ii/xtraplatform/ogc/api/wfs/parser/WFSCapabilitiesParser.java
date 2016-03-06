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

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.XLINK;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMEvent;
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
public class WFSCapabilitiesParser {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(WFSCapabilitiesParser.class);

    private final WFSCapabilitiesAnalyzer analyzer;
    private final SMInputFactory staxFactory;

    public WFSCapabilitiesParser(WFSCapabilitiesAnalyzer analyzer, SMInputFactory staxFactory) {
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

            analyzer.analyzeVersion(root.getAttrValue(WFS.getWord(WFS.VOCABULARY.VERSION)));

            SMInputCursor capabilitiesChild = root.childElementCursor().advance();

            while (capabilitiesChild.readerAccessible()) {

                switch (WFS.findKey(capabilitiesChild.getLocalName())) {
                    case SERVICE_IDENTIFICATION:
                        parseServiceIdentification(capabilitiesChild);
                        break;
                    case SERVICE_PROVIDER:
                        parseServiceProvider(capabilitiesChild);
                        break;
                    case OPERATIONS_METADATA:
                        parseOperationsMetadata(capabilitiesChild);
                        break;
                    case FEATURE_TYPE_LIST:
                        parseFeatureTypeList(capabilitiesChild);
                        break;
                }

                capabilitiesChild = capabilitiesChild.advance();
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

    private void parseServiceIdentification(SMInputCursor cursor) throws XMLStreamException {

        SMInputCursor serviceIdentificationChild = cursor.childElementCursor().advance();

        while (serviceIdentificationChild.readerAccessible()) {

            switch (WFS.findKey(serviceIdentificationChild.getLocalName())) {
                case TITLE:
                    analyzer.analyzeTitle(serviceIdentificationChild.collectDescendantText());
                    break;
                case ABSTRACT:
                    analyzer.analyzeAbstract(serviceIdentificationChild.collectDescendantText());
                    break;
                case KEYWORDS:
                    parseKeywords("", false, serviceIdentificationChild);
                    break;
                case FEES:
                    analyzer.analyzeFees(serviceIdentificationChild.collectDescendantText());
                    break;
                case ACCESS_CONSTRAINTS:
                    analyzer.analyzeAccessConstraints(serviceIdentificationChild.collectDescendantText());
                    break;
                case SERVICE_TYPE_VERSION:
                    analyzer.analyzeVersion(serviceIdentificationChild.collectDescendantText());
                    break;
            }

            serviceIdentificationChild = serviceIdentificationChild.advance();
        }
    }

    private void parseKeywords(String featureTypeName, boolean isFeatureType, SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor keywordsChild = cursor.descendantMixedCursor().advance();

        while (keywordsChild.readerAccessible()) {

            if (keywordsChild.getCurrEvent().hasText() && keywordsChild.getCurrEvent() != SMEvent.IGNORABLE_WS) {
                String keywords = keywordsChild.getText().trim();
                if (!keywords.isEmpty()) {
                    if (isFeatureType) {
                        analyzer.analyzeFeatureTypeKeywords(featureTypeName, keywords.split(","));
                    } else {
                        analyzer.analyzeKeywords(keywords.split(","));
                    }
                }
            }

            keywordsChild = keywordsChild.advance();
        }
    }

    private void parseServiceProvider(SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor serviceProviderChild = cursor.childElementCursor().advance();

        while (serviceProviderChild.readerAccessible()) {

            switch (WFS.findKey(serviceProviderChild.getLocalName())) {
                case PROVIDER_NAME:
                    analyzer.analyzeProviderName(serviceProviderChild.collectDescendantText());
                    break;
                case PROVIDER_SITE:
                    analyzer.analyzeProviderSite(serviceProviderChild.collectDescendantText());
                    break;
                case SERVICE_CONTACT:
                    parseServiceContact(serviceProviderChild);
                    break;
            }

            serviceProviderChild = serviceProviderChild.advance();
        }
    }

    private void parseServiceContact(SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor serviceContactChild = cursor.childElementCursor().advance();

        while (serviceContactChild.readerAccessible()) {

            switch (WFS.findKey(serviceContactChild.getLocalName())) {
                case INDIVIDUAL_NAME:
                    analyzer.analyzeServiceContactIndividualName(serviceContactChild.collectDescendantText());
                    break;
                case ORGANIZATION_NAME:
                    analyzer.analyzeServiceContactOrganizationName(serviceContactChild.collectDescendantText());
                    break;
                case POSITION_NAME:
                    analyzer.analyzeServiceContactPositionName(serviceContactChild.collectDescendantText());
                    break;
                case CONTACT_INFO:
                    parseContactInfo(serviceContactChild);
                    break;
                case ROLE:
                    analyzer.analyzeServiceContactRole(serviceContactChild.collectDescendantText());
                    break;
            }

            serviceContactChild = serviceContactChild.advance();
        }
    }

    private void parseContactInfo(SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor contactInfoChild = cursor.childElementCursor().advance();

        while (contactInfoChild.readerAccessible()) {

            switch (WFS.findKey(contactInfoChild.getLocalName())) {
                case PHONE:
                    parsePhone(contactInfoChild);
                    break;
                case ADDRESS:
                    parseAddress(contactInfoChild);
                    break;
                case ONLINE_RESOURCE:
                    analyzer.analyzeServiceContactOnlineResource(contactInfoChild.getAttrValue(XLINK.getNS(XLINK.VERSION.DEFAULT), XLINK.getWord(XLINK.VOCABULARY.HREF)));
                    break;
                case HOURS_OF_SERVICE:
                    analyzer.analyzeServiceContactHoursOfService(contactInfoChild.collectDescendantText());
                    break;
                case CONTACT_INSTRUCTIONS:
                    analyzer.analyzeServiceContactInstructions(contactInfoChild.collectDescendantText());
                    break;
            }

            contactInfoChild = contactInfoChild.advance();
        }
    }

    private void parsePhone(SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor phoneChild = cursor.childElementCursor().advance();

        while (phoneChild.readerAccessible()) {

            switch (WFS.findKey(phoneChild.getLocalName())) {
                case VOICE:
                    analyzer.analyzeServiceContactPhone(phoneChild.collectDescendantText());
                    break;
                case FACSIMILE:
                    analyzer.analyzeServiceContactFacsimile(phoneChild.collectDescendantText());
                    break;
            }

            phoneChild = phoneChild.advance();
        }
    }

    private void parseAddress(SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor addressChild = cursor.childElementCursor().advance();

        while (addressChild.readerAccessible()) {

            switch (WFS.findKey(addressChild.getLocalName())) {
                case DELIVERY_POINT:
                    analyzer.analyzeServiceContactDeliveryPoint(addressChild.collectDescendantText());
                    break;
                case CITY:
                    analyzer.analyzeServiceContactCity(addressChild.collectDescendantText());
                    break;
                case ADMINISTRATIVE_AREA:
                    analyzer.analyzeServiceContactAdministrativeArea(addressChild.collectDescendantText());
                    break;
                case POSTAL_CODE:
                    analyzer.analyzeServiceContactPostalCode(addressChild.collectDescendantText());
                    break;
                case COUNTRY:
                    analyzer.analyzeServiceContactCountry(addressChild.collectDescendantText());
                    break;
                case EMAIL:
                    analyzer.analyzeServiceContactEmail(addressChild.collectDescendantText());
                    break;
            }

            addressChild = addressChild.advance();
        }
    }

    private void parseOperationsMetadata(SMInputCursor cursor) throws XMLStreamException {

        SMInputCursor operationsMetadataChild = cursor.childElementCursor().advance();

        while (operationsMetadataChild.readerAccessible()) {

            switch (WFS.findKey(operationsMetadataChild.getLocalName())) {
                case OPERATION:
                    parseOperation(operationsMetadataChild);
                    break;
                case PARAMETER:
                    parseParameterOrConstraint(WFS.OPERATION.NONE, false, operationsMetadataChild);
                    break;
                case CONSTRAINT:
                    parseParameterOrConstraint(WFS.OPERATION.NONE, true, operationsMetadataChild);
                    break;
                case EXTENDED_CAPABILITIES:
                    parseExtendedCapabilities(operationsMetadataChild);
                    break;
            }

            operationsMetadataChild = operationsMetadataChild.advance();
        }
    }

    private void parseOperation(SMInputCursor cursor) throws XMLStreamException {

        WFS.OPERATION wfsOperation = WFS.OPERATION.fromString(cursor.getAttrValue(WFS.getWord(WFS.VOCABULARY.NAME_ATTRIBUTE)));

        if (wfsOperation == WFS.OPERATION.NONE) {
            wfsOperation = WFS.OPERATION.fromString(cursor.getLocalName());
        }

        if (wfsOperation == WFS.OPERATION.NONE) {
            cursor = cursor.childElementCursor().advance();

            if (WFS.OPERATION.fromString(cursor.getLocalName()) != WFS.OPERATION.NONE) {
                while (cursor.readerAccessible()) {
                    parseOperation(cursor);

                    cursor = cursor.advance();
                }

                return;
            }
        }

        if (wfsOperation != WFS.OPERATION.NONE) {

            SMInputCursor operationChild = cursor.childElementCursor().advance();

            while (operationChild.readerAccessible()) {

                switch (WFS.findKey(operationChild.getLocalName())) {
                    case DCP:
                        SMInputCursor dcpChild = operationChild.descendantElementCursor().advance();

                        while (dcpChild.readerAccessible()) {
                            if (dcpChild.getCurrEvent() == SMEvent.START_ELEMENT) {
                                switch (WFS.findKey(dcpChild.getLocalName())) {
                                    case GET:
                                        analyzer.analyzeOperationGetUrl(wfsOperation, WFS.cleanUrl(parseUrl(dcpChild)));
                                        break;
                                    case POST:
                                        analyzer.analyzeOperationPostUrl(wfsOperation, WFS.cleanUrl(parseUrl(dcpChild)));
                                        break;
                                }
                            }
                            dcpChild = dcpChild.advance();
                        }
                        break;
                    case PARAMETER:
                        parseParameterOrConstraint(wfsOperation, false, operationChild);
                        break;
                    case RESULT_FORMAT:
                        parseResultFormat(wfsOperation, operationChild);
                        break;
                    case CONSTRAINT:
                        parseParameterOrConstraint(wfsOperation, true, operationChild);
                        break;
                    case METADATA:
                        analyzer.analyzeOperationMetadata(wfsOperation, operationChild.getAttrValue(XLINK.getNS(XLINK.VERSION.DEFAULT), XLINK.getWord(XLINK.VOCABULARY.HREF)));
                        break;
                }

                operationChild = operationChild.advance();
            }
        }
    }

    private String parseUrl(SMInputCursor cursor) throws XMLStreamException {
        String url = cursor.getAttrValue(XLINK.getNS(XLINK.VERSION.DEFAULT), XLINK.getWord(XLINK.VOCABULARY.HREF));
        if (url == null) {
            url = cursor.getAttrValue(WFS.getWord(WFS.VOCABULARY.ONLINE_RESOURCE_ATTRIBUTE));
        }
        return url;
    }

    private void parseParameterOrConstraint(WFS.OPERATION operation, boolean isConstraint, SMInputCursor cursor) throws XMLStreamException {

        WFS.VOCABULARY parameterName = WFS.findKey(cursor.getAttrValue(WFS.getWord(WFS.VOCABULARY.NAME_ATTRIBUTE)));

        if (parameterName != WFS.VOCABULARY.NOT_A_WORD) {

            SMInputCursor parameterChild = cursor.descendantElementCursor().advance();

            while (parameterChild.readerAccessible()) {
                if (parameterChild.getCurrEvent() == SMEvent.START_ELEMENT) {
                    switch (WFS.findKey(parameterChild.getLocalName())) {
                        case VALUE:
                        case DEFAULT_VALUE:
                            if (isConstraint) {
                                analyzer.analyzeOperationConstraint(operation, parameterName, parameterChild.collectDescendantText());
                            } else {
                                analyzer.analyzeOperationParameter(operation, parameterName, parameterChild.collectDescendantText());
                            }
                            break;
                    }
                }

                parameterChild = parameterChild.advance();
            }

        }
    }

    private void parseResultFormat(WFS.OPERATION operation, SMInputCursor cursor) throws XMLStreamException {

        WFS.VOCABULARY parameterName = WFS.findKey(cursor.getLocalName());

        if (parameterName == WFS.VOCABULARY.RESULT_FORMAT) {

            SMInputCursor parameterChild = cursor.childElementCursor().advance();

            while (parameterChild.readerAccessible()) {
                analyzer.analyzeOperationParameter(operation, WFS.VOCABULARY.OUTPUT_FORMAT, parameterChild.getLocalName());

                parameterChild = parameterChild.advance();
            }

        }
    }

    private void parseExtendedCapabilities(SMInputCursor cursor) throws XMLStreamException {

        SMInputCursor extendedCapabilitiesChild = cursor.childElementCursor().advance();

        while (extendedCapabilitiesChild.readerAccessible()) {

            switch (WFS.findKey(extendedCapabilitiesChild.getLocalName())) {
                case EXTENDED_CAPABILITIES:
                    SMInputCursor extendedCapabilitiesChild2 = extendedCapabilitiesChild.childElementCursor().advance();

                    while (extendedCapabilitiesChild2.readerAccessible()) {

                        switch (WFS.findKey(extendedCapabilitiesChild2.getLocalName())) {
                            case INSPIRE_METADATA_URL:
                                SMInputCursor inspireMetadataChild = extendedCapabilitiesChild2.childElementCursor().advance();

                                while (inspireMetadataChild.readerAccessible()) {

                                    switch (WFS.findKey(inspireMetadataChild.getLocalName())) {
                                        case INSPIRE_URL:
                                            analyzer.analyzeInspireMetadataUrl(inspireMetadataChild.collectDescendantText());
                                            break;
                                    }

                                    inspireMetadataChild = inspireMetadataChild.advance();
                                }
                                break;
                        }

                        extendedCapabilitiesChild2 = extendedCapabilitiesChild2.advance();
                    }
                    break;
            }

            extendedCapabilitiesChild = extendedCapabilitiesChild.advance();
        }
    }

    private void parseFeatureTypeList(SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor featureTypeListChild = cursor.childElementCursor().advance();

        while (featureTypeListChild.readerAccessible()) {

            switch (WFS.findKey(featureTypeListChild.getLocalName())) {
                case FEATURE_TYPE:
                    parseFeatureType(featureTypeListChild);
                    break;
            }

            featureTypeListChild = featureTypeListChild.advance();
        }
    }

    private void parseFeatureType(SMInputCursor cursor) throws XMLStreamException {

        parseNamespaces(cursor);

        SMInputCursor featureTypeChild = cursor.childElementCursor().advance();

        String featureTypeName = null;

        while (featureTypeChild.readerAccessible()) {

            switch (WFS.findKey(featureTypeChild.getLocalName())) {
                case NAME:
                    parseNamespaces(featureTypeChild);
                    featureTypeName = featureTypeChild.collectDescendantText();
                    analyzer.analyzeFeatureType(featureTypeName);
                    break;
                case TITLE:
                    analyzer.analyzeFeatureTypeTitle(featureTypeName, featureTypeChild.collectDescendantText());
                    break;
                case ABSTRACT:
                    analyzer.analyzeFeatureTypeAbstract(featureTypeName, featureTypeChild.collectDescendantText());
                    break;
                case KEYWORDS:
                    parseKeywords(featureTypeName, true, featureTypeChild);
                    break;
                case DEFAULT_CRS:
                    analyzer.analyzeFeatureTypeDefaultCrs(featureTypeName, featureTypeChild.getElemStringValue());
                    break;
                case OTHER_CRS:
                    analyzer.analyzeFeatureTypeOtherCrs(featureTypeName, featureTypeChild.getElemStringValue());
                    break;
                case WGS84_BOUNDING_BOX:
                    parseBoundingBox(featureTypeName, featureTypeChild);
                    break;
                case METADATA_URL:
                    parseMetadataUrl(featureTypeName, featureTypeChild);
                    break;
            }

            featureTypeChild = featureTypeChild.advance();
        }
    }

    private void parseBoundingBox(String featureTypeName, SMInputCursor cursor) throws XMLStreamException {

        String xmin = cursor.getAttrValue(WFS.getWord(WFS.VOCABULARY.MIN_X));
        String ymin = cursor.getAttrValue(WFS.getWord(WFS.VOCABULARY.MIN_Y));
        String xmax = cursor.getAttrValue(WFS.getWord(WFS.VOCABULARY.MAX_X));
        String ymax = cursor.getAttrValue(WFS.getWord(WFS.VOCABULARY.MAX_Y));

        if (xmin == null || ymin == null || xmax == null || ymax == null) {

            SMInputCursor bboxChild = cursor.childElementCursor().advance();

            String[] lowerCorner = null;
            String[] upperCorner = null;

            while (bboxChild.readerAccessible()) {

                switch (WFS.findKey(bboxChild.getLocalName())) {
                    case LOWER_CORNER:
                        lowerCorner = bboxChild.getElemStringValue().trim().split(" ");
                        break;
                    case UPPER_CORNER:
                        upperCorner = bboxChild.getElemStringValue().trim().split(" ");
                        break;
                }

                bboxChild = bboxChild.advance();
            }

            if (lowerCorner != null && lowerCorner.length == 2 && upperCorner != null && upperCorner.length == 2) {
                xmin = lowerCorner[0];
                ymin = lowerCorner[1];
                xmax = upperCorner[0];
                ymax = upperCorner[1];
            }
        }

        analyzer.analyzeFeatureTypeBoundingBox(featureTypeName, xmin, ymin, xmax, ymax);
    }

    private void parseMetadataUrl(String featureTypeName, SMInputCursor cursor) throws XMLStreamException {

        String url = cursor.getAttrValue(XLINK.getNS(XLINK.VERSION.DEFAULT), XLINK.getWord(XLINK.VOCABULARY.HREF));

        if (url == null) {
            url = cursor.collectDescendantText();
        }

        analyzer.analyzeFeatureTypeMetadataUrl(featureTypeName, url);
    }
}
