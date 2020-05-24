package de.ii.xtraplatform.features.domain;

import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;

import java.util.function.Supplier;


//TODO: performant alternative

public interface FeatureDecoder<T> {

    <U extends PropertyBase<U,W>, V extends FeatureBase<U,W>, W extends SchemaBase<W>> Flow<T, V, ?> flow(SchemaMapping<W> schemaMapping,
                                                                                                          Supplier<V> featureCreator,
                                                                                                          Supplier<U> propertyCreator);

    default WithSource withSource(Source<T, ?> source) {
        return new WithSource() {

            @Override
            public <U extends PropertyBase<U,W>, V extends FeatureBase<U,W>, W extends SchemaBase<W>> Source<V, ?> decode(SchemaMapping<W> schemaMapping,
                                                                                                                          Supplier<V> featureCreator,
                                                                                                                          Supplier<U> propertyCreator) {

                return source.via(FeatureDecoder.this.flow(schemaMapping, featureCreator, propertyCreator));
            }
        };
    }

    interface WithSource {
        <U extends PropertyBase<U,W>, V extends FeatureBase<U,W>, W extends SchemaBase<W>> Source<V, ?> decode(SchemaMapping<W> schemaMapping,
                                                                                                               Supplier<V> featureCreator,
                                                                                                               Supplier<U> propertyCreator);
    }
}
