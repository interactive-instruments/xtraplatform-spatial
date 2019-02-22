/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import java.io.OutputStream;

/**
 * @author zahnen
 */
public interface FeatureTransformerFactory {
    FeatureTransformer create(FeatureTransformerParameter parameter);

    abstract class FeatureTransformerParameter {
        public abstract OutputStream getOutputStream();
    }
}
