/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.provider.api.ConnectionInfo;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableConnectionInfoSql.Builder.class)
public interface ConnectionInfoSql extends ConnectionInfo {

    enum Dialect {PGIS}

    String getHost();

    String getDatabase();

    String getUser();

    String getPassword();

    @Value.Default
    default Dialect getDialect() {
        return Dialect.PGIS;
    }

    @Value.Default
    default int getMaxThreads() {
        return 16;
    }
}
