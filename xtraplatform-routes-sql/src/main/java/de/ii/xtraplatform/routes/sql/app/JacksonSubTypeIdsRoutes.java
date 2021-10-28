/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.routes.sql.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.dropwizard.domain.JacksonSubTypeIds;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import de.ii.xtraplatform.routes.sql.domain.RoutesConfiguration;
import java.util.Map;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class JacksonSubTypeIdsRoutes implements JacksonSubTypeIds {
    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(RoutesConfiguration.class, ExtensionConfiguration.getIdentifier(
                    RoutesConfiguration.class))
                .build();
    }
}
