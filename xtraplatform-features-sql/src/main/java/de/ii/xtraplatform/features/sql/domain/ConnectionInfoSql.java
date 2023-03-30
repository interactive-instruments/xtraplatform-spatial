/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql.Builder;
import de.ii.xtraplatform.features.sql.infra.db.SqlConnectorRx;
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

/** */
@Value.Immutable
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = DocIgnore.class)
@JsonDeserialize(builder = ImmutableConnectionInfoSql.Builder.class)
@MergeableMapEncodingEnabled
public interface ConnectionInfoSql extends ConnectionInfo {

  enum Dialect {
    PGIS,
    GPKG
  }
  /**
   * @langEn Always `SLICK`.
   * @langDe Stets `SLICK`.
   */
  @Override
  @Value.Derived
  default String getConnectorType() {
    return SqlConnectorRx.CONNECTOR_TYPE;
  }

  /**
   * @langEn `PGIS` for PostgreSQL/PostGIS, `GPKG` for GeoPackage or SQLite/SpatiaLite.
   * @langDe `PGIS` für PostgreSQL/PostGIS, `GPKG` für GeoPackage oder SQLite/SpatiaLite.
   * @default PGIS
   */
  @Value.Default
  default Dialect getDialect() {
    return Dialect.PGIS;
  }

  /**
   * @langEn The name of the database. For `GPKG` the file path, either absolute or relative to the
   *     [data folder](../../application/30-data-folder.md).
   * @langDe Der Name der Datenbank. Für `GPKG` der Pfad zur Datei, entweder absolut oder relativ
   *     zum [Daten-Verzeichnis](../../application/30-data-folder.md).
   */
  String getDatabase();

  /**
   * @langEn The database host. To use a non-default port, add it to the host separated by `:`, e.g.
   *     `db:30305`. Not relevant for `GPKG`.
   * @langDe Der Datenbankhost. Wird ein anderer Port als der Standardport verwendet, ist dieser
   *     durch einen Doppelpunkt getrennt anzugeben, z.B. `db:30305`. Nicht relevant für `GPKG`.
   */
  Optional<String> getHost();

  /**
   * @langEn The user name. Not relevant for `GPKG`.
   * @langDe Der Benutzername. Nicht relevant für `GPKG`.
   */
  Optional<String> getUser();

  /**
   * @langEn The base64 encoded password of the user. Not relevant for `GPKG`.
   * @langDe Das mit base64 kodierte Passwort des Benutzers. Nicht relevant für `GPKG`.
   */
  Optional<String> getPassword();

  /**
   * @langEn The names of database schemas that should be used in addition to `public`. Not relevant
   *     for `GPKG`.
   * @langDe Die Namen der Schemas in der Datenbank, auf die zugegriffen werden soll. Nicht relevant
   *     für `GPKG`.
   * @default []
   */
  List<String> getSchemas();

  /**
   * @langEn Connection pool settings, for details see [Pool](#connection-pool) below.
   * @langDe Einstellungen für den Connection-Pool, für Details siehe [Pool](#connection-pool).
   * @default see below
   */
  @JsonProperty(
      value = "pool",
      access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  // @Value.Default
  // can't use interface, bug in immutables when using attributeBuilderDetection and Default
  // default PoolSettings getPool() {
  //    return new ImmutablePoolSettings.Builder().build();
  // }
  @Nullable
  PoolSettings getPool();

  /**
   * @langEn Custom options for the JDBC driver. For `PGIS`, you might pass `gssEncMode`, `ssl`,
   *     `sslmode`, `sslcert`, `sslkey`, `sslrootcert` and `sslpassword`. For details see the
   *     [driver
   *     documentation](https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters).
   * @langDe Einstellungen für den JDBC-Treiber. Für `PGIS` werden `gssEncMode`, `ssl`, `sslmode`,
   *     `sslcert`, `sslkey`, `sslrootcert` und `sslpassword` durchgereicht. Für Details siehe die
   *     [Dokumentation des
   *     Treibers](https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters).
   * @default {}
   */
  Map<String, Object> getDriverOptions();

  @DocIgnore
  Optional<FeatureActionTrigger> getTriggers();

  @Override
  @JsonIgnore
  @Value.Lazy
  default boolean isShared() {
    return Objects.nonNull(getPool()) && getPool().getShared();
  }

  @Override
  @JsonIgnore
  @Value.Lazy
  default String getDatasetIdentifier() {
    return String.format("%s/%s", getHost().orElse(""), getDatabase());
  }

  /**
   * @langEn *Deprecated* See `pool.maxConnections`.
   * @langDe *Deprecated* Siehe `pool.maxConnections`.
   * @default dynamic
   */
  @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
  @JsonAlias("maxThreads")
  @JsonProperty(
      value = "maxConnections",
      access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  OptionalInt getMaxConnections();

  /**
   * @langEn *Deprecated* See `pool.minConnections`.
   * @langDe *Deprecated* Siehe `pool.minConnections`.
   * @default maxConnections
   */
  @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
  @JsonProperty(
      value = "minConnections",
      access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  OptionalInt getMinConnections();

  /**
   * @langEn *Deprecated* See `pool.initFailFast`.
   * @langDe *Deprecated* Siehe `pool.initFailFast`.
   * @default true
   */
  @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
  @JsonProperty(
      value = "initFailFast",
      access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  Optional<Boolean> getInitFailFast();

  /**
   * @langEn *Deprecated* See [Query Generation](#query-generation) below.
   * @langDe *Deprecated* Siehe [Query-Generierung](#query-generation).
   * @default true
   */
  @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
  @JsonProperty(
      value = "computeNumberMatched",
      access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  Optional<Boolean> getComputeNumberMatched();

  /**
   * @langEn *Deprecated* See [Source Path Defaults](#source-path-defaults) below.
   * @langDe *Deprecated* Siehe [SQL-Pfad-Defaults](#source-path-defaults).
   */
  @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
  @JsonProperty(
      value = "pathSyntax",
      access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  Optional<SqlPathDefaults> getPathSyntax();

  @Value.Check
  default ConnectionInfoSql initNestedDefault() {
    boolean poolIsNull = Objects.isNull(getPool());
    boolean maxConnectionsDiffers =
        !poolIsNull
            && getMaxConnections().isPresent()
            && !Objects.equals(getMaxConnections().getAsInt(), getPool().getMaxConnections());
    boolean minConnectionsDiffers =
        !poolIsNull
            && getMinConnections().isPresent()
            && !Objects.equals(getMinConnections().getAsInt(), getPool().getMinConnections());
    boolean initFailFastDiffers =
        !poolIsNull
            && getInitFailFast().isPresent()
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

    /**
     * @langEn Maximum number of connections to the database. The default value is computed
     *     depending on the number of processor cores and the maximum number of joins per feature
     *     type in the [Types Configuration](README.md#schema-definitions). The default value is
     *     recommended for optimal performance under load. The smallest possible value also depends
     *     on the maximum number of joins per feature type, smaller values are rejected.
     * @langDe Steuert die maximale Anzahl von Verbindungen zur Datenbank. Der Default-Wert ist
     *     abhängig von der Anzahl der Prozessorkerne und der Anzahl der Joins in der
     *     [Types-Konfiguration](README.md#schema-definitions). Der Default-Wert wird für optimale
     *     Performanz unter Last empfohlen. Der kleinstmögliche Wert ist ebenfalls von der Anzahl
     *     der Joins abhängig, kleinere Werte werden zurückgewiesen.
     * @default dynamic
     */
    @Value.Default
    default int getMaxConnections() {
      return -1;
    }

    /**
     * @langEn Minimum number of connections to the database that are maintained.
     * @langDe Steuert die minimale Anzahl von Verbindungen zur Datenbank, die jederzeit offen
     *     gehalten werden.
     * @default maxConnections
     */
    @Value.Default
    default int getMinConnections() {
      return getMaxConnections();
    }

    /**
     * @langEn If disabled the provider will wait longer for the first database connection to be
     *     established. Has no effect if `minConnections` is `0`. Should normally be disabled only
     *     on development systems.
     * @langDe Steuert, ob das Starten des Feature-Providers abgebrochen werden soll, wenn der
     *     Aufbau der ersten Connection länger dauert. Hat keinen Effekt bei `minConnections: 0`.
     *     Diese Option sollte in der Regel nur auf Entwicklungssystemen deaktiviert werden.
     * @default true
     */
    @Deprecated
    @Value.Default
    default boolean getInitFailFast() {
      return true;
    }

    @DocIgnore
    @Value.Default
    default String getInitFailTimeout() {
      return "1";
    }

    /**
     * @langEn The maximum amount of time that a connection is allowed to sit idle in the pool. Only
     *     applies to connections beyond the `minConnections` limit. A value of 0 means that idle
     *     connections are never removed from the pool.
     * @langDe Die maximale Zeit die eine Connection unbeschäftigt im Pool verbleibt. Bezieht sich
     *     nur auf Connections über der `minConnections` Grenze. Ein Wert von `0` bedeutet, dass
     *     unbeschäftigte Connections niemals aus dem Pool entfernt werden.
     * @default 10m
     */
    @Value.Default
    default String getIdleTimeout() {
      return "10m";
    }

    /**
     * @langEn If enabled for multiple providers with matching `host`, `database` and `user`, a
     *     single connection pool will be shared between these providers. If any of the other
     *     `connectionInfo` options do not match, the provider startup will fail.
     * @langDe Wenn `shared` für mehrere Provider mit übereinstimmenden `host`, `database` und
     *     `user` aktiviert ist, teilen sich diese Provider einen Connection-Pool. Wenn eine der
     *     anderen Optionen in `connectionInfo` nicht übereinstimmt, schlägt der Start des Providers
     *     fehl.
     * @default false
     */
    @Value.Default
    default boolean getShared() {
      return false;
    }
  }
}
