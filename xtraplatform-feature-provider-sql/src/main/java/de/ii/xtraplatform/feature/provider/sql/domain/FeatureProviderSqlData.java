/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableFeatureProviderSqlData.Builder.class)
public interface FeatureProviderSqlData extends FeatureProviderDataV2,
    WithConnectionInfo<ConnectionInfoSql> {

  @Nullable
  @Override
  ConnectionInfoSql getConnectionInfo();

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  //@Value.Default
  //can't use interface, bug in immutables when using attributeBuilderDetection and Default
  //default SqlPathDefaults getSourcePathDefaults() {
  //    return new ImmutableSqlPathDefaults.Builder().build();
  //}
  @Nullable
  SqlPathDefaults getSourcePathDefaults();

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
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

    @Value.Default
    default boolean getComputeNumberMatched() {
      return true;
    }
  }

}
