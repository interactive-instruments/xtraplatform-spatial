/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.docs.DocMarker;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import de.ii.xtraplatform.entities.domain.EntityDataDefaults;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableMap;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable(prehash = true)
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = DocIgnore.class)
@JsonDeserialize(builder = ImmutableFeatureProviderSqlData.Builder.class)
public interface FeatureProviderSqlData
    extends FeatureProviderDataV2, WithConnectionInfo<ConnectionInfoSql> {

  /**
   * @langEn See [Connection Info](#connection-info).
   * @langDe Siehe [Connection-Info](#connection-info).
   */
  @DocMarker("specific")
  @Nullable
  @Override
  ConnectionInfoSql getConnectionInfo();

  /**
   * @langEn Defaults for the path expressions in `sourcePath`, for details see [Source Path
   *     Defaults](10-sql.md#source-path-defaults) below.
   * @langDe Defaults für die Pfad-Ausdrücke in `sourcePath`, für Details siehe
   *     [SQL-Pfad-Defaults](10-sql.md#source-path-defaults).
   */
  @DocMarker("specific")
  @Nullable
  SqlPathDefaults getSourcePathDefaults();

  /**
   * @langEn Options for query generation, for details see [Query
   *     Generation](10-sql.md#query-generation) below.
   * @langDe Einstellungen für die Query-Generierung, für Details siehe
   *     [Query-Generierung](10-sql.md#query-generation).
   */
  @DocMarker("specific")
  @Nullable
  QueryGeneratorSettings getQueryGeneration();

  // for json ordering
  @Override
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getTypes();

  // for json ordering
  @Override
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getFragments();

  @Value.Check
  default FeatureProviderSqlData initNestedDefault() {
    /*
     workaround for https://github.com/interactive-instruments/ldproxy/issues/225
     TODO: remove when fixed
    */
    if (Objects.isNull(getConnectionInfo())) {
      ImmutableFeatureProviderSqlData.Builder builder =
          new ImmutableFeatureProviderSqlData.Builder().from(this);
      builder.connectionInfoBuilder().database("");

      return builder.build();
    }

    return this;
  }

  @Value.Check
  default FeatureProviderSqlData mergeExtensions() {
    List<ExtensionConfiguration> distinctExtensions = getMergedExtensions();

    // remove duplicates
    if (getExtensions().size() > distinctExtensions.size()) {
      return new ImmutableFeatureProviderSqlData.Builder()
          .from(this)
          .extensions(distinctExtensions)
          .build();
    }

    return this;
  }

  abstract class Builder
      extends FeatureProviderDataV2.Builder<ImmutableFeatureProviderSqlData.Builder>
      implements EntityDataBuilder<FeatureProviderDataV2> {

    public abstract ImmutableFeatureProviderSqlData.Builder connectionInfo(
        ConnectionInfoSql connectionInfo);

    @Override
    public ImmutableFeatureProviderSqlData.Builder fillRequiredFieldsWithPlaceholders() {
      return this.id(EntityDataDefaults.PLACEHOLDER)
          .providerType(EntityDataDefaults.PLACEHOLDER)
          .providerSubType(EntityDataDefaults.PLACEHOLDER)
          .connectionInfo(
              new ImmutableConnectionInfoSql.Builder()
                  .database(EntityDataDefaults.PLACEHOLDER)
                  .build());
    }
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableQueryGeneratorSettings.Builder.class)
  interface QueryGeneratorSettings {

    @DocIgnore
    @Value.Default
    default int getChunkSize() {
      return 10000;
    }

    /**
     * @langEn Option to disable computation of the number of selected features for performance
     *     reasons that are returned in `numberMatched`. As a general rule this should be disabled
     *     for big datasets.
     * @langDe Steuert, ob bei Abfragen die Anzahl der selektierten Features berechnet und in
     *     `numberMatched` zurückgegeben werden soll oder ob dies aus Performancegründen
     *     unterbleiben soll. Bei großen Datensätzen empfiehlt es sich in der Regel, die Option zu
     *     deaktivieren.
     * @default true
     */
    @Value.Default
    default boolean getComputeNumberMatched() {
      return true;
    }

    // TODO
    @DocIgnore
    @Value.Default
    default Optional<String> getAccentiCollation() {
      return Optional.empty();
    }
  }
}
