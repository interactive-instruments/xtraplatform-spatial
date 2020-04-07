/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.geometries.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.BoundingBoxTransformer;
import de.ii.xtraplatform.crs.domain.CoordinateTuple;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import junit.framework.Assert;

import java.util.Random;

/**
 *
 * @author fischer
 */
public class BoundingBoxTransformerTest {

    public BoundingBoxTransformerTest() {
    }

    @org.testng.annotations.BeforeClass(groups = {"default"})
    public void setUp() {
    }

    @org.testng.annotations.Test(groups = {"default"})
    public void testSomeMethod() throws CrsTransformationException {

        BBT bbt = new BBT();

        EpsgCrs crs = EpsgCrs.of(4326);

        // test equality
        BoundingBox b = new BoundingBox(1, 2, 3, 4, crs);
        BoundingBox c = bbt.transformBoundingBox(b);
        Assert.assertEquals(b.toString(), c.toString());

        // stress it a little bit ...
        for (int i = 0; i < 1000000; i++) {
            
            // randomize: oh yes!
            // we could alternatlively test all combinations
            // but this one reflects the behavior (imho) of XtraProxy better
            bbt.setUseRnd(true);

            BoundingBox b1 = new BoundingBox(0, 0, 1, 1, crs);
            BoundingBox b2 = new BoundingBox(1, 0, 2, 1, crs);
            BoundingBox b3 = new BoundingBox(1, 1, 3, 1, crs);
            BoundingBox b4 = new BoundingBox(1, -1, 2, 0, crs);

            BoundingBox c1 = bbt.transformBoundingBox(b1);
            BoundingBox c2 = bbt.transformBoundingBox(b2);
            BoundingBox c3 = bbt.transformBoundingBox(b3);
            BoundingBox c4 = bbt.transformBoundingBox(b4);

            Assert.assertTrue(c1.getXmax() >= c2.getXmin());
            Assert.assertTrue(c2.getYmax() >= c3.getYmin());
            Assert.assertTrue(c4.getYmax() >= c2.getYmin());
        }
    }

    private class BBT extends BoundingBoxTransformer {

        private int cnt = 0;
        private final Random rnd;
        private boolean useRnd = false;

        public BBT() {
            // in this case it is OK to use Random in unittest
            // as the result has to be allways correct!
            rnd = new Random();
        }

        public void setUseRnd(boolean useRnd) {
            this.useRnd = useRnd;
        }

        @Override
        public EpsgCrs getSourceCrs() {
            return null;
        }

        @Override
        public EpsgCrs getTargetCrs() {
            return null;
        }

        @Override
        public boolean isTargetMetric() {
            return false;
        }

        // fake transformation
        @Override
        public CoordinateTuple transform(double x, double y) {
            if (this.useRnd) {
                if (cnt == 1 || cnt == 3) {
                    double eps = 0.1;
                    if (rnd.nextBoolean()) {
                        eps = -eps;
                    }
                    x += eps;
                    y += eps;
                }

                if (cnt < 3) {
                    cnt++;
                } else {
                    cnt = 0;
                }
            }
            return new CoordinateTuple(x, y);
        }

        @Override
        public CoordinateTuple transform(CoordinateTuple coordinateTuple, boolean swap) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double[] transform(double[] coordinates, int numberOfPoints, boolean swap) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double[] transform3d(double[] coordinates, int numberOfPoints, boolean swap) {
            return new double[0];
        }

        @Override
        public double getSourceUnitEquivalentInMeters() {
            return 0;
        }

        @Override
        public double getTargetUnitEquivalentInMeters() {
            return 0;
        }

        @Override
        public boolean needsCoordinateSwap() {
            return false;
        }

        @Override
        public int getSourceDimension() {
            return 2;
        }

        @Override
        public int getTargetDimension() {
            return 2;
        }
    }
}
