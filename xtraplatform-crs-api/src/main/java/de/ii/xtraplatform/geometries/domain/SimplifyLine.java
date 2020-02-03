package de.ii.xtraplatform.geometries.domain;

import org.immutables.value.Value;

import java.io.IOException;
import java.util.Arrays;

@Value.Immutable
public abstract class SimplifyLine implements CoordinatesTransformation {

    @Value.Parameter
    protected abstract double getDistanceTolerance();

    @Value.Parameter
    protected abstract int getMinNumberOfPoints();

    @Override
    public void onCoordinates(double[] coordinates, int length, int dimension) throws IOException {

        double[] simplified = simplify(coordinates, length / dimension, dimension);

        getNext().onCoordinates(simplified, simplified.length, dimension);
    }

    private double[] simplify(double[] points, int numberOfPoints, int dimension) {
        if (getMinNumberOfPoints() > 0 && numberOfPoints <= getMinNumberOfPoints()) {
            return points;
        }

        boolean[] keepPoints = new boolean[numberOfPoints];
        Arrays.fill(keepPoints, true);

        if (getMinNumberOfPoints() > 0) {
            int split = Math.max(getMinNumberOfPoints() - 1, numberOfPoints / getMinNumberOfPoints());

            for (int i = 0; i < numberOfPoints; i = i + split) {
                simplifySection(points, dimension, i, Math.min(i + split, numberOfPoints - 1), keepPoints);
            }
            /*
            simplifySection(points, dimension, 0, split, keepPoints);
            simplifySection(points, dimension, split, split * 2, keepPoints);
            simplifySection(points, dimension, split * 2, split * 3, keepPoints);
            simplifySection(points, dimension, split * 3, split * 4, keepPoints);
            simplifySection(points, dimension, split * 3, numberOfPoints - 1, keepPoints);
             */
        } else {
            simplifySection(points, dimension, 0, numberOfPoints - 1, keepPoints);
        }

        int simplifiedLength = 0;
        for (int i = 0; i < numberOfPoints; i++) {
            if (keepPoints[i]) {
                simplifiedLength += dimension;
            }
        }

        double[] simplifiedPoints = new double[simplifiedLength];
        int cursor = 0;
        for (int i = 0; i < numberOfPoints; i++) {
            if (keepPoints[i]) {
                for (int j = i * dimension; j < i * dimension + dimension; j++) {
                    simplifiedPoints[cursor++] = points[j];
                }
            }
        }

        return simplifiedPoints;
    }

    private void simplifySection(double[] points, int dimension, int start, int end, boolean[] keepPoints) {
        if ((start + 1) == end) {
            return;
        }

        double maxDistance = -1.0;
        int maxIndex = start;
        for (int i = start + 1; i < end; i++) {
            double distance = distance(points[start * dimension], points[start * dimension + 1], points[end * dimension], points[end * dimension + 1], points[i * dimension], points[i * dimension + 1]);
            if (distance > maxDistance) {
                maxDistance = distance;
                maxIndex = i;
            }
        }
        if (maxDistance <= getDistanceTolerance()) {
            for (int i = start + 1; i < end; i++) {
                keepPoints[i] = false;
            }
        } else {
            simplifySection(points, dimension, start, maxIndex, keepPoints);
            simplifySection(points, dimension, maxIndex, end, keepPoints);
        }
    }

    private double distance(double p1x, double p1y, double p2x, double p2y) {
        double dx = p1x - p2x;
        double dy = p1y - p2y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double distance(double l1x, double l1y, double l2x, double l2y, double px, double py) {
        // if start = end, then just compute distance to one of the endpoints
        if (l1x == l2x && l1y == l2y) {
            return distance(l1x, l1y, px, py);
        }

        // otherwise use comp.graphics.algorithms Frequently Asked Questions method
        /*
         * (1) r = AC dot AB
         *         ---------
         *         ||AB||^2
         *
         * r has the following meaning:
         *   r=0 P = A
         *   r=1 P = B
         *   r<0 P is on the backward extension of AB
         *   r>1 P is on the forward extension of AB
         *   0<r<1 P is interior to AB
         */

        double len2 = (l2x - l1x) * (l2x - l1x) + (l2y - l1y) * (l2y - l1y);
        double r = ((px - l1x) * (l2x - l1x) + (py - l1y) * (l2y - l1y))
                / len2;

        if (r <= 0.0) {
            return distance(px, py, l1x, l1y);
        }
        if (r >= 1.0) {
            return distance(px, py, l2x, l2y);
        }

        /*
         * (2) s = (Ay-Cy)(Bx-Ax)-(Ax-Cx)(By-Ay)
         *         -----------------------------
         *                    L^2
         *
         * Then the distance from C to P = |s|*L.
         *
         * This is the same calculation as {@link #distancePointLinePerpendicular}.
         * Unrolled here for performance.
         */
        double s = ((l1y - py) * (l2x - l1x) - (l1x - px) * (l2y - l1y))
                / len2;
        return Math.abs(s) * Math.sqrt(len2);
    }
}
