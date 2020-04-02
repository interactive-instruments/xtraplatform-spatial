/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableFeatureActionTrigger.class)
public abstract class FeatureActionTrigger {

    public abstract List<String> getOnDelete();

    public List<String> getOnDelete(String id) {
        return getOnDelete().stream()
                            .map(query -> query.replaceAll("\\{\\{id\\}\\}", id))
                            .collect(Collectors.toList());
    }
}
