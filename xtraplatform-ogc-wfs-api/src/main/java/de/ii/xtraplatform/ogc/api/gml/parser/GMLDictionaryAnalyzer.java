package de.ii.xtraplatform.ogc.api.gml.parser;

import org.codehaus.staxmate.in.SMInputCursor;

/**
 * @author zahnen
 */
public interface GMLDictionaryAnalyzer {
    void analyzeStart(SMInputCursor searchResults);
    void analyzeEnd();
    void analyzeFailed(Exception ex);
    void analyzeFailed(String exceptionCode, String exceptionText);

    void analyzeNamespace(String prefix, String uri);

    void analyzeIdentifier(String identifier);
    void analyzeName(String name);

    void analyzeEntryStart();
    void analyzeEntryEnd();

    void analyzeEntryIdentifier(String identifier);
    void analyzeEntryName(String name);
    void analyzeEntryDescription(String description);
}
