package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.geometries.domain.BoundingBox;

public interface FeatureExtents {

    BoundingBox getSpatialExtent(String typeName);
}
