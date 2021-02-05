/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.NotUsed;
import akka.stream.javadsl.Flow;

public interface FeatureProcessor2<T extends PropertyBase<T,W>, U extends FeatureBase<T,W>, V, W extends SchemaBase<W>> {

    U createFeature();

    T createProperty();

    V process(U feature);

    default Flow<U,V, NotUsed> getFlow() {
        return Flow.fromFunction(this::process);
    }
}
