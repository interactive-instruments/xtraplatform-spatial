/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.dropwizard.domain.JacksonSubTypeIds;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.infra.db.SqlConnectorSlick;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Map;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class FeatureProviderRegisterPgis implements JacksonSubTypeIds {
    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                //.put(FeatureProviderDataPgis.class, FeatureProviderPgis.PROVIDER_TYPE)
                .put(ConnectionInfoSql.class, SqlConnectorSlick.CONNECTOR_TYPE)
                .build();
    }
}
