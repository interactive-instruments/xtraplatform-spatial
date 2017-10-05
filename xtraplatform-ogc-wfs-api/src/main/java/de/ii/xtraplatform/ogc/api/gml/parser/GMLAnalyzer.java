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
    public void analyzeStart(Future<SMInputCursor> rootFuture);
    public void analyzeEnd();
    public void analyzeFeatureStart(String id, String nsuri, String localName);
    public void analyzeFeatureEnd();
    public void analyzeAttribute(String nsuri, String localName, String value);
    public void analyzePropertyStart(String nsuri, String localName, int depth, SMInputCursor feature, boolean nil);
    public void analyzePropertyText(String nsuri, String localName, int depth, String text);
    public void analyzePropertyEnd(String nsuri, String localName, int depth);   
    public void analyzeFailed(Exception ex);
}
