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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xtraplatform.jackson.dynamic.DynamicTypeIdResolver;

/**
 * @author zahnen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "mappingType")
@JsonTypeIdResolver(DynamicTypeIdResolver.class)
public interface TargetMapping {
    final String BASE_TYPE = "general";

    String getName();

    Boolean isEnabled();

    @JsonIgnore
    boolean isGeometry();

    @JsonIgnore
    TargetMapping mergeCopyWithBase(TargetMapping targetMapping);
}
