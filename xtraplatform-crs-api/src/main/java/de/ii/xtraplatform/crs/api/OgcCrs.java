package de.ii.xtraplatform.crs.api;

import de.ii.xtraplatform.crs.api.EpsgCrs.Force;

import java.util.Objects;
import java.util.Optional;

public interface OgcCrs {

    EpsgCrs CRS84 = EpsgCrs.of(4326, Force.LON_LAT);

    String CRS84_URI = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

    EpsgCrs CRS84h = EpsgCrs.of(4979, Force.LON_LAT);

    String CRS84h_URI = "http://www.opengis.net/def/crs/OGC/0/CRS84h";

    static Optional<EpsgCrs> fromString(String prefixedCode) {
        if (Objects.equals(prefixedCode, CRS84_URI)) {
            return Optional.of(CRS84);
        }
        if (Objects.equals(prefixedCode, CRS84h_URI)) {
            return Optional.of(CRS84h);
        }

        return Optional.empty();
    }
}
