/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import org.immutables.value.Value;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
public abstract class CoordinatesTransformer extends Writer {

    protected abstract int getSourceDimension();

    protected abstract int getTargetDimension();

    protected abstract CoordinatesWriter<?> getCoordinatesWriter();

    protected abstract Optional<CrsTransformer> getCrsTransformer();

    @Value.Default
    protected boolean isSwapXY() {
        return false;
    }

    @Value.Default
    protected boolean isReverseOrder() {
        return false;
    }

    @Value.Default
    protected double getMaxAllowableOffset() {
        return 0.0;
    }

    @Value.Default
    protected int getMinNumberOfCoordinates() {
        return 0;
    }

    @Value.Default
    protected int getPrecision() {
        return 0;
    }

    @Value.Derived
    protected double getNormalizedMaxAllowableOffset() {
        if (!getCrsTransformer().isPresent()) {
            return getMaxAllowableOffset();
        }

        double requestFactor = getCrsTransformer().get().getTargetUnitEquivalentInMeters();
        double localFactor = getCrsTransformer().get().getSourceUnitEquivalentInMeters();

        if (requestFactor == 1 && localFactor != 1) {
            return getMaxAllowableOffset() / localFactor;

        } else if (requestFactor != 1 && localFactor == 1) {
            return getMaxAllowableOffset() * requestFactor;
        }

        return getMaxAllowableOffset();
    }

    @Value.Derived
    protected Optional<DoubleArrayProcessor> getTransformationPipeline() {
        DoubleArrayProcessor lastTransformation = ImmutableToChars.of(getCoordinatesWriter(), getPrecision());
        boolean doTransform = false;

        //last
        if (isSwapXY()) {
            lastTransformation = ImmutableSwapXY.of(lastTransformation);
            doTransform = true;
        }

        if (isReverseOrder()) {
            lastTransformation = ImmutableReverseLine.of(lastTransformation);
            doTransform = true;
        }

        if (getCrsTransformer().isPresent()) {
            lastTransformation = ImmutableCrsTransform.of(lastTransformation, getCrsTransformer().get());
            doTransform = true;
        }

        //first
        if (getNormalizedMaxAllowableOffset() > 0.0) {
            lastTransformation = ImmutableSimplifyLine.of(lastTransformation, getNormalizedMaxAllowableOffset(), getMinNumberOfCoordinates());
            doTransform = true;
        }

        return doTransform ? Optional.of(lastTransformation) : Optional.empty();
    }

    @Value.Derived
    protected SeperateStringsProcessor getCoordinatesProcessor() {
        Optional<DoubleArrayProcessor> transformationPipeline = getTransformationPipeline();

        if (transformationPipeline.isPresent()) {
            return ImmutableToDoubleArray.of(transformationPipeline.get(), getTargetDimension());
        }

        return getCoordinatesWriter();
    }

    @Value.Derived
    protected CoordinatesParser getCoordinatesParser() {
        return new CoordinatesParser(getCoordinatesWriter(), getSourceDimension(), getTargetDimension());
    }


    @Override
    public void write(char[] chars, int i, int i1) throws IOException {
        getCoordinatesParser().parse(chars, i, i1);
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {
        getCoordinatesParser().close();
    }
}
