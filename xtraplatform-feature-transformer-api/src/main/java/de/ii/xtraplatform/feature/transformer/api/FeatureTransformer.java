/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.query.api.FeatureConsumer;
import de.ii.xtraplatform.feature.query.api.TargetMapping;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author zahnen
 */
public interface FeatureTransformer extends FeatureConsumer {
    String getTargetFormat();

    void onStart(OptionalInt numberReturned, OptionalInt numberMatched) throws Exception;

    void onEnd() throws Exception;

    void onFeatureStart(final TargetMapping mapping) throws Exception;

    void onFeatureEnd() throws Exception;

    void onAttribute(final TargetMapping mapping, final String value) throws Exception;

    void onPropertyStart(final TargetMapping mapping) throws Exception;

    void onPropertyText(final String text) throws Exception;

    void onPropertyEnd() throws Exception;

    void onGeometryStart(final TargetMapping mapping, final GmlFeatureTypeAnalyzer.GML_GEOMETRY_TYPE type, final Integer dimension) throws Exception;

    void onGeometryNestedStart() throws Exception;

    void onGeometryCoordinates(final String text) throws Exception;

    void onGeometryNestedEnd() throws Exception;

    void onGeometryEnd() throws Exception;
}
