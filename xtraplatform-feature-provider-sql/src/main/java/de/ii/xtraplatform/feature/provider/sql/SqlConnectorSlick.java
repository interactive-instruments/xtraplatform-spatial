/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql;

import akka.Done;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderConnector;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {
        @StaticServiceProperty(name = "providerType", type = "java.lang.String", value = "PGIS"),
        @StaticServiceProperty(name = "connectorType", type = "java.lang.String", value = SqlConnectorSlick.CONNECTOR_TYPE)
})
public class SqlConnectorSlick implements FeatureProviderConnector {
    public static final String CONNECTOR_TYPE = "SLICK";

    @Override
    public CompletionStage<Done> runQuery(FeatureQuery query, Sink<ByteString, CompletionStage<Done>> transformer, Map<String, String> additionalQueryParameters) {
        return null;
    }
}
