package de.ii.xtraplatform.features.domain;

import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.immutables.value.Value;

import java.util.function.Supplier;

@Value.Immutable
public interface FeaturePipeline<U extends PropertyBase<U,W>, V extends FeatureBase<U,W>, W extends SchemaBase<W>, X, Y> {

    @Value.Auxiliary
    FeatureDecoder.WithSource getDecoderWithSource();

    @Value.Auxiliary
    SchemaMapping<W> getMapping();

    @Value.Auxiliary
    Supplier<V> getFeatureCreator();

    @Value.Auxiliary
    Supplier<U> getPropertyCreator();

    @Value.Auxiliary
    FeatureEncoder<X, U, V,W> getEncoder();

    @Value.Auxiliary
    Sink<X, Y> getSink();

    @Value.Derived
    default RunnableGraph<Y> getRunnableGraph() {
        Source<V, ?> features = getDecoderWithSource().decode(getMapping(), getFeatureCreator(), getPropertyCreator());

        //TODO: apply ReaderToProcessor

        Source<X, ?> created = features.via(getEncoder().flow());

        return created.toMat(getSink(), Keep.right());
    }


}
