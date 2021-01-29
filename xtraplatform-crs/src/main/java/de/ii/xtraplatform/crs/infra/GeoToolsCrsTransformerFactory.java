/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.infra;

import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.mayThrow;

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
        this.crsCache = new ConcurrentHashMap<>();
        this.transformerCache = new ConcurrentHashMap<>();

        //TODO: at service start

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("warming up GeoTools ...");
        }

        try {
            new GeoToolsCrsTransformer(CRS.decode("EPSG:4326"), CRS.decode("EPSG:4258"), EpsgCrs.of(4326), EpsgCrs.of(4258), 2, 2);
        } catch (Throwable ex) {
            //ignore
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("done");
        }


    }

    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        try {
            crsCache.computeIfAbsent(crs, mayThrow(ignore -> CRS.decode(applyWorkarounds(crs).toSimpleString(), crs.getForceAxisOrder() == Force.LON_LAT)));
        } catch (Throwable e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isCrs3d(EpsgCrs crs) {
        return isCrsSupported(crs) && crsCache.get(crs).getCoordinateSystem().getDimension() == 3;
    }

    @Override
    public Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
        if (Objects.equals(sourceCrs, targetCrs)) {
            return Optional.empty();
        }
        if (!isCrsSupported(sourceCrs)) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", Objects.nonNull(sourceCrs) ? sourceCrs.toSimpleString() : "null"));
        }
        if (!isCrsSupported(targetCrs)) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", Objects.nonNull(targetCrs) ? targetCrs.toSimpleString() : "null"));
        }

        transformerCache.computeIfAbsent(sourceCrs, ignore -> new ConcurrentHashMap<>());
        Map<EpsgCrs, CrsTransformer> transformerCacheForSourceCrs = transformerCache.get(sourceCrs);
        transformerCacheForSourceCrs.computeIfAbsent(targetCrs, ignore -> createCrsTransformer(sourceCrs, targetCrs));

        return Optional.ofNullable(transformerCacheForSourceCrs.get(targetCrs));
    }

    private CrsTransformer createCrsTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
        boolean is3dTo3d = isCrs3d(sourceCrs) && isCrs3d(targetCrs);
        boolean is3dTo2d = isCrs3d(sourceCrs) && !isCrs3d(targetCrs);
        boolean is2dTo3d = !isCrs3d(sourceCrs) && isCrs3d(targetCrs);
        int sourceDimension = isCrs3d(sourceCrs) ? 3 : 2;
        int targetDimension = is3dTo3d ? 3 : 2;
        CoordinateReferenceSystem geotoolsSourceCrs = is3dTo3d || is3dTo2d ? CRS.getHorizontalCRS(crsCache.get(sourceCrs)) : crsCache.get(sourceCrs);
        CoordinateReferenceSystem geotoolsTargetCrs = is3dTo3d || is2dTo3d ? CRS.getHorizontalCRS(crsCache.get(targetCrs)) : crsCache.get(targetCrs);

        try {
            return new GeoToolsCrsTransformer(geotoolsSourceCrs, geotoolsTargetCrs, sourceCrs, targetCrs, sourceDimension, targetDimension);
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
        // FME uses code 900914 instead of 4979, but GeoTools does not support this
        if (crs.getCode() == 900914) {
            return new ImmutableEpsgCrs.Builder().from(crs).code(4979).forceAxisOrder(Force.LON_LAT).build();
        }
        return crs;
    }
}
