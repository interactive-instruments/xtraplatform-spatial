/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import akka.japi.Pair;
import akka.util.ByteString;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.feature.provider.wfs.app.request.WfsOperation;

import java.io.InputStream;

/**
 * @author zahnen
 */
public interface WfsConnectorPost extends FeatureProviderConnector<ByteString, Pair<String,String>, FeatureProviderConnector.QueryOptions> {

    void setQueryEncoder(final FeatureQueryEncoderWfs queryEncoder);

    InputStream runWfsOperation(final WfsOperation wfsOperation);
}
