/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocMarker;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.docs.DocVar;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * # GraphQL
 *
 * @langEn The specifics of the GraphQL feature provider.
 * @langDe Hier werden die Besonderheiten des GraphQL-Feature-Providers beschrieben.
 * @langAll ## Connection Info
 * @langEn The connection info object for GraphQL has the following properties:
 * @langDe Das Connection-Info-Objekt für GraphQL wird wie folgt beschrieben:
 * @langAll {@docTable:connectionInfo}
 * @langEn ### Example
 * @langDe ### Beispiel
 * @langAll {@docVar:example}
 * @langEn ## Path Syntax
 * @langDe ## Pfad-Syntax
 * @ref:connectionInfo {@link
 *     de.ii.xtraplatform.features.graphql.domain.ImmutableConnectionInfoGraphQlHttp}
 * @ref:example {@link ConnectionInfoGraphQlHttp}
 */
@DocFile(
    path = "providers/feature",
    name = "graphql.md",
    tables = {
      @DocTable(
          name = "connectionInfo",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:connectionInfo}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    },
    vars = {
      @DocVar(
          name = "example",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:example}"),
            @DocStep(type = Step.TAG, params = "{@examplesAll}")
          })
    })
@Value.Immutable
@BuildableMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureProviderGraphQlData.Builder.class)
public interface FeatureProviderGraphQlData
    extends FeatureProviderDataV2, WithConnectionInfo<ConnectionInfoGraphQlHttp> {

  String PLACEHOLDER_URI = "https://place.holder";

  @Nullable
  @Override
  ConnectionInfoGraphQlHttp getConnectionInfo();

  /**
   * @langEn Options for query generation, for details see [Queries](#queries) below.
   * @langDe Einstellungen für die Query-Generierung, für Details siehe [Queries](#queries).
   */
  @DocMarker("specific")
  @JsonProperty(
      value = "queries",
      access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  @Nullable
  GraphQlQueries getQueries();

  // for json ordering
  @Override
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getTypes();

  @Value.Check
  default FeatureProviderGraphQlData initNestedDefault() {
    /*
     workaround for https://github.com/interactive-instruments/ldproxy/issues/225
     TODO: remove when fixed
    */
    if (Objects.isNull(getConnectionInfo()) || Objects.isNull(getConnectionInfo().getUri())) {
      ImmutableFeatureProviderGraphQlData.Builder builder =
          new ImmutableFeatureProviderGraphQlData.Builder().from(this);
      builder.connectionInfoBuilder().uri(URI.create(PLACEHOLDER_URI));

      return builder.build();
    }

    return this;
  }

  @Value.Check
  default FeatureProviderGraphQlData mergeExtensions() {
    List<ExtensionConfiguration> distinctExtensions = getMergedExtensions();

    // remove duplicates
    if (getExtensions().size() > distinctExtensions.size()) {
      return new ImmutableFeatureProviderGraphQlData.Builder()
          .from(this)
          .extensions(distinctExtensions)
          .build();
    }

    return this;
  }

  abstract class Builder
      extends FeatureProviderDataV2.Builder<ImmutableFeatureProviderGraphQlData.Builder>
      implements EntityDataBuilder<FeatureProviderDataV2> {

    public abstract ImmutableFeatureProviderGraphQlData.Builder connectionInfo(
        ConnectionInfoGraphQlHttp connectionInfo);

    @Override
    public ImmutableFeatureProviderGraphQlData.Builder fillRequiredFieldsWithPlaceholders() {
      return this.id(EntityDataDefaults.PLACEHOLDER)
          .providerType(EntityDataDefaults.PLACEHOLDER)
          .providerSubType(EntityDataDefaults.PLACEHOLDER)
          .connectionInfo(
              new ImmutableConnectionInfoGraphQlHttp.Builder()
                  .uri(URI.create("http://www.example.com"))
                  .build());
    }
  }
}
