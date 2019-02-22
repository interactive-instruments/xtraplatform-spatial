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
