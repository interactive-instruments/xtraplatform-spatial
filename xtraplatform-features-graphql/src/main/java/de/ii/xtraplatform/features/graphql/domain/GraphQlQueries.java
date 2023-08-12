/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableGraphQlQueries.Builder.class)
public interface GraphQlQueries {

  CollectionQuery getCollection();

  Optional<SingleQuery> getSingle();

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableSingleQuery.Builder.class)
  interface SingleQuery {
    String getName();

    default String getName(String type) {
      return StringTemplateFilters.applyTemplate(getName(), (Map.of("type", type))::get);
    }

    @Nullable
    QueryArgumentsSingle getArguments();

    @Nullable
    QueryFields getFields();
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableCollectionQuery.Builder.class)
  interface CollectionQuery {
    @Value.Default
    default String getName() {
      return "{{type | toLower}}";
    }

    default String getName(String type) {
      return StringTemplateFilters.applyTemplate(getName(), (Map.of("type", type))::get);
    }

    @Nullable
    QueryArgumentsCollection getArguments();

    @Nullable
    QueryFields getFields();
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableQueryArgumentsSingle.Builder.class)
  interface QueryArgumentsSingle {
    Optional<String> getId();
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableQueryArgumentsCollection.Builder.class)
  interface QueryArgumentsCollection {

    Optional<String> getId();

    Optional<String> getLimit();

    Optional<String> getOffset();

    Optional<String> getFilter();

    Optional<String> getEquals();

    Optional<String> getBbox();

    Optional<String> getGeometry();
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableQueryFields.Builder.class)
  interface QueryFields {
    Optional<String> getGeometry();
  }
}
