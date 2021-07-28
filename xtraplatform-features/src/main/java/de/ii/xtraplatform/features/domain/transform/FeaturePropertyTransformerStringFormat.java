/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import java.util.Map;
import java.util.Objects;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerStringFormat extends FeaturePropertyValueTransformer {

    String TYPE = "STRING_FORMAT";
    String DEFAULT_SUBSTITUTION_KEY = "value";

    @Override
    default String getType() {
        return TYPE;
    }

    Map<String, String> getSubstitutions();

    @Override
    default String transform(String input) {
        return StringTemplateFilters.applyTemplate(getParameter(), key -> Objects.isNull(key)
            ? null
            : Objects.equals(key, DEFAULT_SUBSTITUTION_KEY) || getPropertyName().filter(key::equals).isPresent()
                ? input
                : getSubstitutions().get(key));
    }
}
