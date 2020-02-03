/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.app;

import de.ii.xtraplatform.geometries.domain.CrsTransformer;
import de.ii.xtraplatform.geometries.domain.CrsTransformerFactory;
import de.ii.xtraplatform.geometries.domain.EpsgCrs;
import de.ii.xtraplatform.geometries.domain.ImmutableEpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.ii.xtraplatform.api.functional.LambdaWithException.mayThrow;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoToolsCrsTransformerFactory implements CrsTransformerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoToolsCrsTransformerFactory.class);

    private final Map<EpsgCrs, CoordinateReferenceSystem> crsCache;
    private final Map<EpsgCrs, Map<EpsgCrs, CrsTransformer>> transformerCache;

    public GeoToolsCrsTransformerFactory() {
        this.crsCache = new HashMap<>();
        this.transformerCache = new HashMap<>();

        //TODO: at service start
        /*
        LOGGER.debug("warming up GeoTools ...");

        try {
            new GeoToolsCrsTransformer(CRS.decode("EPSG:4326"), CRS.decode("EPSG:4258"), new EpsgCrs(4326), new EpsgCrs(4258));
        } catch (Throwable ex) {
            //ignore
        }

        LOGGER.debug("done");

         */
    }

    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        try {
            crsCache.computeIfAbsent(crs, mayThrow(ignore -> CRS.decode(applyWorkarounds(crs).toSimpleString(), crs.getForceLonLat())));
        } catch (Throwable e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isCrs3d(EpsgCrs crs) {
        return isCrsSupported(crs) && Objects.nonNull(CRS.getVerticalCRS(crsCache.get(crs)));
    }

    @Override
    public Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
        if (Objects.equals(sourceCrs, targetCrs)) {
            return Optional.empty();
        }
        if (!isCrsSupported(sourceCrs)) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", sourceCrs.toSimpleString()));
        }
        if (!isCrsSupported(targetCrs)) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", targetCrs.toSimpleString()));
        }

        transformerCache.computeIfAbsent(sourceCrs, ignore -> new HashMap<>());
        Map<EpsgCrs, CrsTransformer> transformerCacheForSourceCrs = transformerCache.get(sourceCrs);
        transformerCacheForSourceCrs.computeIfAbsent(targetCrs, ignore -> createCrsTransformer(sourceCrs, targetCrs));

        return Optional.ofNullable(transformerCacheForSourceCrs.get(targetCrs));
    }

    private CrsTransformer createCrsTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
        boolean preserveHeight = isCrs3d(sourceCrs) && isCrs3d(targetCrs);

        if (preserveHeight) {
            SingleCRS horizontalSourceCrs = CRS.getHorizontalCRS(crsCache.get(sourceCrs));
            SingleCRS horizontalTargetCrs = CRS.getHorizontalCRS(crsCache.get(targetCrs));

            try {
                return new GeoToolsCrsTransformer(horizontalSourceCrs, horizontalTargetCrs, sourceCrs, targetCrs, true);
            } catch (FactoryException ex) {
                LOGGER.debug("GeoTools error", ex);
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }

        try {
            return new GeoToolsCrsTransformer(crsCache.get(sourceCrs), crsCache.get(targetCrs), sourceCrs, targetCrs, false);
        } catch (FactoryException ex) {
            LOGGER.debug("GeoTools error", ex);
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private EpsgCrs applyWorkarounds(EpsgCrs crs) {
        // ArcGIS still uses code 102100, but GeoTools does not support it anymore
        if (crs.getCode() == 102100) {
            return new ImmutableEpsgCrs.Builder().from(crs).code(3857).build();
        }
        return crs;
    }
}
