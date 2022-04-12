/**
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
import de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql.Builder;
import de.ii.xtraplatform.features.sql.infra.db.SqlConnectorRx;
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

/**
 * @title Connection Info for SQL databases
 * @en The connection info object for SQL databases has the following properties:
 * @de Das Connection-Info-Objekt für SQL-Datenbanken wird wie folgt beschrieben:
 */

/**
 * @title Source Path Syntax
 * @en The fundamental elements of the path syntax are demonstrated in the example above. The path to a property is
 * formed by concatenating the relative paths (`sourcePath`) with "/". A `sourcePath` has to be defined for the for
 * object that represents the feature type and most child objects.
 *
 * On the first level the path is formed by a "/" followed by the table name for the feature type. Every row in the
 * table corresponds to a feature. Example: `/kita`
 *
 * When defining a feature property on a deeper level using a column from the given table, the path equals the column
 * name, e.g. `name`. The full path will then be `/kita/name`.
 *
 * A join is defined using the pattern `[id=fk]tab`, where `id` is the primary key of the table from the parent object,
 * `fk` is the foreign key of the joining table and `tab` is the name of the joining table. Example from above:
 * `[oid=kita_fk]plaetze`. When a junction table should be used, two such joins are concatenated with "/", e.g. `[id=fka]a_2_b/[fkb=id]tab_b`.
 *
 * Rows for a table can be filtered by adding `{filter=expression}` after the table name, where `expression` is a
 * [CQL Text](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) expression. For details see the module [Filter / CQL](../services/filter.md), which provides the implementation but does not have to be enabled.
 *
 * To select capacity information only when the value is not NULL and greater than zero in the example above,
 * the filter would look like this: `[oid=kita_fk]plaetze{filter=anzahl IS NOT NULL AND anzahl>0}`
 *
 * A non-default sort key can be set by adding `{sortKey=columnName}` after the table name.
 * @de In dem Beispiel oben sind die wesentlichen Elemente der Pfadsyntax in der Datenbank bereits erkennbar.
 * Der Pfad zu einer Eigenschaft ergibt sich immer als Konkatenation der relativen Pfadangaben (`sourcePath`),
 * jeweils ergänzt um ein "/". Die Eigenschaft `sourcePath` ist beim ersten Objekt, das die Objektart repräsentiert,
 * angegeben und bei allen untergeordneten Schemaobjekten, außer es handelt sich um einen festen Wert.
 *
 * Auf der obersten Ebene entspricht der Pfad einem "/" gefolgt vom Namen der Tabelle zur Objektart. Jede Zeile in der
 * Tabelle entsprich einem Feature. Beispiel: `/kita`.
 *
 * Bei nachgeordneten relativen Pfadangaben zu einem Feld in derselben Tabelle wird einfach der Spaltenname angeben,
 * z.B. `name`. Daraus ergibt sich der Gesamtpfad `/kita/name`.
 *
 * Ein Join wird nach dem Muster `[id=fk]tab` angegeben, wobei `id` der Primärschlüssel der Tabelle aus dem übergeordneten
 * Schemaobjekt ist, `fk` der Fremdschlüssel aus der über den Join angebundenen Tabelle und `tab` der Tabellenname. Siehe
 * `[oid=kita_fk]plaetze` in dem Beispiel oben. Bei der Verwendung einer Zwischentabelle werden zwei dieser Joins
 * aneinandergehängt, z.B. `[id=fka]a_2_b/[fkb=id]tab_b`.
 *
 * Auf einer Tabelle (der Haupttabelle eines Features oder einer über Join-angebundenen Tabelle) kann zusätzlich ein
 * einschränkender Filter durch den Zusatz `{filter=ausdruck}` angegeben werden, wobei `ausdruck` das Selektionskriertium
 * in [CQL Text](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) spezifiziert. Für Details siehe das Modul
 * [Filter / CQL](../services/filter.md), welches die Implementierung bereitstellt, aber nicht aktiviert sein muss.
 *
 * Wenn z.B. in dem Beispiel oben nur Angaben zur Belegungskapazität selektiert werden sollen, deren Wert nicht NULL
 * und gleichzeitig größer als Null ist, dann könnte man schreiben: `[oid=kita_fk]plaetze{filter=anzahl IS NOT NULL AND anzahl>0}`.
 *
 * Ein vom Standard abweichender `sortKey` kann durch den Zusatz von `{sortKey=Spaltenname}` nach dem Tabellennamen angegeben werden.
 *
 * Ein vom Standard abweichender `primaryKey` kann durch den Zusatz von `{primaryKey=Spaltenname}` nach dem Tabellennamen angegeben werden.
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableConnectionInfoSql.Builder.class)
@MergeableMapEncodingEnabled
public interface ConnectionInfoSql extends ConnectionInfo {

    /**
     * @en `PGIS` for PostgreSQL/PostGIS, `GPKG` for GeoPackage or SQLite/SpatiaLite.
     * @de `PGIS` für PostgreSQL/PostGIS, `GPKG` für GeoPackage oder SQLite/SpatiaLite.
     * @default `PGIS
     */
    enum Dialect {PGIS,GPKG}

    @Override
    @Value.Derived
    default String getConnectorType() {
        return SqlConnectorRx.CONNECTOR_TYPE;
    }

    @Value.Default
    default Dialect getDialect() {
        return Dialect.PGIS;
    }

    /**
     * @en The name of the database. For `GPKG` the file path, either absolute or relative to the [data folder](../../data-folder.md).
     * @de Der Name der Datenbank. Für `GPKG` der Pfad zur Datei, entweder absolut oder relativ zum [Daten-Verzeichnis](../../data-folder.md).
     * @default
     */
    String getDatabase();

    /**
     * @en The database host. To use a non-default port, add it to the host separated by `:`, e.g. `db:30305`. Not relevant for `GPKG`.
     * @de Der Datenbankhost. Wird ein anderer Port als der Standardport verwendet, ist dieser durch einen Doppelpunkt
     * getrennt anzugeben, z.B. `db:30305`. Nicht relevant für `GPKG`.
     * @default
     */
    Optional<String> getHost();

    /**
     * @en The user name. Not relevant for `GPKG`.
     * @de Der Benutzername. Nicht relevant für `GPKG`.
     * @default
     */
    Optional<String> getUser();

    /**
     * @en The base64 encoded password of the user. Not relevant for `GPKG`.
     * @de Das mit base64 kodierte Passwort des Benutzers. Nicht relevant für `GPKG`.
     * @default
     */
    Optional<String> getPassword();

    /**
     * @en The names of database schemas that should be used in addition to `public`. Not relevant for `GPKG`.
     * @de Die Namen der Schemas in der Datenbank, auf die zugegriffen werden soll. Nicht relevant für `GPKG`.
     * @default `[]`
     */
    List<String> getSchemas();

    /**
     * @en Connection pool settings, for details see [Pool](#connection-pool) below.
     * @de Einstellungen für den Connection-Pool, für Details siehe [Pool](#connection-pool).
     * @default see below
     */
    @JsonProperty(value = "pool", access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    //@Value.Default
    //can't use interface, bug in immutables when using attributeBuilderDetection and Default
    //default PoolSettings getPool() {
    //    return new ImmutablePoolSettings.Builder().build();
    //}
    @Nullable
    PoolSettings getPool();

    /**
     * @en Custom options for the JDBC driver. For `PGIS`, you might pass `gssEncMode`, `ssl`, `sslmode`, `sslcert`,
     * `sslkey`, `sslrootcert` and `sslpassword`. For details see the
     * [driver documentation](https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters).
     * @de Einstellungen für den JDBC-Treiber. Für `PGIS` werden `gssEncMode`, `ssl`, `sslmode`, `sslcert`,
     * `sslkey`, `sslrootcert` und `sslpassword` durchgereicht. Für Details siehe die
     * [Dokumentation des Treibers](https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters).
     * @default `{}`
     */
    Map<String,Object> getDriverOptions();

    /**
     * @en
     * @de
     * @default
     */
    Optional<FeatureActionTrigger> getTriggers();

    @Override
    @JsonIgnore
    @Value.Lazy
    default boolean isShared() {
        return Objects.nonNull(getPool()) && getPool().getShared();
    }

    /**
     * @en *Deprecated* See `pool.maxConnections`.
     * @de *Deprecated* Siehe `pool.maxConnections`.
     * @default dynamic
     */
    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonAlias("maxThreads")
    @JsonProperty(value = "maxConnections", access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    OptionalInt getMaxConnections();

    /**
     * @en *Deprecated* See `pool.minConnections`.
     * @de *Deprecated* Siehe `pool.minConnections`.
     * @default `maxConnections`
     */
    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonProperty(value = "minConnections", access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    OptionalInt getMinConnections();

    /**
     * @en *Deprecated* See `pool.initFailFast`.
     * @de *Deprecated* Siehe `pool.initFailFast`.
     * @default `true`
     */
    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonProperty(value = "initFailFast", access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    Optional<Boolean> getInitFailFast();

    /**
     * @en *Deprecated* See [Query Generation](#query-generation) below.
     * @de *Deprecated* Siehe [Query-Generierung](#query-generation).
     * @default `true`
     */
    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonProperty(value = "computeNumberMatched", access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    Optional<Boolean> getComputeNumberMatched();

    /**
     * @en *Deprecated* See [Source Path Defaults](#source-path-defaults) below.
     * @de *Deprecated* Siehe [SQL-Pfad-Defaults](#source-path-defaults).
     * @default `{ 'defaultPrimaryKey': 'id', 'defaultSortKey': 'id' }`
     */
    @Deprecated(forRemoval = true, since = "ldproxy 3.0.0")
    @JsonProperty(value = "pathSyntax", access = JsonProperty.Access.WRITE_ONLY) // means only read from json
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

    /**
     * @title Pool
     * @en Settings for the connection pool.
     * @de Einstellungen für den Connection-Pool.
     */
    @Value.Immutable
    @JsonDeserialize(builder = ImmutablePoolSettings.Builder.class)
    interface PoolSettings {

        /**
         * @en Maximum number of connections to the database. The default value is computed depending on the number of
         * processor cores and the maximum number of joins per feature type in the [Types Configuration](README.md#feature-provider-types).
         * The default value is recommended for optimal performance under load. The smallest possible value also depends
         * on the maximum number of joins per feature type, smaller values are rejected.
         * @de Steuert die maximale Anzahl von Verbindungen zur Datenbank. Der Default-Wert ist abhängig von der Anzahl
         * der Prozessorkerne und der Anzahl der Joins in der [Types-Konfiguration](README.md#feature-provider-types).
         * Der Default-Wert wird für optimale Performanz unter Last empfohlen. Der kleinstmögliche Wert ist ebenfalls
         * von der Anzahl der Joins abhängig, kleinere Werte werden zurückgewiesen.
         * @default dynamisch
         */
        @Value.Default
        default int getMaxConnections() {
            return -1;
        }

        /**
         * @en Minimum number of connections to the database that are maintained.
         * @de Steuert die minimale Anzahl von Verbindungen zur Datenbank, die jederzeit offen gehalten werden.
         * @default `maxConnections`
         */
        @Value.Default
        default int getMinConnections() {
            return getMaxConnections();
        }

        /**
         * @en If disabled the provider will wait longer for the first database connection to be established.
         * Has no effect if `minConnections` is `0`. Should normally be disabled only on development systems.
         * @de Steuert, ob das Starten des Feature-Providers abgebrochen werden soll, wenn der Aufbau der ersten
         * Connection länger dauert. Hat keinen Effekt bei `minConnections: 0`. Diese Option sollte in der
         * Regel nur auf Entwicklungssystemen deaktiviert werden.
         * @default `true`
         */
        @Deprecated
        @Value.Default
        default boolean getInitFailFast() {
            return true;
        }

        /**
         * @en
         * @de
         * @default
         */
        @Value.Default
        default String getInitFailTimeout() {
            return "1";
        }

        /**
         * @en The maximum amount of time that a connection is allowed to sit idle in the pool. Only applies to
         * connections beyond the `minConnections` limit. A value of 0 means that idle connections are never removed from the pool.
         * @de Die maximale Zeit die eine Connection unbeschäftigt im Pool verbleibt. Bezieht sich nur auf Connections
         * über der `minConnections` Grenze. Ein Wert von `0` bedeutet, dass unbeschäftigte Connections niemals aus dem Pool entfernt werden.
         * @default `10m`
         */
        @Value.Default
        default String getIdleTimeout() {
            return "10m";
        }

        /**
         * @en If enabled for multiple providers with matching `host`, `database` and `user`, a single connection pool
         * will be shared between these providers. If any of the other `connectionInfo`
         * options do not match, the provider startup will fail.
         * @de Wenn `shared` für mehrere Provider mit übereinstimmenden `host`, `database` und `user` aktiviert ist,
         * teilen sich diese Provider einen Connection-Pool. Wenn eine der anderen Optionen in `connectionInfo`
         * nicht übereinstimmt, schlägt der Start des Providers fehl.
         * @default `false`
         */
        @Value.Default
        default boolean getShared() {
            return false;
        }
    }
}
