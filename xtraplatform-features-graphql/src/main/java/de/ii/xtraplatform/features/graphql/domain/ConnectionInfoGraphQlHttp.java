/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.graphql.infra.GraphQlConnectorHttp;
import java.net.URI;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = DocIgnore.class)
@JsonDeserialize(builder = ImmutableConnectionInfoGraphQlHttp.Builder.class)
public interface ConnectionInfoGraphQlHttp extends ConnectionInfo {

  @DocIgnore
  @Override
  @Value.Derived
  default String getConnectorType() {
    return GraphQlConnectorHttp.CONNECTOR_TYPE;
  }

  /**
   * @langEn The URI of the GraphQL endpoint.
   * @langDe Die URI des GraphQL Endpunkts.
   */
  @Nullable
  URI getUri();

  /**
   * @langEn The user name for HTTP Basic Auth.
   * @langDe Der Benutzername.
   */
  Optional<String> getUser();

  /**
   * @langEn The base64 encoded password for HTTP Basic Auth.
   * @langDe Das mit base64 verschüsselte Passwort des Benutzers.
   */
  Optional<String> getPassword();

  @Override
  @JsonIgnore
  @Value.Lazy
  default String getDatasetIdentifier() {
    return Optional.ofNullable(getUri()).map(URI::toString).orElse("");
  }
}
