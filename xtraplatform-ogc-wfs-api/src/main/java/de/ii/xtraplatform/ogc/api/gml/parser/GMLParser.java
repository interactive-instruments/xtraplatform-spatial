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
package de.ii.xtraplatform.ogc.api.gml.parser;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.XSI;
import de.ii.xtraplatform.ogc.api.exceptions.GMLAnalyzeFailed;
import de.ii.xtraplatform.ogc.api.exceptions.WFSException;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMFilterFactory;
import org.codehaus.staxmate.in.SMFlatteningCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author zahnen
 */
public class GMLParser {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(GMLParser.class);
    private final GMLAnalyzer analyzer;
    private final SMInputFactory staxFactory;
    private int featureDepth;
    private boolean parseText;

    public GMLParser(GMLAnalyzer analyzer, SMInputFactory staxFactory) {
        this.analyzer = analyzer;
        this.staxFactory = staxFactory;
        this.featureDepth = 1;
    }

    public void enableTextParsing() {
        this.parseText = true;
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
            if (root.getLocalName().equals(WFS.getWord(WFS.VOCABULARY.EXCEPTION_REPORT))) {
                parseException(root);
            }

            if (root.hasName(featureType)) {
                parseFeature(root);
            } else {
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

    private void parseFeature(SMInputCursor cursor) throws XMLStreamException {

        analyzer.analyzeFeatureStart(null, cursor.getNsUri(), cursor.getLocalName());

        featureDepth = cursor.getParentCount();

        for (int i = 0; i < cursor.getAttrCount(); i++) {
            analyzer.analyzeAttribute(cursor.getAttrNsUri(i), cursor.getAttrLocalName(i), cursor.getAttrValue(i));
        }

        SMFlatteningCursor feature;
        StringBuilder text = null;
        if (parseText) {
            feature = (SMFlatteningCursor) cursor.descendantCursor().advance();
            text = new StringBuilder();
        } else {
            feature = (SMFlatteningCursor) cursor.descendantElementCursor().advance();
        }


        while (feature.readerAccessible()) {
            if (feature.getCurrEventCode() == XMLStreamConstants.START_ELEMENT) {
                boolean nil = false;

                for (int i = 0; i < feature.getAttrCount(); i++) {
                    if (feature.getAttrNsUri(i).equals(XSI.getNS(XSI.VERSION.DEFAULT))
                            && feature.getAttrLocalName(i).equals(XSI.getWord(XSI.VOCABULARY.NIL)) && feature.getAttrValue(i).equals(XSI.getWord(XSI.VOCABULARY.NIL_TRUE))) {
                        nil = true;
                    }
                    analyzer.analyzeAttribute(feature.getAttrNsUri(i), feature.getAttrLocalName(i), feature.getAttrValue(i));
                }

                analyzer.analyzePropertyStart(feature.getNsUri(), feature.getLocalName(), feature.getParentCount() - featureDepth, feature, nil);

            } else if (feature.getCurrEventCode() == XMLStreamConstants.END_ELEMENT) {
                if (parseText && text.length() > 0) {
                    analyzer.analyzePropertyText(feature.getNsUri(), feature.getLocalName(), feature.getParentCount() - featureDepth, text.toString());
                    text = new StringBuilder();
                }
                analyzer.analyzePropertyEnd(feature.getNsUri(), feature.getLocalName(), feature.getParentCount() - featureDepth);
            } else if (parseText && (feature.getCurrEventCode() == XMLStreamConstants.CHARACTERS)) {
                text.append(feature.getText().trim());
            }

            feature = (SMFlatteningCursor) feature.advance();
        }

        analyzer.analyzeFeatureEnd();
    }

    private void parseException(SMInputCursor cursor) throws XMLStreamException {

        SMFlatteningCursor body = (SMFlatteningCursor) cursor.descendantElementCursor().advance();

        String exceptionCode = "";
        String exceptionText = "";

        while (body.readerAccessible()) {
            if (body.getCurrEventCode() == XMLStreamConstants.START_ELEMENT) {
                if (body.getLocalName().equals(WFS.getWord(WFS.VOCABULARY.EXCEPTION))) {
                    exceptionCode = body.getAttrValue(WFS.getWord(WFS.VOCABULARY.EXCEPTION_CODE));
                }
                if (body.getLocalName().equals(WFS.getWord(WFS.VOCABULARY.EXCEPTION_TEXT))) {
                    exceptionText = body.collectDescendantText();
                }
            }
            body = (SMFlatteningCursor) body.advance();
        }

        LOGGER.error(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionCode + " " + exceptionText);
        WFSException wfse = new WFSException(FrameworkMessages.EXCEPTION_COMING_FROM_WFS, exceptionCode);
        wfse.addDetail(exceptionText);
        throw wfse;
    }
}
