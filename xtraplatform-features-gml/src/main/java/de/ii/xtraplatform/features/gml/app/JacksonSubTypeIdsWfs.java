/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.JacksonSubTypeIds;
import de.ii.xtraplatform.features.gml.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.gml.infra.WfsConnectorHttp;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.util.Map;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class JacksonSubTypeIdsWfs implements JacksonSubTypeIds {

    @Inject
    public JacksonSubTypeIdsWfs() {
    }

    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(ConnectionInfoWfsHttp.class, WfsConnectorHttp.CONNECTOR_TYPE)
                .build();
    }
}
