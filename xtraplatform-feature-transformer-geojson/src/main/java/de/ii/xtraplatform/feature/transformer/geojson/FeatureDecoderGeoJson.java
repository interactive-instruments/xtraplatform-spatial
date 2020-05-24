package de.ii.xtraplatform.feature.transformer.geojson;

import akka.stream.javadsl.Flow;
import akka.util.ByteString;
import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureDecoder;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaMapping;

import java.util.function.Supplier;

public class FeatureDecoderGeoJson implements FeatureDecoder<ByteString> {

    @Override
    public <U extends PropertyBase<U, W>, V extends FeatureBase<U, W>, W extends SchemaBase<W>> Flow<ByteString, V, ?> flow(SchemaMapping<W> schemaMapping, Supplier<V> featureCreator, Supplier<U> propertyCreator) {
        return GeoJsonParserReactive.flowOf(schemaMapping, featureCreator, propertyCreator);
    }
}
