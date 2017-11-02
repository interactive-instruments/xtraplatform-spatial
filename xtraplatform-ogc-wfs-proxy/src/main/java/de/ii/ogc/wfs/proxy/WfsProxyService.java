/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.ogc.wfs.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xsf.core.api.Service;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;

import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface WfsProxyService extends Service {
    WFSAdapter getWfsAdapter();

    WFSProxyServiceProperties getServiceProperties();

    Map<String, WfsProxyFeatureType> getFeatureTypes();

    @JsonIgnore
    Optional<WfsProxyFeatureType> getFeatureTypeByName(String name);
}
