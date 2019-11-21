package de.ii.xtraplatform.feature.provider.api;

import static de.ii.xtraplatform.feature.provider.api.FeatureProviderConnector.QueryOptions;

/**
 * @author zahnen
 *
 * featureQuery -> transformer -> sqlQueries -> connector -> SqlRows -> normalizer -> Features
 *
 *
 * @param <T> query result type
 * @param <U> query type
 * @param <V> options type
 */
public interface FeatureProviderFactory<T,U,V extends QueryOptions> {

    FeatureQueryTransformer<U> getFeatureQueryTransformer();

    FeatureProviderConnector<T,U,V> getFeatureProviderConnector();

    FeatureNormalizer<T> getFeatureNormalizer();
}
