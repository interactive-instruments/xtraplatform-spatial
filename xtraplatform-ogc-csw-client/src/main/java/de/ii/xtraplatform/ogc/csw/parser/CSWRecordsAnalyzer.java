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
