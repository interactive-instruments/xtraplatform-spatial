/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.provider.api.ConnectionInfo;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableConnectionInfoPgis.Builder.class)
public abstract class ConnectionInfoPgis implements ConnectionInfo {

    public abstract String getHost();
    public abstract String getDatabase();
    public abstract String getUser();
    public abstract String getPassword();
}
