/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogc.wfs.proxy;

import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_TYPE;

/**
 * @author zahnen
 */
public interface WfsProxyMappingProvider {
    String getTargetType();

    TargetMapping getTargetMappingForFeatureType(String nsUri, String localName);

    TargetMapping getTargetMappingForAttribute(String path, String nsUri, String localName, GML_TYPE type);

    TargetMapping getTargetMappingForProperty(String path, String nsUri, String localName, GML_TYPE type);

    TargetMapping getTargetMappingForGeometry(String path, String nsUri, String localName, GML_GEOMETRY_TYPE type);
}
