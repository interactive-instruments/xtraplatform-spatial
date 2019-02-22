/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.parser;

import de.ii.xtraplatform.ogc.api.WFS;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMInputCursor;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

/**
 * @author zahnen
 */
public class GMLDictionaryParser {
    private final GMLDictionaryAnalyzer analyzer;
    private final SMInputFactory staxFactory;

    public GMLDictionaryParser(GMLDictionaryAnalyzer analyzer, SMInputFactory staxFactory) {
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

            SMInputCursor dictionaryChild = root.childElementCursor().advance();

            while (dictionaryChild.readerAccessible()) {

                switch (dictionaryChild.getLocalName()) {
                    case "identifier":
                        analyzer.analyzeIdentifier(dictionaryChild.collectDescendantText().trim());
                        break;
                    case "name":
                        analyzer.analyzeName(dictionaryChild.collectDescendantText().trim());
                        break;
                    case "dictionaryEntry":
                        parseEntry(dictionaryChild);
                        break;
                }

                dictionaryChild = dictionaryChild.advance();
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

    private void parseEntry(SMInputCursor cursor) throws XMLStreamException {

        analyzer.analyzeEntryStart();

        SMInputCursor entryChild = cursor.childElementCursor().advance();

        while (entryChild.readerAccessible()) {

            switch (entryChild.getLocalName()) {
                case "Definition":
                    parseDefinition(entryChild);
                    break;
            }

            entryChild = entryChild.advance();
        }

        analyzer.analyzeEntryEnd();
    }

    private void parseDefinition(SMInputCursor cursor) throws XMLStreamException {

        analyzer.analyzeEntryStart();

        SMInputCursor definitionChild = cursor.childElementCursor().advance();

        while (definitionChild.readerAccessible()) {

            switch (definitionChild.getLocalName()) {
                case "identifier":
                    analyzer.analyzeEntryIdentifier(definitionChild.collectDescendantText().trim());
                    break;
                case "name":
                    analyzer.analyzeEntryName(definitionChild.collectDescendantText().trim());
                    break;
                case "description":
                    analyzer.analyzeEntryDescription(definitionChild.collectDescendantText().trim());
                    break;
            }

            definitionChild = definitionChild.advance();
        }

        analyzer.analyzeEntryEnd();
    }
}
