/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import org.immutables.value.Value;

import java.util.OptionalLong;

@Value.Immutable
@Value.Modifiable
@Value.Style(set = "*")
public interface CollectionMetadata {

    @Value.Parameter
    OptionalLong getNumberReturned();

    @Value.Parameter
    OptionalLong getNumberMatched();

    @Value.Default
    default boolean isSingleFeature() {
        return false;
    }
}
