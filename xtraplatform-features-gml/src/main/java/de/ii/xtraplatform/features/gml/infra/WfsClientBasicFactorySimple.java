/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.infra;

import de.ii.xtraplatform.features.gml.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.gml.domain.WfsClientBasic;
import de.ii.xtraplatform.features.gml.domain.WfsClientBasicFactory;
import de.ii.xtraplatform.web.domain.Http;

public class WfsClientBasicFactorySimple implements WfsClientBasicFactory {
  private final Http http;

  public WfsClientBasicFactorySimple(Http http) {
    this.http = http;
  }

  @Override
  public WfsClientBasic create(
      String providerType, String providerId, ConnectionInfoWfsHttp connectionInfo) {
    return new WfsConnectorHttp(http, null, providerId, connectionInfo);
  }

  @Override
  public void dispose(WfsClientBasic wfsClient) {
    if (wfsClient instanceof WfsConnectorHttp) {
      ((WfsConnectorHttp) wfsClient).stop();
    }
  }
}
