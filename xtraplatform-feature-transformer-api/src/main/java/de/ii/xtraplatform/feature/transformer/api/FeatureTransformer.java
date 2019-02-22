/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;

import java.util.List;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public interface FeatureTransformer {
    String getTargetFormat();

    void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception;

    void onEnd() throws Exception;

    void onFeatureStart(final TargetMapping mapping) throws Exception;

    void onFeatureEnd() throws Exception;

    void onPropertyStart(final TargetMapping mapping, List<Integer> multiplicities) throws Exception;

    void onPropertyText(final String text) throws Exception;

    void onPropertyEnd() throws Exception;

    void onGeometryStart(final TargetMapping mapping, final SimpleFeatureGeometry type, final Integer dimension) throws Exception;

    void onGeometryNestedStart() throws Exception;

    void onGeometryCoordinates(final String text) throws Exception;

    void onGeometryNestedEnd() throws Exception;

    void onGeometryEnd() throws Exception;

    interface OnTheFly {
        OnTheFlyMapping getOnTheFlyMapping();
    }
}
