/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.domain;

import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface WfsInfo {

    @Value.Default
    default String getVersion() {
        return "2.0.0";
    }

    Optional<String> getGmlVersion();

    Map<String,String> getNamespaces();

}
