package de.ii.xtraplatform.geometries.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableEpsgCrs.Builder.class)
public interface EpsgCrs {

    enum Force {LON_LAT, LAT_LON}

    static EpsgCrs of(int code) {
        return ImmutableEpsgCrs.of(code);
    }

    static EpsgCrs of(int code, Force force) {
        return new ImmutableEpsgCrs.Builder().code(code)
                                             .forceLonLat(force == Force.LON_LAT)
                                             .forceLatLon(force == Force.LAT_LON)
                                             .build();
    }

    static EpsgCrs fromString(String prefixedCode) {
        Optional<EpsgCrs> ogcCrs = OgcCrs.fromString(prefixedCode);
        if (ogcCrs.isPresent()) {
            return ogcCrs.get();
        }

        int code;
        try {
            code = Integer.parseInt(prefixedCode.substring(prefixedCode.lastIndexOf(":") + 1));
        } catch (NumberFormatException e) {
            try {
                code = Integer.parseInt(prefixedCode.substring(prefixedCode.lastIndexOf("/") + 1));
            } catch (NumberFormatException e2) {
                try {
                    code = Integer.parseInt(prefixedCode);
                } catch (NumberFormatException e3) {
                    throw new IllegalArgumentException("Could not parse CRS: " + prefixedCode);
                }
            }
        }
        return ImmutableEpsgCrs.of(code);
    }

    @Value.Parameter
    int getCode();

    @JsonAlias("forceLongitudeFirst")
    @Value.Default
    default boolean getForceLonLat() {
        return false;
    }

    @Value.Default
    default boolean getForceLatLon() {
        return false;
    }

    @Value.Lazy
    default String toSimpleString() {
        return String.format("EPSG:%d", getCode());
    }

    @Value.Lazy
    default String toUrnString() {
        return String.format("urn:ogc:def:crs:EPSG::%d", getCode());
    }

    @Value.Lazy
    default String toUriString() {
        if (Objects.equals(this, OgcCrs.CRS84)) {
            return OgcCrs.CRS84_URI;
        }
        if (Objects.equals(this, OgcCrs.CRS84h)) {
            return OgcCrs.CRS84h_URI;
        }
        return String.format("http://www.opengis.net/def/crs/EPSG/0/%d", getCode());
    }
}
