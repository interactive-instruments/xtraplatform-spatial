/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xsf.core.api.Service;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;

import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface FeatureTransformerService extends Service {
    // TODO: FeatureProvider and Transformer
    WFSAdapter getWfsAdapter();

    FeatureTransformerServiceProperties getServiceProperties();

    Map<String, FeatureTypeConfigurationOld> getFeatureTypes();

    @JsonIgnore
    Optional<FeatureTypeConfigurationOld> getFeatureTypeByName(String name);

    FeatureProvider getFeatureProvider();
}
