/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.app;

import de.ii.xtraplatform.di.domain.Registry;
import de.ii.xtraplatform.di.domain.RegistryState;
import de.ii.xtraplatform.features.domain.FeatureQueriesExtension;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;

@Component
@Provides
@Instantiate
@Wbp(
    filter =
        Registry.FILTER_PREFIX
            + ProviderExtensionRegistryImpl.EXTENSION
            + Registry.FILTER_SUFFIX,
    onArrival = Registry.ON_ARRIVAL_METHOD,
    onDeparture = Registry.ON_DEPARTURE_METHOD)
public class ProviderExtensionRegistryImpl implements ProviderExtensionRegistry {

  static final String EXTENSION =
      "de.ii.xtraplatform.features.domain.FeatureQueriesExtension";

  private final Registry.State<FeatureQueriesExtension> extensions;

  public ProviderExtensionRegistryImpl(@Context BundleContext context) {
    this.extensions = new RegistryState<>(EXTENSION, context);
  }

  @Override
  public State<FeatureQueriesExtension> getRegistryState() {
    return extensions;
  }
}
