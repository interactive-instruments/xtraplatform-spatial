package de.ii.xtraplatform.ogc.api.wfs.parser;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.WFS;
import java.io.IOException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import de.ii.xtraplatform.ogc.api.exceptions.ParseError;
import de.ii.xtraplatform.ogc.api.exceptions.SchemaParseException;
import de.ii.xtraplatform.ogc.api.exceptions.WFSException;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.xml.sax.InputSource;

/**
 *
 * @author zahnen
 */
public class WFSCapabilitiesParser {

    private static final String XLINK_NSURI = "http://www.w3.org/1999/xlink";
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(WFSCapabilitiesParser.class);
    private final WFSCapabilitiesAnalyzer analyzer;
    private final SMInputFactory staxFactory;

    public WFSCapabilitiesParser(WFSCapabilitiesAnalyzer analyzer, SMInputFactory staxFactory) {
        this.analyzer = analyzer;
        this.staxFactory = staxFactory;
    }

    public void parse(HttpEntity entity) throws ParseError {
        try {
            InputSource is = new InputSource(entity.getContent());
            parse(is);
        } catch (IOException ex) {
            LOGGER.error(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);
            throw new SchemaParseException(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
    }

    public void parse(InputSource is) throws WFSException {
        LOGGER.debug(FrameworkMessages.PARSING_CAPABILITIES_FOR_WFS);
        SMInputCursor root = null;
        try {
            root = staxFactory.rootElementCursor(is.getByteStream()).advance();

            if (root.getLocalName().contains("ExceptionReport")) {
                SMInputCursor exception = root.childElementCursor().advance();
                while (exception.readerAccessible()) {

                    String exceptionCode = "";
                    String exceptionText = "";
                    while (exception.readerAccessible()) {
                        if (exception.getCurrEventCode() == XMLStreamConstants.START_ELEMENT) {
                            if (exception.getLocalName().equals("ServiceException")) {
                                exceptionCode = exception.getAttrValue("code");
                                exceptionText = exception.collectDescendantText();
                            }
                        }
                        exception = exception.advance();
                    }

                    LOGGER.error(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionCode + " " + exceptionText + "");
                    WFSException wfse = new WFSException(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionText);
                    wfse.addDetail(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionCode);
                    throw wfse;
                }
            }

            analyzer.analyzeNamespaces(root.getStreamReader());

            // for WFS 1.0.0 ...
            String version = root.getAttrValue("version");

            analyzer.analyzeVersion(version);

            SMInputCursor body = root.childElementCursor().advance();

            while (body.readerAccessible()) {
                if (body.hasLocalName("ServiceIdentification") || body.hasLocalName("Service")) {
                    parseServiceIdentification(body);
                } else if (body.hasLocalName("OperationsMetadata")) {
                    parseOperationsMetadata(body);
                } else if (body.hasLocalName("FeatureTypeList")) {
                    parseFeatureTypes(body);
                }
                body = body.advance();
            }
        } catch (WFSException ex) {
            throw ex;
        } catch (XMLStreamException ex) {
            LOGGER.error(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);
            throw new ParseError(FrameworkMessages.ERROR_PARSING_WFS_CAPABILITIES);
        } finally {
            if (root != null) {
                try {
                    root.getStreamReader().closeCompletely();
                } catch (XMLStreamException ex) {
                }
            }
        }
    }

    private void parseServiceIdentification(SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor serviceIdentification = cursor.childElementCursor().advance();
        while (serviceIdentification.readerAccessible()) {
            if (serviceIdentification.hasLocalName("Title")) {
                analyzer.analyzeTitle(serviceIdentification.collectDescendantText());
            } else if (serviceIdentification.hasLocalName("Abstract")) {
                String txt = serviceIdentification.collectDescendantText();
                if (!txt.isEmpty()) {
                    analyzer.analyzeTitle(": " + txt);
                }
            } else if (serviceIdentification.hasLocalName("Fees")) {
                String txt = serviceIdentification.collectDescendantText();
                if (!txt.isEmpty()) {
                    analyzer.analyzeTitle(", Fees: " + txt);
                }
            } else if (serviceIdentification.hasLocalName("AccessConstraints")) {
                String txt = serviceIdentification.collectDescendantText();
                if (!txt.isEmpty()) {
                    analyzer.analyzeTitle(", AccessConstraints: " + txt);
                    analyzer.analyzeCopyright(txt);
                }
            } else if (serviceIdentification.hasLocalName("ServiceTypeVersion")) {
                analyzer.analyzeVersion(serviceIdentification.collectDescendantText());
            }
            serviceIdentification = serviceIdentification.advance();
        }
    }

    private void parseOperationsMetadata(SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor operationsMetadata = cursor.childElementCursor().advance();
        while (operationsMetadata.readerAccessible()) {
            if (operationsMetadata.hasLocalName("Operation") || operationsMetadata.hasLocalName("Parameter")) {

                String op = operationsMetadata.getAttrValue("name");
                WFS.OPERATION wfsOp = WFS.OPERATION.fromString(op);

                if (wfsOp != null) {
                    //LOGGER.info("OPERATION: {}", wfsOp.toString());

                    SMInputCursor dcp = operationsMetadata.descendantElementCursor().advance();
                    while (dcp.readerAccessible()) {
                        if (dcp.getCurrEvent() == SMEvent.START_ELEMENT) {
                            if (dcp.hasLocalName("Post")) {
                                String url = dcp.getAttrValue(XLINK_NSURI, "href");
                                //LOGGER.info(" - Post: {}", url);
                                analyzer.analyzeDCPPOST(wfsOp, url);
                            }
                            if (dcp.hasLocalName("Get")) {
                                String url = dcp.getAttrValue(XLINK_NSURI, "href");
                                //LOGGER.info(" - Get: {}", url);
                                analyzer.analyzeDCPGET(wfsOp, url);
                            }
                            if (dcp.hasLocalName("Parameter")) {
                                if (dcp.getAttrValue("name").equals("outputFormat")) {

                                    SMInputCursor value = dcp.childElementCursor().advance();
                                    while (value.readerAccessible()) {

                                        if (value.hasLocalName("AllowedValues")) {
                                            value = value.childElementCursor().advance();
                                        }

                                        if (value.hasLocalName("Value") || value.hasLocalName("DefaultValue")) {
                                            String val = value.collectDescendantText();
                                            analyzer.analyzeGMLOutputFormat(val);
                                        }
                                        value = value.advance();
                                    }
                                }
                            }
                        }
                        dcp = dcp.advance();
                    }
                } else if (op.equals("outputFormat")) {
                    SMInputCursor value = operationsMetadata.childElementCursor().advance();
                    while (value.readerAccessible()) {

                        if (value.hasLocalName("AllowedValues")) {
                            value = value.childElementCursor().advance();
                        }

                        if (value.hasLocalName("Value") || value.hasLocalName("DefaultValue")) {
                            String val = value.collectDescendantText();
                            analyzer.analyzeGMLOutputFormat(val);
                        }
                        value = value.advance();
                    }
                }
            }
            operationsMetadata = operationsMetadata.advance();
        }
    }

    private void parseFeatureTypes(SMInputCursor cursor) throws XMLStreamException {
        SMInputCursor featureTypes = cursor.descendantElementCursor().advance();
        while (featureTypes.readerAccessible()) {

            if (featureTypes.hasLocalName("FeatureType")) {
                analyzer.analyzeNamespaces(featureTypes.getStreamReader());
            } else if (featureTypes.hasLocalName("Name")) {
                analyzer.analyzeNamespaces(featureTypes.getStreamReader());
                analyzer.analyzeFeatureType(featureTypes.collectDescendantText());
            } else if (featureTypes.hasLocalName("WGS84BoundingBox")) {
                parseBoundingBox(featureTypes);
            } else if (featureTypes.hasLocalName("LatLongBoundingBox")) {

                if (featureTypes.getCurrEvent() == SMEvent.START_ELEMENT) {
                    parseLatLongBoundingBox(featureTypes);
                }

            } else if (featureTypes.hasLocalName("DefaultCRS") || featureTypes.hasLocalName("DefaultSRS")) {
                parseDefaultCRS(featureTypes);
            } else if (featureTypes.hasLocalName("OtherCRS") || featureTypes.hasLocalName("OtherSRS") || featureTypes.hasLocalName("SRS")) {
                parseOtherCRS(featureTypes);
            }
            featureTypes = featureTypes.advance();
        }
    }

    private void parseDefaultCRS(SMInputCursor cursor) throws XMLStreamException {
        analyzer.analyzeDefaultSRS(cursor.getElemStringValue());
    }

    private void parseOtherCRS(SMInputCursor cursor) throws XMLStreamException {
        analyzer.analyzeOtherSRS(cursor.getElemStringValue());
    }

    private void parseBoundingBox(SMInputCursor cursor) throws XMLStreamException {

        SMInputCursor bbox = cursor.childElementCursor().advance();
        String bblower = null;
        String bbupper = null;

        while (bbox.readerAccessible()) {
            if (bbox.hasLocalName("LowerCorner")) {
                bblower = bbox.getElemStringValue();
            }
            if (bbox.hasLocalName("UpperCorner")) {
                bbupper = bbox.getElemStringValue();
            }
            bbox = bbox.advance();
        }

        analyzer.analyzeBoundingBox(bblower, bbupper);
    }

    private void parseLatLongBoundingBox(SMInputCursor cursor) throws XMLStreamException {

        String xmin = cursor.getAttrValue("minx");
        String ymin = cursor.getAttrValue("miny");
        String xmax = cursor.getAttrValue("maxx");
        String ymax = cursor.getAttrValue("maxy");

        analyzer.analyzeBoundingBox(xmin, ymin, xmax, ymax);
    }
}
