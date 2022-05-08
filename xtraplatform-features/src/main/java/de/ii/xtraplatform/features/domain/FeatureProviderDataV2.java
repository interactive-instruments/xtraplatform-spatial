/**
 * Copyright 2022 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.docs.DocColumn;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocFilesTemplate;
import de.ii.xtraplatform.docs.DocFilesTemplate.ForEach;
import de.ii.xtraplatform.docs.DocI18n;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.store.domain.entities.AutoEntity;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @langEn # Overview
 * <p>
 * A feature provider is defined in a configuration file by an object with the following properties.
 * Properties without default are mandatory.
 * <p>
 * {@docTable:properties}
 * <p>
 * ## Types
 * <p>
 * {@docTable:types}
 * <p>
 * @langDe # Übersicht
 * <p>
 * Jeder Feature-Provider wird in einer Konfigurationsdatei in einem Objekt mit den folgenden
 * Eigenschaften beschrieben. Werte ohne Defaultwert sind in diesem Fall Pflichtangaben.
 * <p>
 * {@docTable:properties}
 * @langEn ## Connection Info
 * <p>
 * For data source specifics, see [SQL](sql.md#connection-info) and [WFS](wfs.md#connection-info).
 * @langDe ## Connection Info
 * <p>
 * Informationen zu den Datenquellen finden Sie auf separaten Seiten: [SQL](sql.md#connection-info)
 * und [WFS](wfs.md#connection-info).
 * <p> *
 * @langEn ## Example Configuration (SQL)
 * <p>
 * See the [feature
 * provider](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/entities/providers/vineyards.yml)
 * of the API [Vineyards in Rhineland-Palatinate, Germany](https://demo.ldproxy.net/vineyards).
 * @langDe ## Example Configuration (SQL)
 * <p>
 * Als Beispiel siehe
 * die[Provider-Konfiguration](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/entities/providers/vineyards.yml)
 * der API [Weinlagen in Rheinland-Pfalz](https://demo.ldproxy.net/vineyards).
 * @propertyTable {@link de.ii.xtraplatform.features.domain.ImmutableFeatureProviderCommonData}
 * @propertyTable:types {@link de.ii.xtraplatform.features.domain.ImmutableFeatureSchema}
 */
@DocFile(
    path = "configuration/providers",
    name = "README.md",
    tables = {
        @DocTable(
            name = "types",
            rows = {
                @DocStep(type = Step.TAG_REFS, params = "{@propertyTable:types}"),
                @DocStep(type = Step.JSON_PROPERTIES)
            },
            columnSet = ColumnSet.JSON_PROPERTIES
        ),
        @DocTable(
            name = "properties",
            rows = {
                @DocStep(type = Step.TAG_REFS, params = "{@propertyTable}"),
                @DocStep(type = Step.JSON_PROPERTIES)
            },
            columnSet = ColumnSet.JSON_PROPERTIES
        )
    }
)
@DocFilesTemplate(
    files = ForEach.IMPLEMENTATION,
    path = "configuration/providers",
    stripPrefix = "FeatureProviderData",
    template = {
        @DocI18n(language = "en", value = "{@body}"),
        @DocI18n(language = "de", value = "{@body}")
    }
)
@JsonDeserialize(builder = ImmutableFeatureProviderCommonData.Builder.class)
public interface FeatureProviderDataV2 extends EntityData, AutoEntity, ExtendableConfiguration {

  @Override
  @Value.Derived
  default long getEntitySchemaVersion() {
    return 2;
  }

  /**
   * @langEn Always `FEATURE`.
   * @langDe Stets `FEATURE`.
   */
  String getProviderType();

  /**
   * @langEn `SQL` for SQL DBMS as data source, `WFS` for *OGC Web Feature Service* as data source.
   * @langDe `SQL` für ein SQL-DBMS als Datenquelle, `WFS` für einen OGC Web Feature Service als
   * Datenquelle.
   */
  String getFeatureProviderType();

  @JsonIgnore
  @Value.Derived
  @Override
  default Optional<String> getEntitySubType() {
    return Optional.of(
        String.format("%s/%s", getProviderType(), getFeatureProviderType()).toLowerCase());
  }


  /**
   * @langEn Coordinate reference system of geometries in the dataset. The EPSG code of the
   * coordinate reference system is given as integer in `code`. `forceAxisOrder` may be set to use a
   * non-default axis order:  `LON_LAT` uses longitude/east as first value and latitude/north as
   * second value, `LAT_LON` uses the reverse. `NONE` uses the default axis order and is the default
   * value. Example: The default coordinate reference system `CRS84` would look like this: `code:
   * 4326` and `forceAxisOrder: LON_LAT`.
   * @langDe Das Koordinatenreferenzsystem, in dem Geometrien in dem Datensatz geführt werden. Der
   * EPSG-Code des Koordinatenreferenzsystems wird als Integer in `code` angegeben. Mit
   * `forceAxisOrder` kann die Koordinatenreihenfolge geändert werden: `NONE` verwendet die
   * Reihenfolge des Koordinatenreferenzsystems, `LON_LAT` verwendet stets Länge/Ostwert als ersten
   * und Breite/Nordwert als zweiten Wert, `LAT_LON` entsprechend umgekehrt. Beispiel: Das
   * Default-Koordinatenreferenzsystem `CRS84` entspricht `code: 4326` und `forceAxisOrder:
   * LON_LAT`.
   * @default CRS84
   */
  Optional<EpsgCrs> getNativeCrs();


  /**
   * @langEn A timezone ID, such as `Europe/Berlin`. Is applied to temporal values without timezone
   * in the dataset.
   * @langDe Eine Zeitzonen-ID, z.B. `Europe/Berlin`. Wird auf temporale Werte ohne Zeitzone im
   * Datensatz angewendet.
   * @default UTC
   */
  @JsonDeserialize(converter = ZoneIdFromString.class)
  Optional<ZoneId> getNativeTimeZone();


  /**
   * @langEn
   * @langDe
   * @default
   */
  Optional<String> getDefaultLanguage();


  @Value.Default
  default MODE getTypeValidation() {
    return MODE.NONE;
  }


  @Override
  List<ExtensionConfiguration> getExtensions();


  /**
   * @langEn Definition of feature types, see [below](#types).
   * @langDe Definition of object types, see [below](#types).
   */
  @JsonMerge
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getTypes();


  /**
   * @langEn
   * @langDe
   * @default {}
   */
  Map<String, Map<String, String>> getCodelists();


  /**
   * @langEn List of source types to include in derived `types` definitions when `auto: true`.
   * Currently only works for [SQL](sql.md).
   * @langDe Liste von Quelltypen, die für die Ableitung der `types` Definitionen im Auto-Modus
   * berücksichtigt werden sollen. Funktioniert aktuell nur für [SQL](sql.md).
   * @default []
   */
  List<String> getAutoTypes();


  // custom builder to automatically use keys of types as name of FeatureTypeV2
  abstract class Builder<T extends Builder<T>> implements EntityDataBuilder<FeatureProviderDataV2> {

    @JsonIgnore
    public abstract Map<String, ImmutableFeatureSchema.Builder> getTypes();

    @JsonProperty(value = "types")
    public Map<String, ImmutableFeatureSchema.Builder> getTypes2() {
      Map<String, ImmutableFeatureSchema.Builder> types = getTypes();

      return new ApplyKeyToValueMap<>(types, (key, builder) -> builder.name(key));
    }

    public abstract T putTypes(String key, ImmutableFeatureSchema.Builder builder);

    @JsonProperty(value = "types")
    public T putTypes2(String key, ImmutableFeatureSchema.Builder builder) {
      return putTypes(key, builder.name(key));
    }

    public abstract T id(String id);

    public abstract T providerType(String providerType);

    public abstract T featureProviderType(String featureProviderType);

    // jackson should append to instead of replacing extensions
    @JsonIgnore
    public abstract T extensions(Iterable<? extends ExtensionConfiguration> elements);

    @JsonProperty("extensions")
    public abstract T addAllExtensions(Iterable<? extends ExtensionConfiguration> elements);

  }

}
