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
    public void analyzeFeatureStart(String id, String nsuri, String localName) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureStart(id, nsuri, localName);
        }
    }

    @Override
    public void analyzeFeatureEnd() {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeFeatureEnd();
        }
    }

    @Override
    public void analyzeAttribute(String nsuri, String localName, String value) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeAttribute(nsuri, localName, value);
        }
    }

    @Override
    public void analyzePropertyStart(String nsuri, String localName, int depth, SMInputCursor feature, boolean nil) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzePropertyStart(nsuri, localName, depth, feature, nil);
        }
    }

    @Override
    public void analyzePropertyText(String nsuri, String localName, int depth, String text) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzePropertyText(nsuri, localName, depth, text);
        }
    }

    @Override
    public void analyzePropertyEnd(String nsuri, String localName, int depth) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzePropertyEnd(nsuri, localName, depth);
        }
    }

    @Override
    public void analyzeFailed(Exception ex) {
        for (GMLAnalyzer analyzer: analyzers) {
            analyzer.analyzeFailed(ex);
        }
    }
}
