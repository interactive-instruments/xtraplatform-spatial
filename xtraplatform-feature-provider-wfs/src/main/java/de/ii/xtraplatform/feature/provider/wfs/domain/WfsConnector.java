/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.domain;

import akka.util.ByteString;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.feature.provider.wfs.FeatureQueryEncoderWfs;
import de.ii.xtraplatform.ogc.api.wfs.WfsOperation;

import java.io.InputStream;

/**
 * @author zahnen
 */
public interface WfsConnector extends FeatureProviderConnector<ByteString, String, FeatureProviderConnector.QueryOptions> {

    void setQueryEncoder(final FeatureQueryEncoderWfs queryEncoder);

    InputStream runWfsOperation(final WfsOperation wfsOperation);
}
