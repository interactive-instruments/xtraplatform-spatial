/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
