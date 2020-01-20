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
package de.ii.xtraplatform.crs.geotools;

import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoToolsCrsTransformation implements CrsTransformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoToolsCrsTransformation.class);

    private final Map<EpsgCrs, Map<EpsgCrs, CrsTransformer>> transformerCache;

    public GeoToolsCrsTransformation() {
        this.transformerCache = new HashMap<>();

        LOGGER.debug("warming up GeoTools ...");

        try {
            new GeoToolsCrsTransformer(CRS.decode("EPSG:4326"), CRS.decode("EPSG:4258"), new EpsgCrs(4326), new EpsgCrs(4258));
        } catch (Throwable ex) {
            //ignore
        }

        LOGGER.debug("done");
    }

    @Override
    public boolean isCrsSupported(String crs) {

        try {
            CRS.decode(applyWorkarounds(crs));
        } catch (FactoryException ex) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isCrsAxisOrderEastNorth(String crs) {
        try {      
            return CRS.getAxisOrder(CRS.decode(applyWorkarounds(crs))) == CRS.AxisOrder.EAST_NORTH;
        } catch (FactoryException ex) {
            // ignore
        }

        return false;
    }

    @Override
    public Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
        if (Objects.isNull(sourceCrs) || Objects.isNull(targetCrs) || Objects.equals(sourceCrs, targetCrs)) {
            return Optional.empty();
        }

        transformerCache.computeIfAbsent(sourceCrs, ignore -> new HashMap<>());
        Map<EpsgCrs, CrsTransformer> transformerCacheForSourceCrs = transformerCache.get(sourceCrs);
        transformerCacheForSourceCrs.computeIfAbsent(targetCrs, ignore -> createCrsTransformer(sourceCrs, targetCrs));

        return Optional.ofNullable(transformerCacheForSourceCrs.get(targetCrs));
    }

    private CrsTransformer createCrsTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
        try {
            return new GeoToolsCrsTransformer(CRS.decode(applyWorkarounds(sourceCrs.getAsSimple()), sourceCrs.isForceLongitudeFirst()), CRS.decode(applyWorkarounds(targetCrs.getAsSimple()), targetCrs.isForceLongitudeFirst()), sourceCrs, targetCrs);
        } catch (FactoryException ex) {
            LOGGER.debug("GeoTools error", ex);
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private String applyWorkarounds(String code) {
        // ArcGIS still uses code 102100, but GeoTools does not support it anymore
        if (code.endsWith("102100")) {
            return code.replace("102100", "3857");
        }
        return code;
    }
}
