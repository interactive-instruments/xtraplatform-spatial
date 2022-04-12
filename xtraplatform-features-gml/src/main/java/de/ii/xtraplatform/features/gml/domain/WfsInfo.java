/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.domain;

import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface WfsInfo {

    /**
     * @en The WFS version to use.
     * @de Die zu verwendende WFS-Version.
     * @default 2.0.0
     */
    @Value.Default
    default String getVersion() {
        return "2.0.0";
    }

    /**
     * @en The GML version to use.
     * @de Die zu verwendende GML-Version.
     * @default
     */
    Optional<String> getGmlVersion();

    /**
     * @en A map of namespace prefixes and URIs used in the mapping paths.
     * @de Eine Map von zu verwendenden Namespace-Prefixen und der zugehörigen Namespace-URI.
     * @default
     */
    Map<String,String> getNamespaces();

}
