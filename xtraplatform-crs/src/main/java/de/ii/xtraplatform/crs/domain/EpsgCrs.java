package de.ii.xtraplatform.crs.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableEpsgCrs.Builder.class)
//TODO: test
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public interface EpsgCrs {

    enum Force {NONE, LON_LAT, LAT_LON}

    static EpsgCrs of(int code) {
        return ImmutableEpsgCrs.of(code);
    }

    static EpsgCrs of(int code, Force force) {
        return new ImmutableEpsgCrs.Builder().code(code)
                                             .forceAxisOrder(force)
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

    //TODO: migrate
    @Deprecated
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Value.Default
    @Value.Auxiliary
    default boolean getForceLongitudeFirst() {
        return false;
    }

    @Value.Default
    default Force getForceAxisOrder() {
        return getForceLongitudeFirst() ? Force.LON_LAT : Force.NONE;
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
