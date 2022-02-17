/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.JacksonSubTypeIds;

import java.util.Map;

/**
 * @author zahnen
 */
public class TargetMappingWfs3Register implements JacksonSubTypeIds {

    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(TargetMappingWfs3.class, "WFS3")
                .put(TargetMappingTestGeneric.class, "GENERIC_PROPERTY")
                .put(TargetMappingTestMicrodata.class, "MICRODATA_PROPERTY")
                .put(TargetMappingTestGeoJson.class, "GEO_JSON_PROPERTY")
                .put(TargetMappingTestMicrodataGeo.class, "MICRODATA_GEOMETRY")
                .put(TargetMappingTestGeoJsonGEO.class, "GEO_JSON_GEOMETRY")

                .build();
    }
}
