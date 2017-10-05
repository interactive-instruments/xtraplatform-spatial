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
