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
package de.ii.xtraplatform.ogc.api.gml.parser;

import java.util.concurrent.Future;
import org.codehaus.staxmate.in.SMInputCursor;

/**
 *
 * @author zahnen
 */
public interface GMLAnalyzer {
    void analyzeStart(Future<SMInputCursor> rootFuture);
    void analyzeEnd();
    void analyzeFeatureStart(String id, String nsUri, String localName);
    void analyzeFeatureEnd();
    void analyzeAttribute(String nsUri, String localName, String value);
    void analyzePropertyStart(String nsUri, String localName, int depth, SMInputCursor feature, boolean nil);
    void analyzePropertyText(String nsUri, String localName, int depth, String text);
    void analyzePropertyEnd(String nsUri, String localName, int depth);
    void analyzeFailed(Exception ex);
    boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName);
}
