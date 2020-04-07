package de.ii.xtraplatform.feature.provider.api;


import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;

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
public interface FeatureProviderFactory<T,U,V extends FeatureProviderConnector.QueryOptions> {

    FeatureQueryTransformer<U> getFeatureQueryTransformer();

    FeatureProviderConnector<T,U,V> getFeatureProviderConnector();

    FeatureNormalizer<T> getFeatureNormalizer();
}
