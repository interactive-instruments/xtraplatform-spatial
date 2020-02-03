/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Writer;
import java.util.Objects;

/**
 *
 * @author zahnen
 */
public enum CoordinatesWriterType {

    DEFAULT {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonCoordinatesWriter");
            return new DefaultCoordinatesWriter(builder.formatter, builder.srsDimension);
        }
    },
    SWAP {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonFastXYSwapCoordinatesWriter");
            return new FastXYSwapCoordinatesWriter(builder.formatter, builder.srsDimension);
        }
    },
    TRANSFORM {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonTransCoordinatesWriter");
            return new TransformingCoordinatesWriter(builder.formatter, builder.srsDimension, builder.transformer);
        }
    },
    BUFFER_TRANSFORM {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonBufferedTransformingCoordinatesWriter");
            return new BufferedTransformingCoordinatesWriter(builder.formatter, builder.srsDimension, builder.transformer, builder.swap, builder.reversepolygon, builder.precision);
        }
    },
    SIMPLIFY_BUFFER_TRANSFORM {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonSimplifiyingBufferedTransformingCoordinatesWriter");
            return new SimplifiyingBufferedTransformingCoordinatesWriter(builder.formatter, builder.srsDimension, builder.transformer, builder.simplifier, builder.swap, builder.reversepolygon, builder.precision);
        }
    };

    abstract Writer create(Builder builder);

    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinatesWriterType.class);
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(CoordinateFormatter formatter) {
        return new Builder().format(formatter);
    }
    
    public static class Builder {
        private CoordinateFormatter formatter;
        private int srsDimension;
        private boolean swap;
        private boolean transform;
        private boolean simplify;
        private boolean reversepolygon;
        private CrsTransformer transformer;
        private DouglasPeuckerLineSimplifier simplifier;
        private int precision;
        
        public Builder () {
            this.srsDimension = 2;
            this.swap = false;
            this.transform = false;
            this.simplify = false;
            this.reversepolygon = false;
            this.precision = 0;
        }
        
        public CoordinatesWriterType getType() {
            if (simplify) {
                return CoordinatesWriterType.SIMPLIFY_BUFFER_TRANSFORM;
            }
            else if (reversepolygon) {
                return CoordinatesWriterType.BUFFER_TRANSFORM;
            }
            else if (transform) {
                return CoordinatesWriterType.TRANSFORM;
            }
            else if (swap) {
                return CoordinatesWriterType.SWAP;
            }
            return CoordinatesWriterType.DEFAULT;
        }
        
        public Writer build() {
            if (formatter == null) {
                return null;
            }
            return getType().create(this);
        }
        
        public Builder format(CoordinateFormatter formatter) {
            this.formatter = formatter;
            return this;
        }
        
        public Builder dimension(int dim) {
            this.srsDimension = dim;
            return this;
        }
        
        public Builder swap() {
            this.swap = true;
            return this;
        }
        
        public Builder reversepolygon() {
            this.reversepolygon = true;
            return this;
        }
        
        public Builder transformer(CrsTransformer transformer) {
            this.transformer = transformer;
            this.transform = true;
            return this;
        }
        // TODO: staged builder, has to be set after transformer
        public Builder simplifier(double maxAllowableOffset, int minPoints) {
            this.simplifier = new DouglasPeuckerLineSimplifier(normalizeMaxAllowableOffset(maxAllowableOffset), minPoints);
            this.simplify = true;
            return this;
        }

        public Builder precision(int precision) {
            this.precision = precision;
            this.simplify = true;
            return this;
        }

        private double normalizeMaxAllowableOffset(double maxAllowableOffset) {
            if (Objects.isNull(transformer)) {
                return maxAllowableOffset;
            }

            double requestFactor = transformer.getTargetUnitEquivalentInMeters();
            double localFactor = transformer.getSourceUnitEquivalentInMeters();

            if (requestFactor == 1
                    && localFactor != 1) {
                return maxAllowableOffset / localFactor;

            } else if (requestFactor != 1
                    && localFactor == 1) {
                return maxAllowableOffset * requestFactor;
            }

            return maxAllowableOffset;

        }
    }
}