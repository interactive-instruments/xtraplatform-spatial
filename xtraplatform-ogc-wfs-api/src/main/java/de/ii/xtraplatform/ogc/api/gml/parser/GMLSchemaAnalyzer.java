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

/**
 *
 * @author zahnen
 */
public interface GMLSchemaAnalyzer {
    public void analyzeFeatureType(String nsuri, String localName);
    public void analyzeAttribute(String nsuri, String localName, String type, boolean required);
    public void analyzeProperty(String nsuri, String localName, String type, long minOccurs, long maxOccurs, 
            int depth, boolean isParentMultible, boolean isComplex, boolean isObject);
    public void analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName);
}
