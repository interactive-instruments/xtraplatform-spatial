/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.api.gml.parser;

import org.codehaus.staxmate.in.SMInputCursor;

import java.util.concurrent.Future;

/**
 * @author zahnen
 */
public class MultiGMLAnalyzer implements GMLAnalyzer {

    private final GMLAnalyzer[] analyzers;

    public MultiGMLAnalyzer(GMLAnalyzer... analyzers) {
        this.analyzers = analyzers;
    }

    @Override
    public void analyzeStart(Future<SMInputCursor> rootFuture) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeStart(rootFuture);
        }
    }

    @Override
    public void analyzeEnd() {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeEnd();
        }
    }

    @Override
    public void analyzeFeatureStart(String id, String nsUri, String localName) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureStart(id, nsUri, localName);
        }
    }

    @Override
    public void analyzeFeatureEnd() {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureEnd();
        }
    }

    @Override
    public void analyzeAttribute(String nsUri, String localName, String value) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeAttribute(nsUri, localName, value);
        }
    }

    @Override
    public void analyzePropertyStart(String nsUri, String localName, int depth, SMInputCursor feature, boolean nil) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzePropertyStart(nsUri, localName, depth, feature, nil);
        }
    }

    @Override
    public void analyzePropertyText(String nsUri, String localName, int depth, String text) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzePropertyText(nsUri, localName, depth, text);
        }
    }

    @Override
    public void analyzePropertyEnd(String nsUri, String localName, int depth) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzePropertyEnd(nsUri, localName, depth);
        }
    }

    @Override
    public void analyzeFailed(Exception ex) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeFailed(ex);
        }
    }

    @Override
    public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeNamespaceRewrite(oldNamespace, newNamespace, featureTypeName);
        }
        return false;
    }
}
