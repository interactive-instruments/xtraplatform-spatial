/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonMerge;
import de.ii.xtraplatform.service.api.ServiceData;

import java.util.Map;

/**
 * @author zahnen
 */
//@Value.Immutable
//@Value.Modifiable
//@Value.Style(deepImmutablesDetection = true)
//@JsonDeserialize(as = ModifiableFeatureTransformerServiceData.class)
public abstract class FeatureTransformerServiceData<T extends FeatureTypeConfiguration> implements ServiceData {

    public abstract Map<String, T> getFeatureTypes();

    //@JsonIgnore
    @JsonMerge
    public abstract FeatureProviderDataTransformer getFeatureProvider();
}
