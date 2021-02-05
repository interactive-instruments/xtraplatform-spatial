/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.io.IOException;

public interface FeatureProcessor<T extends PropertyBase<T,W>, U extends FeatureBase<T,W>, W extends SchemaBase<W>> {

    U createFeature();

    T createProperty();

    void process(U feature) throws IOException;
}
