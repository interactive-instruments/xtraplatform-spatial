/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.api;

import java.util.List;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public interface FeatureConsumer {
    void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception;

    void onEnd() throws Exception;

    void onFeatureStart(List<String> path) throws Exception;

    void onFeatureEnd(List<String> path) throws Exception;

    void onPropertyStart(List<String> path, List<Integer> multiplicities) throws Exception;

    void onPropertyText(String text) throws Exception;

    void onPropertyEnd(List<String> path) throws Exception;
}
