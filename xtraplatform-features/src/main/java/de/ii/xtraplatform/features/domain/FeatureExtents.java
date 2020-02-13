package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;

import java.util.Optional;

public interface FeatureExtents {

    Optional<BoundingBox> getSpatialExtent(String typeName);

    Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs);
}
