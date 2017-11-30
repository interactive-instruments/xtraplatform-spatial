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
    void analyzeFeatureType(String nsUri, String localName);
    void analyzeAttribute(String nsUri, String localName, String type, boolean required);
    void analyzeProperty(String nsUri, String localName, String type, long minOccurs, long maxOccurs,
            int depth, boolean isParentMultiple, boolean isComplex, boolean isObject);
    boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName);
}
