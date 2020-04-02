/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.geojson;

import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;

import java.util.List;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
class FeatureConsumerToString implements FeatureConsumer {
    public StringBuilder log = new StringBuilder();

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {
        log.append(String.format("START: %d %d\n", numberReturned.orElse(-1), numberMatched.orElse(-1)));
    }

    @Override
    public void onEnd() throws Exception {
        log.append(String.format("END\n"));
    }

    @Override
    public void onFeatureStart(List<String> path) throws Exception {
        log.append(path);
        log.append("{\n");
    }

    @Override
    public void onFeatureEnd(List<String> path) throws Exception {
        log.append("}\n");
    }

    @Override
    public void onPropertyStart(List<String> path, List<Integer> multiplicities) throws Exception {
        log.append("    ");
        log.append(path);
        log.append(": ");
        log.append(multiplicities);
        log.append(":");
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        log.append(text);
    }

    @Override
    public void onPropertyEnd(List<String> path) throws Exception {
        log.append("\n");
        log.append("   >");
        log.append(path);
        log.append("\n");
    }
}
