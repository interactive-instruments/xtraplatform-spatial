/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.geojson;

import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;

import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public class FeatureTransformerToString implements FeatureTransformer {
    private final StringBuilder log = new StringBuilder();

    @Override
    public String toString() {
        return log.toString();
    }

    @Override
    public String getTargetFormat() {
        return "SQL";
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {
        log.append(String.format("START: %d %d\n", numberReturned.orElse(-1), numberMatched.orElse(-1)));
    }

    @Override
    public void onEnd() throws Exception {
        log.append(String.format("END\n"));
    }

    @Override
    public void onFeatureStart(TargetMapping mapping) throws Exception {
        log.append(Objects.nonNull(mapping) ? mapping.getName() : "NOMAPPING");
        log.append("{\n");
    }

    @Override
    public void onFeatureEnd() throws Exception {
        log.append("}\n");
    }

    @Override
    public void onPropertyStart(TargetMapping mapping, List<Integer> multiplicities) throws Exception {
        log.append("    ");
        log.append(Objects.nonNull(mapping) ? mapping.getName() : "NOMAPPING");
        if (Objects.nonNull(mapping) && mapping.getName().contains("["))
            log.append("[").append(multiplicities).append("]");
        log.append(":");
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        log.append(text);
    }


    @Override
    public void onPropertyEnd() throws Exception {
        log.append("\n");
    }

    @Override
    public void onGeometryStart(TargetMapping mapping, SimpleFeatureGeometry type, Integer dimension) throws Exception {
        log.append("    ");
        log.append(Objects.nonNull(mapping) ? mapping.getName() : "NOMAPPING");
        log.append("|");
        log.append(type);
        log.append(":");
    }

    @Override
    public void onGeometryNestedStart() throws Exception {
        log.append("\nNESTOPEN");
    }

    @Override
    public void onGeometryCoordinates(String text) throws Exception {
        log.append("\n")
           .append(text.trim());
    }

    @Override
    public void onGeometryNestedEnd() throws Exception {
        log.append("\nNESTCLOSE");
    }

    @Override
    public void onGeometryEnd() throws Exception {
        log.append("\n");
    }
}
