/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
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

    URI getUri();

    @Value.Default
    default METHOD getMethod() {
        return METHOD.GET;
    }

    Optional<String> getUser();

    Optional<String> getPassword();

    Map<String,String> getOtherUrls();
}
