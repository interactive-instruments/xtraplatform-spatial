/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import java.util.Map;
import java.util.Map.Entry;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerStringFormat extends FeaturePropertyValueTransformer {

    String TYPE = "STRING_FORMAT";

    @Override
    default String getType() {
        return TYPE;
    }

    Map<String, String> getSubstitutions();

    //TODO: double cols
    @Override
    default String transform(String input) {
        //boolean more = false;
        //if (currentFormatter == null) {

        String formattedValue = StringTemplateFilters.applyTemplate(getParameter(), input);

        for (Entry<String, String> entry : getSubstitutions().entrySet()) {
            formattedValue = formattedValue.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        int subst = formattedValue.indexOf("}}");
        if (subst > -1) {
            formattedValue = formattedValue.substring(0, formattedValue.indexOf("{{")) + input + formattedValue.substring(subst + 2);
            //more = formattedValue.contains("}}");
        }
        /*} else {
            int subst = currentFormatter.indexOf("}}");
            if (subst > -1) {
                property.value = currentFormatter.substring(0, currentFormatter.indexOf("{{")) + value + currentFormatter.substring(subst + 2);
                more = property.value.contains("}}");
            }
        }
        if (more) {
            this.currentFormatter = property.value;
            return;
        } else {
            currentFormatter = null;
        }*/

        return formattedValue;
    }
}
