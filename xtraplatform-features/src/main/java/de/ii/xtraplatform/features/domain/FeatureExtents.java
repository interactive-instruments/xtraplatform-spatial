package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;

public interface FeatureExtents {

    BoundingBox getSpatialExtent(String typeName);
}
