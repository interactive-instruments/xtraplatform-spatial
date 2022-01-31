/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.provider.wfs.infra.WfsConnectorHttp;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import javax.annotation.Nullable;
import org.immutables.value.Value;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableConnectionInfoWfsHttp.Builder.class)
public interface ConnectionInfoWfsHttp extends ConnectionInfo, WfsInfo {

    enum METHOD {GET,POST}

    @Override
    @Value.Derived
    default String getConnectorType() {
        return WfsConnectorHttp.CONNECTOR_TYPE;
    }

    @Nullable
    URI getUri();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    default METHOD getMethod() {
        return METHOD.GET;
    }

    Optional<String> getUser();

    Optional<String> getPassword();

    Map<String,String> getOtherUrls();
}
