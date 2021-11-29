/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xtraplatform.dropwizard.domain.JacksonProvider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * @author zahnen
 *
 *
 * @param <T> query result type
 * @param <U> query type
 * @param <V> options type
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = FeatureProviderConnector.CONNECTOR_TYPE_KEY, visible = true)
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public interface FeatureProviderConnector<T,U,V extends FeatureProviderConnector.QueryOptions> {

    String CONNECTOR_TYPE_KEY = "connectorType";

    interface QueryOptions {}

    String getProviderId();

    boolean isConnected();

    Optional<Throwable> getConnectionError();

    //TODO: refactor FeatureProviderWfs to use getSourceStream, remove this
    @Deprecated
    CompletionStage<Done> runQuery(final FeatureQuery query, final Sink<T, CompletionStage<Done>> consumer, final Map<String, String> additionalQueryParameters);

    Source<T, NotUsed> getSourceStream(U query);

    Source<T, NotUsed> getSourceStream(U query, V options);

    default Tuple<Boolean, String> canBeSharedWith(ConnectionInfo connectionInfo, boolean checkAllParameters) {
        return Tuple.of(false, null);
    }
}
