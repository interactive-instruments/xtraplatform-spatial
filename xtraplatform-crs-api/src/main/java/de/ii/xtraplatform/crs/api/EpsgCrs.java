package de.ii.xtraplatform.crs.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Objects;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableEpsgCrs.Builder.class)
public interface EpsgCrs {

    String CRS84 = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    String CRS84h = "http://www.opengis.net/def/crs/OGC/0/CRS84h";

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
        if (Objects.equals(prefixedCode, CRS84)) {
            return of(4326, Force.LON_LAT);
        }
        if (Objects.equals(prefixedCode, CRS84h)) {
            return of(4979, Force.LON_LAT);
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
        if (getCode() == 4326 && getForceLonLat()) {
            return CRS84;
        }
        if (getCode() == 4979 && getForceLonLat()) {
            return CRS84h;
        }
        return String.format("http://www.opengis.net/def/crs/EPSG/0/%d", getCode());
    }
}
