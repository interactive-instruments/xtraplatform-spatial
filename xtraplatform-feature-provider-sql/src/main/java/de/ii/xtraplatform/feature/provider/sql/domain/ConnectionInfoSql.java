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
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableConnectionInfoSql.Builder;
import de.ii.xtraplatform.feature.provider.sql.infra.db.SqlConnectorSlick;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.MergeableMapEncodingEnabled;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableConnectionInfoSql.Builder.class)
@MergeableMapEncodingEnabled
public interface ConnectionInfoSql extends ConnectionInfo {

    enum Dialect {PGIS,GPKG}

    @Override
    @Value.Derived
    default String getConnectorType() {
        return SqlConnectorSlick.CONNECTOR_TYPE;
    }

    @Value.Default
    default Dialect getDialect() {
        return Dialect.PGIS;
    }

    String getDatabase();

    Optional<String> getHost();

    Optional<String> getUser();

    Optional<String> getPassword();

    List<String> getSchemas();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    //@Value.Default
    //can't use interface, bug in immutables when using attributeBuilderDetection and Default
    //default PoolSettings getPool() {
    //    return new ImmutablePoolSettings.Builder().build();
    //}
    @Nullable
    PoolSettings getPool();

    Map<String,Object> getDriverOptions();

    Optional<FeatureActionTrigger> getTriggers();

    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonAlias("maxThreads")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    OptionalInt getMaxConnections();

    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    OptionalInt getMinConnections();

    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    Optional<Boolean> getInitFailFast();

    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    Optional<Boolean> getComputeNumberMatched();

    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    Optional<SqlPathDefaults> getPathSyntax();

    @Value.Check
    default ConnectionInfoSql initNestedDefault() {
        boolean poolIsNull = Objects.isNull(getPool());
        boolean maxConnectionsDiffers =
            !poolIsNull && getMaxConnections().isPresent()
                && !Objects.equals(getMaxConnections().getAsInt(), getPool().getMaxConnections());
        boolean minConnectionsDiffers =
            !poolIsNull && getMinConnections().isPresent()
                && !Objects.equals(getMinConnections().getAsInt(), getPool().getMinConnections());
        boolean initFailFastDiffers =
            !poolIsNull && getInitFailFast().isPresent()
                && !Objects.equals(getInitFailFast().get(), getPool().getInitFailFast());

        if (poolIsNull || maxConnectionsDiffers || minConnectionsDiffers || initFailFastDiffers) {
            Builder builder = new Builder().from(this);
            ImmutablePoolSettings.Builder poolBuilder = builder.poolBuilder();

            if (maxConnectionsDiffers) {
                getMaxConnections().ifPresent(poolBuilder::maxConnections);
            }
            if (minConnectionsDiffers) {
                getMinConnections().ifPresent(poolBuilder::minConnections);
            }
            if (initFailFastDiffers) {
                getInitFailFast().ifPresent(poolBuilder::initFailFast);
            }

            return builder.build();
        }

        return this;
    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutablePoolSettings.Builder.class)
    interface PoolSettings {

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
