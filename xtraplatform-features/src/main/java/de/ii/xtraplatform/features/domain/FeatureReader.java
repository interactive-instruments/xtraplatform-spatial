/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public interface FeatureReader<T,U> {
    void onStart(OptionalLong numberReturned, OptionalLong numberMatched, T context) throws Exception;

    void onEnd() throws Exception;

    void onFeatureStart(List<String> path, T context) throws Exception;

    void onFeatureEnd(List<String> path) throws Exception;

    void onObjectStart(List<String> path, U context) throws Exception;

    void onObjectEnd(List<String> path, Map<String, String> context) throws Exception;

    void onArrayStart(List<String> path, U context) throws Exception;

    void onArrayEnd(List<String> path, Map<String, String> context) throws Exception;

    void onValue(List<String> path, String value, U context) throws Exception;
}
