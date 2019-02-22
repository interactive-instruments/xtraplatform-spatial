package de.ii.xtraplatform.feature.provider.api;

import akka.Done;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xsf.dropwizard.cfg.JacksonProvider;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * @author zahnen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "connectorType", visible = true)
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public interface FeatureProviderConnector {

    CompletionStage<Done> runQuery(final FeatureQuery query, final Sink<ByteString, CompletionStage<Done>> transformer, final Map<String, String> additionalQueryParameters);
}
