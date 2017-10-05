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
package de.ii.xtraplatform.ogc.csw.parser;

import de.ii.xtraplatform.ogc.api.WFS;
import org.codehaus.staxmate.in.SMInputCursor;

/**
 *
 * @author zahnen
 */
public interface CSWRecordsAnalyzer {

    void analyzeStart(SMInputCursor searchResults);
    void analyzeEnd();
    void analyzeFailed(Exception ex);
    void analyzeFailed(String exceptionCode, String exceptionText);

    void analyzeNamespace(String prefix, String uri);

    void analyzeRecordStart();
    void analyzeRecordEnd();

    void analyzeServiceType(String serviceType);
    void analyzeServiceTypeVersion(String serviceTypeVersion);

    void analyzeOperationName(String operationName);
    void analyzeOperationUrl(String operationUrl);
}
