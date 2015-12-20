package de.ii.xtraplatform.ogc.api.gml.parser;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.exceptions.GMLAnalyzeFailed;
import de.ii.xtraplatform.ogc.api.exceptions.WFSException;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import de.ii.xtraplatform.ogc.api.GML;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMFlatteningCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public class GMLParser {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(GMLParser.class);
    private final GMLAnalyzer analyzer;
    private final SMInputFactory staxFactory;
    private int featureDepth;

    public GMLParser(GMLAnalyzer analyzer, SMInputFactory staxFactory) {
        this.analyzer = analyzer;
        this.staxFactory = staxFactory;
        this.featureDepth = 1;
    }

    public void parse(ListenableFuture<HttpEntity> entity, String ns, String ft) throws ExecutionException {

        QName featureType = new QName(ns, ft);

        LOGGER.debug(FrameworkMessages.PARSING_GETFEATURE_RESPONSE_FOR, ft);

        ListenableFuture<SMInputCursor> rootFuture = Futures.transform(entity, new Function<HttpEntity, SMInputCursor>() {
            @Override
            public SMInputCursor apply(HttpEntity e) {
                try {
                    return staxFactory.rootElementCursor(e.getContent()).advance();
                } catch (IOException | IllegalStateException | XMLStreamException ex) {
                    LOGGER.debug(FrameworkMessages.ERROR_PARSING_WFS_GETFEATURE, ex.getMessage());
                }

                return null;
            }
        });

        SMInputCursor root = null;
        try {

            analyzer.analyzeStart(rootFuture);

            root = rootFuture.get();
            if (root == null) {
                return;
            }

            LOGGER.debug(FrameworkMessages.PARSING_GETFEATURE_RESPONSE_FOR, ft);

            // parse for exceptions
            if (root.getLocalName().equals("ExceptionReport")) {
                parseException(root);
            }

            if (root.hasName(featureType)) {
                parseFeature(root);
            } /*else if (root.getLocalName().equals("ValueCollection")) {
                parseValueCollection(root);
            }*/ else {
                SMInputCursor body = root.descendantElementCursor().advance();
                while (body.readerAccessible()) {
                    if (body.hasName(featureType)) {
                        parseFeature(body);
                    }
                    body = body.advance();
                }
            }

            analyzer.analyzeEnd();
        } catch (GMLAnalyzeFailed ex) {
            LOGGER.debug(FrameworkMessages.GMLPARSER_RECIEVED_STOP_PARSING, ex.getMessage());
        } catch (XMLStreamException ex) {
            LOGGER.debug(FrameworkMessages.ERROR_PARSING_WFS_GETFEATURE, ex.getMessage());
        } catch (Exception ex) {
            LOGGER.debug(FrameworkMessages.ERROR_PARSING_WFS_GETFEATURE, ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                EntityUtils.consumeQuietly(entity.get());
            } catch (InterruptedException ex) {

            }
            if (root != null) {
                try {
                    root.getStreamReader().closeCompletely();
                } catch (XMLStreamException ex) {
                }
            }
        }
    }
    /*
     public void parse(HttpEntity entity) {
     LOGGER.getLogger().debug("Parsing GetFeature response");

     SMInputCursor root = null;
     try {
     root = staxFactory.rootElementCursor(entity.getContent()).advance();

     if (root.getLocalName().equals("FeatureCollection")) {
     parseFeatureCollection(root);
     }

     analyzer.analyzeStart(root);

     analyzer.analyzeEnd();
     } catch (IOException ex) {
     LOGGER.debug(ERROR_PARSING_WFS_GETFEATURE, ex);
     throw new WebApplicationException();
     } catch (XMLStreamException ex) {
     LOGGER.debug(ERROR_PARSING_WFS_GETFEATURE, ex);
     throw new WebApplicationException();
     } finally {
     EntityUtils.consumeQuietly(entity);
     if (root != null) {
     try {
     root.getStreamReader().closeCompletely();
     } catch (XMLStreamException ex) {
     }
     }
     }
     }*/
    /*
     private void parseFeatureCollection(SMInputCursor cursor) throws XMLStreamException {

     SMInputCursor body = cursor.descendantElementCursor().advance();

     while (body.readerAccessible()) {

     //LOGGER.debug("lastElementName {} ", body.getLocalName());
     if (body.getLocalName().equals("member") || body.getLocalName().equals("featureMember")) {
     SMInputCursor member = body.descendantElementCursor().advance();
     while (member.readerAccessible()) {
     if (member.getLocalName().equals("FeatureCollection")) {
     parseFeatureCollection(member);
     } else {
     parseFeature(member);
     }
     member = member.advance();
     }
     }
     body = body.advance();
     }
     }*/

    private void parseFeature(SMInputCursor cursor) throws XMLStreamException {

        // TODO: should be determined during analysis
        String id = cursor.getAttrValue(GML.getNS(GML.VERSION._3_2_1), "id");
        if (id == null) {
            id = cursor.getAttrValue(GML.getNS(GML.VERSION._3_1_1), "id");
        }
        if (id == null) {
            id = cursor.getAttrValue("fid");
        }

        analyzer.analyzeFeatureStart(id, cursor.getNsUri(), cursor.getLocalName());
        featureDepth = cursor.getParentCount();
        for (int i = 0; i < cursor.getAttrCount(); i++) {
            analyzer.analyzeAttribute(cursor.getAttrNsUri(i), cursor.getAttrLocalName(i), cursor.getAttrValue(i));
        }
        SMFlatteningCursor feature = (SMFlatteningCursor) cursor.descendantElementCursor().advance();
        while (feature.readerAccessible()) {
            if (feature.getCurrEventCode() == XMLStreamConstants.START_ELEMENT) {
                boolean nil = false;

                for (int i = 0; i < feature.getAttrCount(); i++) {
                    if (feature.getAttrNsUri(i).equals("http://www.w3.org/2001/XMLSchema-instance")
                            && feature.getAttrLocalName(i).equals("nil") && feature.getAttrValue(i).equals("true")) {
                        nil = true;
                    }
                    analyzer.analyzeAttribute(feature.getAttrNsUri(i), feature.getAttrLocalName(i), feature.getAttrValue(i));
                }

                analyzer.analyzePropertyStart(feature.getNsUri(), feature.getLocalName(), feature.getParentCount() - featureDepth, feature, nil);

            } else if (feature.getCurrEventCode() == XMLStreamConstants.END_ELEMENT) {
                analyzer.analyzePropertyEnd(feature.getNsUri(), feature.getLocalName(), feature.getParentCount() - featureDepth);
            }
            feature = (SMFlatteningCursor) feature.advance();
        }
        analyzer.analyzeFeatureEnd();
    }

    private void parseValueCollection(SMInputCursor cursor) throws XMLStreamException {
        SMFlatteningCursor feature = (SMFlatteningCursor) cursor.descendantElementCursor().advance();
        while (feature.readerAccessible()) {
            
            // TODO: this is only working for WFS 2.0 and GML 3.2, 
            // this is done because the required information is missing in the WFS response.
            
            String txt = feature.collectDescendantText();
            analyzer.analyzeFeatureStart("", null, null);
            analyzer.analyzeAttribute("http://www.opengis.net/gml/3.2", "id", txt);
            analyzer.analyzeFeatureEnd();
            
            feature = (SMFlatteningCursor) feature.advance();
        }         
    }

    private void parseException(SMInputCursor cursor) throws XMLStreamException {

        //SMInputCursor body = cursor.descendantElementCursor().advance();
        SMFlatteningCursor body = (SMFlatteningCursor) cursor.descendantElementCursor().advance();

        String exceptionCode = "";
        String exceptionText = "";

        while (body.readerAccessible()) {
            if (body.getCurrEventCode() == XMLStreamConstants.START_ELEMENT) {
                if (body.getLocalName().equals("Exception")) {
                    exceptionCode = body.getAttrValue("exceptionCode");
                }
                if (body.getLocalName().equals("ExceptionText")) {
                    exceptionText = body.collectDescendantText();
                }
            }
            body = (SMFlatteningCursor) body.advance();
        }

        LOGGER.error(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionCode + " " + exceptionText + " GMLParser");
        WFSException wfse = new WFSException(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionCode);
        wfse.addDetail(exceptionText);
        throw wfse;
    }
}
