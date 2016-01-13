/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xtraplatform.crs.api;

import java.util.Random;
import junit.framework.Assert;

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

        EpsgCrs crs = new EpsgCrs();

        // test equality
        BoundingBox b = new BoundingBox(1, 2, 3, 4, crs);
        BoundingBox c = bbt.transformBoundingBox(b, crs);
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

            BoundingBox c1 = bbt.transformBoundingBox(b1, crs);
            BoundingBox c2 = bbt.transformBoundingBox(b2, crs);
            BoundingBox c3 = bbt.transformBoundingBox(b3, crs);
            BoundingBox c4 = bbt.transformBoundingBox(b4, crs);

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
        public CoordinateTuple transform(CoordinateTuple coordinateTuple) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double[] transform(double[] coordinates, int numberOfPoints) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
