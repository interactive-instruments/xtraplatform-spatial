package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.crs.api.BoundingBox;

public interface FeatureExtents {

    BoundingBox getSpatialExtent(String typeName);
}
