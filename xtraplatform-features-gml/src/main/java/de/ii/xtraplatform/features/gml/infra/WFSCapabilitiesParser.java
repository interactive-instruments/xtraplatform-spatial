/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.infra;

import de.ii.xtraplatform.features.domain.FeatureProviderMetadataConsumer;
import de.ii.xtraplatform.features.gml.infra.req.OWS;
import de.ii.xtraplatform.features.gml.infra.req.XLINK;
import java.io.InputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMInputCursor;
import org.xml.sax.InputSource;

/**
 * @author zahnen
 */
public class WFSCapabilitiesParser {

  private final FeatureProviderMetadataConsumer metadataConsumer;
  private final SMInputFactory staxFactory;

  public WFSCapabilitiesParser(
      FeatureProviderMetadataConsumer metadataConsumer, SMInputFactory staxFactory) {
    this.metadataConsumer = metadataConsumer;
    this.staxFactory = staxFactory;
  }

  public void parse(InputStream inputStream) {
    try {
      InputSource is = new InputSource(inputStream);
      parse(is);
    } catch (Exception ex) {
      // TODO: move to analyzer for XtraProxy
      // LOGGER.error(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);
      // throw new SchemaParseException(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);

      metadataConsumer.analyzeFailed(ex);
    }
  }

  public void parse(InputSource is) {

    SMInputCursor root = null;

    metadataConsumer.analyzeStart();

    try {
      root = staxFactory.rootElementCursor(is.getByteStream()).advance();

      if (checkForExceptionReport(root)) {
        return;
      }

      parseNamespaces(root);

      metadataConsumer.analyzeVersion(root.getAttrValue(OWS.getWord(OWS.VOCABULARY.VERSION)));

      SMInputCursor capabilitiesChild = root.childElementCursor().advance();

      while (capabilitiesChild.readerAccessible()) {

        switch (OWS.findKey(capabilitiesChild.getLocalName())) {
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
      // LOGGER.error(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);
      // throw new ParseError(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);

      metadataConsumer.analyzeFailed(ex);
    } finally {
      if (root != null) {
        try {
          root.getStreamReader().closeCompletely();
        } catch (XMLStreamException ex) {
          // ignore
        }
      }
    }

    metadataConsumer.analyzeEnd();
  }

  private void parseNamespaces(SMInputCursor cursor) throws XMLStreamException {
    XMLStreamReader xml = cursor.getStreamReader();

    for (int i = 0; i < xml.getNamespaceCount(); i++) {
      metadataConsumer.analyzeNamespace(xml.getNamespacePrefix(i), xml.getNamespaceURI(i));
    }
  }

  private boolean checkForExceptionReport(SMInputCursor cursor) throws XMLStreamException {

    boolean exceptionFound = false;

    // TODO: make version agnostic
    if (OWS.findKey(cursor.getLocalName()) == OWS.VOCABULARY.EXCEPTION_REPORT) {

      exceptionFound = true;

      SMInputCursor exceptionReportChild = cursor.childElementCursor().advance();

      boolean reportChildFound = false;

      while (exceptionReportChild.readerAccessible()) {

        switch (OWS.findKey(exceptionReportChild.getLocalName())) {
          case EXCEPTION:
            parseException(exceptionReportChild);
            reportChildFound = true;
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

      if (!reportChildFound) {
        metadataConsumer.analyzeFailed("", "");
      }
    }

    return exceptionFound;
  }

  private void parseException(SMInputCursor cursor) throws XMLStreamException {

    String exceptionCode =
        cursor.getAttrValue(OWS.getWord(OWS.VERSION._1_1_0, OWS.VOCABULARY.EXCEPTION_CODE));

    if (exceptionCode != null) {

      SMInputCursor exceptionChild = cursor.childElementCursor().advance();

      while (exceptionChild.readerAccessible()) {

        switch (OWS.findKey(exceptionChild.getLocalName())) {
          case EXCEPTION_TEXT:
            metadataConsumer.analyzeFailed(exceptionCode, exceptionChild.collectDescendantText());
            break;
        }

        exceptionChild = exceptionChild.advance();
      }

    } else {
      exceptionCode =
          cursor.getAttrValue(OWS.getWord(OWS.VERSION._1_0_0, OWS.VOCABULARY.EXCEPTION_CODE));

      metadataConsumer.analyzeFailed(exceptionCode, cursor.collectDescendantText());
    }
  }

  private void parseServiceIdentification(SMInputCursor cursor) throws XMLStreamException {

    SMInputCursor serviceIdentificationChild = cursor.childElementCursor().advance();

    while (serviceIdentificationChild.readerAccessible()) {

      switch (OWS.findKey(serviceIdentificationChild.getLocalName())) {
        case TITLE:
          metadataConsumer.analyzeTitle(serviceIdentificationChild.collectDescendantText());
          break;
        case ABSTRACT:
          metadataConsumer.analyzeAbstract(serviceIdentificationChild.collectDescendantText());
          break;
        case KEYWORDS:
          parseKeywords("", false, serviceIdentificationChild);
          break;
        case FEES:
          metadataConsumer.analyzeFees(serviceIdentificationChild.collectDescendantText());
          break;
        case ACCESS_CONSTRAINTS:
          metadataConsumer.analyzeAccessConstraints(
              serviceIdentificationChild.collectDescendantText());
          break;
        case SERVICE_TYPE_VERSION:
          metadataConsumer.analyzeVersion(serviceIdentificationChild.collectDescendantText());
          break;
      }

      serviceIdentificationChild = serviceIdentificationChild.advance();
    }
  }

  private void parseKeywords(String featureTypeName, boolean isFeatureType, SMInputCursor cursor)
      throws XMLStreamException {
    SMInputCursor keywordsChild = cursor.descendantMixedCursor().advance();

    while (keywordsChild.readerAccessible()) {

      if (keywordsChild.getCurrEvent().hasText()
          && keywordsChild.getCurrEvent() != SMEvent.IGNORABLE_WS) {
        String keywords = keywordsChild.getText().trim();
        if (!keywords.isEmpty()) {
          if (isFeatureType) {
            metadataConsumer.analyzeFeatureTypeKeywords(featureTypeName, keywords.split(","));
          } else {
            metadataConsumer.analyzeKeywords(keywords.split(","));
          }
        }
      }

      keywordsChild = keywordsChild.advance();
    }
  }

  private void parseServiceProvider(SMInputCursor cursor) throws XMLStreamException {
    SMInputCursor serviceProviderChild = cursor.childElementCursor().advance();

    while (serviceProviderChild.readerAccessible()) {

      switch (OWS.findKey(serviceProviderChild.getLocalName())) {
        case PROVIDER_NAME:
          metadataConsumer.analyzeProviderName(serviceProviderChild.collectDescendantText());
          break;
        case PROVIDER_SITE:
          metadataConsumer.analyzeProviderSite(serviceProviderChild.collectDescendantText());
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

      switch (OWS.findKey(serviceContactChild.getLocalName())) {
        case INDIVIDUAL_NAME:
          metadataConsumer.analyzeServiceContactIndividualName(
              serviceContactChild.collectDescendantText());
          break;
        case ORGANIZATION_NAME:
          metadataConsumer.analyzeServiceContactOrganizationName(
              serviceContactChild.collectDescendantText());
          break;
        case POSITION_NAME:
          metadataConsumer.analyzeServiceContactPositionName(
              serviceContactChild.collectDescendantText());
          break;
        case CONTACT_INFO:
          parseContactInfo(serviceContactChild);
          break;
        case ROLE:
          metadataConsumer.analyzeServiceContactRole(serviceContactChild.collectDescendantText());
          break;
      }

      serviceContactChild = serviceContactChild.advance();
    }
  }

  private void parseContactInfo(SMInputCursor cursor) throws XMLStreamException {
    SMInputCursor contactInfoChild = cursor.childElementCursor().advance();

    while (contactInfoChild.readerAccessible()) {

      switch (OWS.findKey(contactInfoChild.getLocalName())) {
        case PHONE:
          parsePhone(contactInfoChild);
          break;
        case ADDRESS:
          parseAddress(contactInfoChild);
          break;
        case ONLINE_RESOURCE:
          metadataConsumer.analyzeServiceContactOnlineResource(
              contactInfoChild.getAttrValue(
                  XLINK.getNS(XLINK.VERSION.DEFAULT), XLINK.getWord(XLINK.VOCABULARY.HREF)));
          break;
        case HOURS_OF_SERVICE:
          metadataConsumer.analyzeServiceContactHoursOfService(
              contactInfoChild.collectDescendantText());
          break;
        case CONTACT_INSTRUCTIONS:
          metadataConsumer.analyzeServiceContactInstructions(
              contactInfoChild.collectDescendantText());
          break;
      }

      contactInfoChild = contactInfoChild.advance();
    }
  }

  private void parsePhone(SMInputCursor cursor) throws XMLStreamException {
    SMInputCursor phoneChild = cursor.childElementCursor().advance();

    while (phoneChild.readerAccessible()) {

      switch (OWS.findKey(phoneChild.getLocalName())) {
        case VOICE:
          metadataConsumer.analyzeServiceContactPhone(phoneChild.collectDescendantText());
          break;
        case FACSIMILE:
          metadataConsumer.analyzeServiceContactFacsimile(phoneChild.collectDescendantText());
          break;
      }

      phoneChild = phoneChild.advance();
    }
  }

  private void parseAddress(SMInputCursor cursor) throws XMLStreamException {
    SMInputCursor addressChild = cursor.childElementCursor().advance();

    while (addressChild.readerAccessible()) {

      switch (OWS.findKey(addressChild.getLocalName())) {
        case DELIVERY_POINT:
          metadataConsumer.analyzeServiceContactDeliveryPoint(addressChild.collectDescendantText());
          break;
        case CITY:
          metadataConsumer.analyzeServiceContactCity(addressChild.collectDescendantText());
          break;
        case ADMINISTRATIVE_AREA:
          metadataConsumer.analyzeServiceContactAdministrativeArea(
              addressChild.collectDescendantText());
          break;
        case POSTAL_CODE:
          metadataConsumer.analyzeServiceContactPostalCode(addressChild.collectDescendantText());
          break;
        case COUNTRY:
          metadataConsumer.analyzeServiceContactCountry(addressChild.collectDescendantText());
          break;
        case EMAIL:
          metadataConsumer.analyzeServiceContactEmail(addressChild.collectDescendantText());
          break;
      }

      addressChild = addressChild.advance();
    }
  }

  private void parseOperationsMetadata(SMInputCursor cursor) throws XMLStreamException {

    SMInputCursor operationsMetadataChild = cursor.childElementCursor().advance();

    while (operationsMetadataChild.readerAccessible()) {

      switch (OWS.findKey(operationsMetadataChild.getLocalName())) {
        case OPERATION:
          parseOperation(operationsMetadataChild);
          break;
        case PARAMETER:
          parseParameterOrConstraint(OWS.OPERATION.NONE, false, operationsMetadataChild);
          break;
        case CONSTRAINT:
          parseParameterOrConstraint(OWS.OPERATION.NONE, true, operationsMetadataChild);
          break;
        case EXTENDED_CAPABILITIES:
          parseExtendedCapabilities(operationsMetadataChild);
          break;
      }

      operationsMetadataChild = operationsMetadataChild.advance();
    }
  }

  private void parseOperation(SMInputCursor cursor) throws XMLStreamException {

    OWS.OPERATION wfsOperation =
        OWS.OPERATION.fromString(cursor.getAttrValue(OWS.getWord(OWS.VOCABULARY.NAME_ATTRIBUTE)));

    if (wfsOperation == OWS.OPERATION.NONE) {
      wfsOperation = OWS.OPERATION.fromString(cursor.getLocalName());
    }

    if (wfsOperation == OWS.OPERATION.NONE) {
      cursor = cursor.childElementCursor().advance();

      while (cursor.readerAccessible()) {
        if (OWS.OPERATION.fromString(cursor.getLocalName()) != OWS.OPERATION.NONE) {
          parseOperation(cursor);
        }

        cursor = cursor.advance();
      }

      return;
    }

    if (wfsOperation != OWS.OPERATION.NONE) {

      SMInputCursor operationChild = cursor.childElementCursor().advance();

      while (operationChild.readerAccessible()) {

        switch (OWS.findKey(operationChild.getLocalName())) {
          case DCP:
            SMInputCursor dcpChild = operationChild.descendantElementCursor().advance();

            while (dcpChild.readerAccessible()) {
              if (dcpChild.getCurrEvent() == SMEvent.START_ELEMENT) {
                switch (OWS.findKey(dcpChild.getLocalName())) {
                  case GET:
                    metadataConsumer.analyzeOperationGetUrl(
                        wfsOperation.toString(), parseUrl(dcpChild));
                    break;
                  case POST:
                    metadataConsumer.analyzeOperationPostUrl(
                        wfsOperation.toString(), parseUrl(dcpChild));
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
            metadataConsumer.analyzeOperationMetadata(
                wfsOperation.toString(),
                operationChild.getAttrValue(
                    XLINK.getNS(XLINK.VERSION.DEFAULT), XLINK.getWord(XLINK.VOCABULARY.HREF)));
            break;
        }

        operationChild = operationChild.advance();
      }
    }
  }

  private String parseUrl(SMInputCursor cursor) throws XMLStreamException {
    String url =
        cursor.getAttrValue(
            XLINK.getNS(XLINK.VERSION.DEFAULT), XLINK.getWord(XLINK.VOCABULARY.HREF));
    if (url == null) {
      url = cursor.getAttrValue(OWS.getWord(OWS.VOCABULARY.ONLINE_RESOURCE_ATTRIBUTE));
    }
    return url;
  }

  private void parseParameterOrConstraint(
      OWS.OPERATION operation, boolean isConstraint, SMInputCursor cursor)
      throws XMLStreamException {

    OWS.VOCABULARY parameterName =
        OWS.findKey(cursor.getAttrValue(OWS.getWord(OWS.VOCABULARY.NAME_ATTRIBUTE)));

    if (parameterName != OWS.VOCABULARY.NOT_A_WORD) {

      SMInputCursor parameterChild = cursor.descendantElementCursor().advance();

      while (parameterChild.readerAccessible()) {
        if (parameterChild.getCurrEvent() == SMEvent.START_ELEMENT) {
          switch (OWS.findKey(parameterChild.getLocalName())) {
            case VALUE:
            case DEFAULT_VALUE:
              if (isConstraint) {
                metadataConsumer.analyzeOperationConstraint(
                    operation.toString(),
                    parameterName.toString(),
                    parameterChild.collectDescendantText());
              } else {
                metadataConsumer.analyzeOperationParameter(
                    operation.toString(),
                    parameterName.toString(),
                    parameterChild.collectDescendantText());
              }
              break;
          }
        }

        parameterChild = parameterChild.advance();
      }
    }
  }

  private void parseResultFormat(OWS.OPERATION operation, SMInputCursor cursor)
      throws XMLStreamException {

    OWS.VOCABULARY parameterName = OWS.findKey(cursor.getLocalName());

    if (parameterName == OWS.VOCABULARY.RESULT_FORMAT) {

      SMInputCursor parameterChild = cursor.childElementCursor().advance();

      while (parameterChild.readerAccessible()) {
        metadataConsumer.analyzeOperationParameter(
            operation.toString(),
            OWS.VOCABULARY.OUTPUT_FORMAT.toString(),
            parameterChild.getLocalName());

        parameterChild = parameterChild.advance();
      }
    }
  }

  private void parseExtendedCapabilities(SMInputCursor cursor) throws XMLStreamException {

    SMInputCursor extendedCapabilitiesChild = cursor.childElementCursor().advance();

    while (extendedCapabilitiesChild.readerAccessible()) {

      switch (OWS.findKey(extendedCapabilitiesChild.getLocalName())) {
        case EXTENDED_CAPABILITIES:
          SMInputCursor extendedCapabilitiesChild2 =
              extendedCapabilitiesChild.childElementCursor().advance();

          while (extendedCapabilitiesChild2.readerAccessible()) {

            switch (OWS.findKey(extendedCapabilitiesChild2.getLocalName())) {
              case INSPIRE_METADATA_URL:
                SMInputCursor inspireMetadataChild =
                    extendedCapabilitiesChild2.childElementCursor().advance();

                while (inspireMetadataChild.readerAccessible()) {

                  switch (OWS.findKey(inspireMetadataChild.getLocalName())) {
                    case INSPIRE_URL:
                      metadataConsumer.analyzeInspireMetadataUrl(
                          inspireMetadataChild.collectDescendantText());
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

      switch (OWS.findKey(featureTypeListChild.getLocalName())) {
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

      switch (OWS.findKey(featureTypeChild.getLocalName())) {
        case NAME:
          parseNamespaces(featureTypeChild);
          featureTypeName = featureTypeChild.collectDescendantText();
          metadataConsumer.analyzeFeatureType(featureTypeName);
          break;
        case TITLE:
          metadataConsumer.analyzeFeatureTypeTitle(
              featureTypeName, featureTypeChild.collectDescendantText());
          break;
        case ABSTRACT:
          metadataConsumer.analyzeFeatureTypeAbstract(
              featureTypeName, featureTypeChild.collectDescendantText());
          break;
        case KEYWORDS:
          parseKeywords(featureTypeName, true, featureTypeChild);
          break;
        case DEFAULT_CRS:
          metadataConsumer.analyzeFeatureTypeDefaultCrs(
              featureTypeName, featureTypeChild.getElemStringValue());
          break;
        case OTHER_CRS:
          metadataConsumer.analyzeFeatureTypeOtherCrs(
              featureTypeName, featureTypeChild.getElemStringValue());
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

  private void parseBoundingBox(String featureTypeName, SMInputCursor cursor)
      throws XMLStreamException {

    String xmin = cursor.getAttrValue(OWS.getWord(OWS.VOCABULARY.MIN_X));
    String ymin = cursor.getAttrValue(OWS.getWord(OWS.VOCABULARY.MIN_Y));
    String xmax = cursor.getAttrValue(OWS.getWord(OWS.VOCABULARY.MAX_X));
    String ymax = cursor.getAttrValue(OWS.getWord(OWS.VOCABULARY.MAX_Y));

    if (xmin == null || ymin == null || xmax == null || ymax == null) {

      SMInputCursor bboxChild = cursor.childElementCursor().advance();

      String[] lowerCorner = null;
      String[] upperCorner = null;

      while (bboxChild.readerAccessible()) {

        switch (OWS.findKey(bboxChild.getLocalName())) {
          case LOWER_CORNER:
            lowerCorner = bboxChild.getElemStringValue().trim().split(" ");
            break;
          case UPPER_CORNER:
            upperCorner = bboxChild.getElemStringValue().trim().split(" ");
            break;
        }

        bboxChild = bboxChild.advance();
      }

      if (lowerCorner != null
          && lowerCorner.length == 2
          && upperCorner != null
          && upperCorner.length == 2) {
        xmin = lowerCorner[0];
        ymin = lowerCorner[1];
        xmax = upperCorner[0];
        ymax = upperCorner[1];
      }
    }

    metadataConsumer.analyzeFeatureTypeBoundingBox(featureTypeName, xmin, ymin, xmax, ymax);
  }

  private void parseMetadataUrl(String featureTypeName, SMInputCursor cursor)
      throws XMLStreamException {

    String url =
        cursor.getAttrValue(
            XLINK.getNS(XLINK.VERSION.DEFAULT), XLINK.getWord(XLINK.VOCABULARY.HREF));

    if (url == null) {
      url = cursor.collectDescendantText();
    }

    metadataConsumer.analyzeFeatureTypeMetadataUrl(featureTypeName, url);
  }
}
