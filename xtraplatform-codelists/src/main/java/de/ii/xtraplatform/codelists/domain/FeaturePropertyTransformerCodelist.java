/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.codelists.domain;

import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Value.Immutable
public interface FeaturePropertyTransformerCodelist extends FeaturePropertyValueTransformer {

    Logger LOGGER = LoggerFactory.getLogger(FeaturePropertyTransformerCodelist.class);
    String TYPE = "CODELIST";

    @Override
    default String getType() {
        return TYPE;
    }

    Map<String, Codelist> getCodelists();

    @Override
    default String transform(String input) {
        if (!getCodelists().containsKey(getParameter())) {
            LOGGER.warn("Skipping {} transformation for property '{}', codelist '{}' not found.", getType(), getPropertyName(), getParameter());

            return input;
        }

        Codelist cl = getCodelists().get(getParameter());
        String resolvedValue = cl.getValue(input);

        if (cl.getData()
              .getSourceType() == CodelistData.IMPORT_TYPE.TEMPLATES) {
            resolvedValue = StringTemplateFilters.applyFilterMarkdown(StringTemplateFilters.applyTemplate(resolvedValue, input));
        }

        return resolvedValue;
    }
}
