/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import org.immutables.value.Value;

import java.util.Map;

/**
 * @author zahnen
 */
@Value.Immutable
//@Value.Modifiable
@JsonDeserialize(as = ModifiableSourcePathMapping.class)
public abstract class SourcePathMapping {

    @JsonAnyGetter
    @JsonMerge
    public abstract Map<String, TargetMapping> getMappings();

    public boolean hasMappingForType(String type) {
        return getMappings().containsKey(type);
    }

    public TargetMapping getMappingForType(String type) {
        return getMappings().get(type);
    }
}
