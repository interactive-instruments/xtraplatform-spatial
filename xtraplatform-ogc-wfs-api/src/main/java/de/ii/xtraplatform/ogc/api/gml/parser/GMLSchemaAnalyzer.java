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
