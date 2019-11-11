package de.ii.xtraplatform.feature.provider.sql.domain;

import org.immutables.value.Value;

import java.util.List;

//TODO a feature type may have multiple main tables, ergo multiple FeatureStoreTypeInfo
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface FeatureStoreTypeInfo {

    //TODO: multiple main containers, sorted, important for e.g. limit (Instance(s|Container))
    // main container must not have oid, is that relevant for read or only for write?
    // main container is itself an attribute container and has 0-n child attribute container (Attribute(s|Container))
    // attribute container implements Relation|Connection with join conditions

    String getName();

    List<FeatureStoreInstanceContainer> getInstanceContainers();

}
