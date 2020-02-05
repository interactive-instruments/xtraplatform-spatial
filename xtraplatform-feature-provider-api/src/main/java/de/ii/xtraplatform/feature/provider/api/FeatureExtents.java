package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.crs.domain.BoundingBox;

public interface FeatureExtents {

    BoundingBox getSpatialExtent(String typeName);
}
