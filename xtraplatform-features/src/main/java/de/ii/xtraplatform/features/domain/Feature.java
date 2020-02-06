package de.ii.xtraplatform.features.domain;

import java.util.List;

//@Value.Immutable
//@Value.Modifiable
//@Value.Style(builder = "new")
public interface Feature<T extends Property<?>> {

    FeatureType getSchema();

    long getIndex();

    FeatureCollection getFeatureCollection();

    List<T> getProperties();

    //List<T> getPropertiesByRoles(FeatureProperty.Role... roles);


    Feature<T> setSchema(FeatureType schema);

    Feature<T> setIndex(long index);

    Feature<T> setFeatureCollection(FeatureCollection featureCollection);

    Feature<T> addProperties(T property);

/*
    @Value.Derived
    @Value.Auxiliary
    String getId();

    @Value.Derived
    @Value.Auxiliary
    Optional<Object> getSpatial();

    @Value.Derived
    @Value.Auxiliary
    Optional<Object> getSingleTemporal();

    @Value.Derived
    @Value.Auxiliary
    Optional<Object> getTemporalIntervalStart();

    @Value.Derived
    @Value.Auxiliary
    Optional<Object> getTemporalIntervalEnd();
*/
}
