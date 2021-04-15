/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import org.immutables.value.Value;

public interface ConnectionInfo {

    Optional<String> getConnectionUri();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    String getConnectorType();
}
