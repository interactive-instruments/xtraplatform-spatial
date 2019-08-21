/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xtraplatform.dropwizard.cfg.JacksonProvider;

import java.util.Optional;

/**
 * @author zahnen
 */
//@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "providerType", visible = true)
//@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public abstract class FeatureProviderData {

    public abstract String getProviderType();

    public abstract String getConnectorType();

    @JsonIgnore
    public abstract Optional<String> getDataSourceUrl();
}
