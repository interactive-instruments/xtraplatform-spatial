package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.geometries.domain.BoundingBox;
import de.ii.xtraplatform.geometries.domain.EpsgCrs;

import java.util.Optional;

public interface SqlDialect {

    String applyToWkt(String column);

    String applyToExtent(String column);

    Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs);

}
