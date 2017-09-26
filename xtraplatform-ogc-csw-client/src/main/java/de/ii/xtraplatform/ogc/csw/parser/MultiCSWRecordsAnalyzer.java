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

import org.codehaus.staxmate.in.SMInputCursor;

/**
 * @author zahnen
 */
public class MultiCSWRecordsAnalyzer implements CSWRecordsAnalyzer {

    private final CSWRecordsAnalyzer[] analyzers;

    public MultiCSWRecordsAnalyzer(CSWRecordsAnalyzer... analyzers) {
        this.analyzers = analyzers;
    }

    @Override
    public void analyzeStart(SMInputCursor searchResults) {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeStart(searchResults);
        }
    }

    @Override
    public void analyzeEnd() {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeEnd();
        }
    }

    @Override
    public void analyzeFailed(Exception ex) {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeFailed(ex);
        }
    }

    @Override
    public void analyzeFailed(String exceptionCode, String exceptionText) {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeFailed(exceptionCode, exceptionText);
        }
    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeNamespace(prefix, uri);
        }
    }

    @Override
    public void analyzeRecordStart() {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeRecordStart();
        }
    }

    @Override
    public void analyzeRecordEnd() {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeRecordEnd();
        }
    }

    @Override
    public void analyzeServiceType(String serviceType) {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceType(serviceType);
        }
    }

    @Override
    public void analyzeServiceTypeVersion(String serviceTypeVersion) {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeServiceTypeVersion(serviceTypeVersion);
        }
    }

    @Override
    public void analyzeOperationName(String operationName) {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeOperationName(operationName);
        }
    }

    @Override
    public void analyzeOperationUrl(String operationUrl) {
        for (CSWRecordsAnalyzer analyzer: analyzers) {
            analyzer.analyzeOperationUrl(operationUrl);
        }
    }
}
