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
package de.ii.ogc.wfs.proxy;

import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.util.xml.XMLPathTracker;

/**
 * @author zahnen
 */
public interface WfsProxyOnTheFlyMapping {
    TargetMapping getTargetMappingForFeatureType(XMLPathTracker path, String nsuri, String localName);

    TargetMapping getTargetMappingForAttribute(XMLPathTracker path, String nsuri, String localName, String value);

    TargetMapping getTargetMappingForProperty(XMLPathTracker path, String nsuri, String localName, String value);

    TargetMapping getTargetMappingForGeometry(XMLPathTracker path, String nsuri, String localName);
}
