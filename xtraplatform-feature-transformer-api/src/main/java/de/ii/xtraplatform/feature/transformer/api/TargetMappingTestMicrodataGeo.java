/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableTargetMappingTestMicrodataGeo.class)
public abstract class TargetMappingTestMicrodataGeo implements TargetMapping<TargetMappingTestMicrodataGeo.WFS3_TYPES> {
    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        return targetMapping;
    }

    @Value.Derived
    @Override
    public boolean isSpatial() {
        return getType() == WFS3_TYPES.SPATIAL;
    }

    public enum WFS3_TYPES {ID,VALUE,SPATIAL,TEMPORAL,STRING,NUMBER,BOOLEAN,DATE,GEOMETRY,REFERENCE}

}
