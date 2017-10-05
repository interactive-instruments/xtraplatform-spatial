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
public class AbstractCSWRecordsAnalyzer implements CSWRecordsAnalyzer {
    @Override
    public void analyzeStart(SMInputCursor searchResults) {

    }

    @Override
    public void analyzeEnd() {

    }

    @Override
    public void analyzeFailed(Exception ex) {

    }

    @Override
    public void analyzeFailed(String exceptionCode, String exceptionText) {

    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {

    }

    @Override
    public void analyzeRecordStart() {

    }

    @Override
    public void analyzeRecordEnd() {

    }

    @Override
    public void analyzeServiceType(String serviceType) {

    }

    @Override
    public void analyzeServiceTypeVersion(String serviceTypeVersion) {

    }

    @Override
    public void analyzeOperationName(String operationName) {

    }

    @Override
    public void analyzeOperationUrl(String operationUrl) {

    }
}
