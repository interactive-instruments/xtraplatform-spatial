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
    public void analyzePropertyEnd(String nsuri, String localName, int depth);   
    public void analyzeFailed(Exception ex);
}
