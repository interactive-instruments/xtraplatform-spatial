package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.feature.provider.api.FeatureProviderConnector;
import org.immutables.value.Value;

@Value.Immutable
public interface SqlQueryOptions extends FeatureProviderConnector.QueryOptions {

    FeatureStoreAttributesContainer getAttributesContainer();

    int getContainerPriority();
}
