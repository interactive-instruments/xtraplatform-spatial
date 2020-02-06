/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.feature.provider.sql.ImmutableOptions;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import org.immutables.value.Value;

import java.util.List;

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

    List<String> getSchemas();

    @Value.Default
    default Dialect getDialect() {
        return Dialect.PGIS;
    }

    @Value.Default
    default SqlPathSyntax.Options getPathSyntax() {
        return new ImmutableOptions.Builder().build();
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    default int getMaxThreads() {
        return 16;
    }
}
