/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.provider.api.ConnectionInfo;
import org.immutables.value.Value;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
//@Value.Modifiable
//@JsonDeserialize(as = ModifiableConnectionInfoWfs.class)
@Value.Style(builder = "new")
//@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableConnectionInfoWfsHttp.Builder.class)
public abstract class ConnectionInfoWfsHttp extends WfsInfo implements ConnectionInfo {

    public enum METHOD {GET,POST}

    public abstract URI getUri();
    public abstract METHOD getMethod();
    public abstract Optional<String> getUser();
    public abstract Optional<String> getPassword();
    public abstract Map<String,String> getOtherUrls();
}
