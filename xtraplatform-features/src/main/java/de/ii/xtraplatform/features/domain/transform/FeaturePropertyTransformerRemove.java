/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureProperty;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public interface FeaturePropertyTransformerRemove extends FeaturePropertySchemaTransformer {

    Logger LOGGER = LoggerFactory.getLogger(FeaturePropertyTransformerRemove.class);

    enum Condition {ALWAYS, OVERVIEW, NEVER;

        @Override
        public String toString() {
            return super.toString();
        }
    }

    String TYPE = "REMOVE";

    @Override
    default String getType() {
        return TYPE;
    }

    boolean isOverview();

    @Override
    default FeatureProperty transform(FeatureProperty input) {
        Condition condition = Condition.NEVER;
        try {
            condition = Condition.valueOf(getParameter().toUpperCase());
        } catch (Throwable e) {
            LOGGER.warn("Skipping {} transformation for property '{}', condition '{}' is not supported. Supported types: {}", getType(), input.getName(), getParameter(), Condition.values());
        }


        if (condition == Condition.ALWAYS || (condition == Condition.OVERVIEW && isOverview())) {
            return null;
        }

        return input;
    }
}
