/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableConnectionInfoSql.Builder.class)
public interface ConnectionInfoSql extends ConnectionInfo {

    enum Dialect {PGIS,GPKG}

    @Value.Default
    default Dialect getDialect() {
        return Dialect.PGIS;
    }

    Optional<String> getHost();

    String getDatabase();

    Optional<String> getUser();

    Optional<String> getPassword();

    List<String> getSchemas();

    @Value.Default
    default boolean getComputeNumberMatched() {
        return true;
    }

    @JsonAlias("pathSyntax")
    @Value.Default
    default SqlPathDefaults getSourcePathDefaults() {
        return new ImmutableSqlPathDefaults.Builder().build();
    }

    Optional<FeatureActionTrigger> getTriggers();

    @Deprecated
    @JsonAlias("maxThreads")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    default int getMaxConnections() {
        return getPool().getMaxConnections();
    }

    @Deprecated
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    default int getMinConnections() {
        return getPool().getMinConnections();
    }

    @Deprecated
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    default boolean getInitFailFast() {
        return getPool().getInitFailFast();
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    default Pool getPool() {
        return new ImmutablePool.Builder().build();
    }

    Map<String,Object> getDriverOptions();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutablePool.Builder.class)
    interface Pool {

        @Value.Default
        default int getMaxConnections() {
            return -1;
        }

        @Value.Default
        default int getMinConnections() {
            return getMaxConnections();
        }

        @Value.Default
        default boolean getInitFailFast() {
            return true;
        }

        @Value.Default
        default String getIdleTimeout() {
            return "10m";
        }

        @Value.Default
        default boolean getReuse() {
            return false;
        }
    }
}
