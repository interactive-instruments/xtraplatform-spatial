/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.parser;

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
