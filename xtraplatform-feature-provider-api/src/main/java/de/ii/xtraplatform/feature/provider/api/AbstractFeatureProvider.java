package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.entity.api.AbstractPersistentEntity;

public abstract class AbstractFeatureProvider extends AbstractPersistentEntity<FeatureProviderData> implements FeatureProvider2 {

    @Override
    protected boolean shouldRegister() {
        return true;
    }
}
