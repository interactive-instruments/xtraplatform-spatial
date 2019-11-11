package de.ii.xtraplatform.feature.provider.sql.domain;

public interface FeatureStoreContainer {

    String getName();

    String getPath();

    String getDefaultSortKey();

    //TODO
    //boolean shouldAutoGenerateId();
}
