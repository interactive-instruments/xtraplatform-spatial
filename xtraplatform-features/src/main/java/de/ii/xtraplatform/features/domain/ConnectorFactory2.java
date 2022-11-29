/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.Optional;
import java.util.Set;

@AutoMultiBind
public interface ConnectorFactory2<T, U, V extends FeatureProviderConnector.QueryOptions> {

  String type();

  default Optional<String> subType() {
    return Optional.empty();
  }

  default String fullType() {
    if (subType().isPresent()) {
      return String.format("%s/%s", type(), subType().get());
    }

    return type();
  }

  Optional<FeatureProviderConnector<T, U, V>> instance(String id);

  Set<FeatureProviderConnector<T, U, V>> instances();

  FeatureProviderConnector<T, U, V> createInstance(
      String providerId, ConnectionInfo connectionInfo);

  boolean deleteInstance(String id);
}
