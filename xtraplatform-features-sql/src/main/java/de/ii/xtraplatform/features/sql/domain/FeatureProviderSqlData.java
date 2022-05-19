/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.docs.DocMarker;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * # SQL Features
 * @langEn The specifics of the SQL feature provider.
 * @langDe Hier werden die Besonderheiten des SQL-Feature-Providers beschrieben.
 * @langAll {@docTable:properties}
 * @langAll ## Connection Info
 * @langEn The connection info object for SQL databases has the following properties:
 * @langDe Das Connection-Info-Objekt für SQL-Datenbanken wird wie folgt beschrieben:
 * @langAll {@docTable:connectionInfo}
 * @langAll ### Pool
 * @langEn Settings for the connection pool.
 * @langDe Einstellungen für den Connection-Pool.
 * @langAll {@docTable:pool}
 * @ref:properties {@link de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData}
 * @ref:connectionInfo {@link de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql}
 * @ref:pool {@link de.ii.xtraplatform.features.sql.domain.ImmutablePoolSettings}
 */
@DocFile(
    path = "providers",
    name = "sql.md",
    tables = {
        @DocTable(
            name = "properties",
            rows = {
                @DocStep(type = Step.TAG_REFS, params = "{@ref:properties}"),
                @DocStep(type = Step.JSON_PROPERTIES),
                @DocStep(type = Step.MARKED, params = "specific")
            },
            columnSet = ColumnSet.JSON_PROPERTIES
        ),
        @DocTable(
            name = "connectionInfo",
            rows = {
                @DocStep(type = Step.TAG_REFS, params = "{@ref:connectionInfo}"),
                @DocStep(type = Step.JSON_PROPERTIES)
            },
            columnSet = ColumnSet.JSON_PROPERTIES
        ),
        @DocTable(
            name = "pool",
            rows = {
                @DocStep(type = Step.TAG_REFS, params = "{@ref:pool}"),
                @DocStep(type = Step.JSON_PROPERTIES)
            },
            columnSet = ColumnSet.JSON_PROPERTIES
        ),
    }
)
@Value.Immutable
@JsonDeserialize(builder = ImmutableFeatureProviderSqlData.Builder.class)
public interface FeatureProviderSqlData extends FeatureProviderDataV2,
    WithConnectionInfo<ConnectionInfoSql> {

  /**
   * @langEn See [Connection Info](#connection-info).
   * @langDe Siehe [Connection-Info](#connection-info).
   */
  @DocMarker("specific")
  @Nullable
  @Override
  ConnectionInfoSql getConnectionInfo();

  /**
   * @langEn Defaults for the path expressions in `sourcePath`, for details see [Source Path Defaults](#source-path-defaults) below.
   * @langDe Defaults für die Pfad-Ausdrücke in `sourcePath`, für Details siehe [SQL-Pfad-Defaults](#source-path-defaults).
   */
  //@JsonProperty(value = "sourcePathDefaults", access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  //@Value.Default
  //can't use interface, bug in immutables when using attributeBuilderDetection and Default
  //default SqlPathDefaults getSourcePathDefaults() {
  //    return new ImmutableSqlPathDefaults.Builder().build();
  //}
  @DocMarker("specific")
  @Nullable
  SqlPathDefaults getSourcePathDefaults();

  /**
   * @langEn Options for query generation, for details see [Query Generation](#query-generation) below.
   * @langDe Einstellungen für die Query-Generierung, für Details siehe [Query-Generierung](#query-generation).
   */
  @DocMarker("specific")
  @JsonProperty(value = "queryGeneration", access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  //@Value.Default
  //can't use interface, bug in immutables when using attributeBuilderDetection and Default
  /*default QueryGeneratorSettings getQueryGeneration() {
    ImmutableQueryGeneratorSettings.Builder builder = new ImmutableQueryGeneratorSettings.Builder();

    getConnectionInfo().getComputeNumberMatched().ifPresent(builder::computeNumberMatched);

    return builder.build();
  }*/
  @Nullable
  QueryGeneratorSettings getQueryGeneration();

  // for json ordering
  @Override
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getTypes();

  @Value.Check
  default FeatureProviderSqlData initNestedDefault() {
    /*
     workaround for https://github.com/interactive-instruments/ldproxy/issues/225
     TODO: remove when fixed
    */
    if (Objects.isNull(getConnectionInfo())) {
      ImmutableFeatureProviderSqlData.Builder builder = new ImmutableFeatureProviderSqlData.Builder()
          .from(this);
      builder.connectionInfoBuilder().database("");

      return builder.build();
    }

    /*
     - apply defaults for sourcePathDefaults and queryGeneration if necessary (cannot be set via @Value.Default due to a bug in immutables)
     - migrate from old syntax for sourcePathDefaults and queryGeneration in connectionInfo
    */
    boolean queryGenerationIsNull = Objects.isNull(getQueryGeneration());
    boolean computeNumberMatchedDiffers =
        !queryGenerationIsNull && getConnectionInfo().getComputeNumberMatched().isPresent()
            && !Objects.equals(getConnectionInfo().getComputeNumberMatched().get(),
            getQueryGeneration().getComputeNumberMatched());
    boolean sourcePathDefaultsIsNull = Objects.isNull(getSourcePathDefaults());
    boolean sourcePathDefaultsDiffers =
        !sourcePathDefaultsIsNull && getConnectionInfo().getPathSyntax().isPresent()
            && !Objects.equals(getConnectionInfo().getPathSyntax().get(), getSourcePathDefaults());

    if ((queryGenerationIsNull || computeNumberMatchedDiffers) || sourcePathDefaultsIsNull || sourcePathDefaultsDiffers) {
      ImmutableFeatureProviderSqlData.Builder builder = new ImmutableFeatureProviderSqlData.Builder()
          .from(this);
      ImmutableConnectionInfoSql.Builder connectionInfoBuilder = builder.connectionInfoBuilder();

      if (queryGenerationIsNull || computeNumberMatchedDiffers) {
        ImmutableQueryGeneratorSettings.Builder queryGenerationBuilder = builder.queryGenerationBuilder();
        getConnectionInfo().getComputeNumberMatched().ifPresent(
            computeNumberMatched -> {
              queryGenerationBuilder.computeNumberMatched(computeNumberMatched);
              connectionInfoBuilder.computeNumberMatched(Optional.empty());
            });
      }
      if (sourcePathDefaultsIsNull || sourcePathDefaultsDiffers) {
        ImmutableSqlPathDefaults.Builder sourcePathDefaultsBuilder = builder.sourcePathDefaultsBuilder();
        getConnectionInfo().getPathSyntax().ifPresent(pathSyntax -> {
          if (!Objects.equals(pathSyntax.getSortKey(), "id")) {
            sourcePathDefaultsBuilder.sortKey(pathSyntax.getSortKey());
          }
          if (!Objects.equals(pathSyntax.getPrimaryKey(), "id")) {
            sourcePathDefaultsBuilder.primaryKey(pathSyntax.getPrimaryKey());
          }
          if (pathSyntax.getJunctionTablePattern().isPresent()) {
            sourcePathDefaultsBuilder.junctionTablePattern(pathSyntax.getJunctionTablePattern());
          }
          connectionInfoBuilder.pathSyntax(Optional.empty());
        });
      }

      return builder.build();
    }

    return this;
  }

  @Value.Check
  default FeatureProviderSqlData mergeExtensions() {
    List<ExtensionConfiguration> distinctExtensions = getMergedExtensions();

    // remove duplicates
    if (getExtensions().size() > distinctExtensions.size()) {
      return new ImmutableFeatureProviderSqlData.Builder().from(this)
          .extensions(distinctExtensions)
          .build();
    }

    return this;
  }

  abstract class Builder extends
      FeatureProviderDataV2.Builder<ImmutableFeatureProviderSqlData.Builder> implements
      EntityDataBuilder<FeatureProviderDataV2> {

    public abstract ImmutableFeatureProviderSqlData.Builder connectionInfo(
        ConnectionInfoSql connectionInfo);

    @Override
    public ImmutableFeatureProviderSqlData.Builder fillRequiredFieldsWithPlaceholders() {
      return this.id(EntityDataDefaults.PLACEHOLDER)
          .providerType(EntityDataDefaults.PLACEHOLDER)
          .featureProviderType(EntityDataDefaults.PLACEHOLDER)
          .connectionInfo(new ImmutableConnectionInfoSql.Builder().database(EntityDataDefaults.PLACEHOLDER).build());
    }
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableQueryGeneratorSettings.Builder.class)
  interface QueryGeneratorSettings {

    /**
     * @langEn Option to disable computation of the number of selected features for performance reasons that
     * are returned in `numberMatched`. As a general rule this should be disabled for big datasets.
     * @langDe Steuert, ob bei Abfragen die Anzahl der selektierten Features berechnet und in `numberMatched` zurückgegeben
     * werden soll oder ob dies aus Performancegründen unterbleiben soll. Bei großen Datensätzen empfiehlt es
     * sich in der Regel, die Option zu deaktivieren.
     * @default true
     */
    @Value.Default
    default boolean getComputeNumberMatched() {
      return true;
    }

    //TODO
    @DocIgnore
    @Value.Default
    default Optional<String> getAccentiCollation() {
      return Optional.empty();
    }
  }

}
