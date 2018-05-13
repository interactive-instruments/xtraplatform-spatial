/**
 * Copyright 2018 interactive instruments GmbH
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

import javax.measure.unit.SI;
import javax.measure.unit.Unit;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoToolsCrsTransformation implements CrsTransformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoToolsCrsTransformation.class);

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
    public CrsTransformer getTransformer(String sourceCrs, String targetCrs) {
        try {
            return new GeoToolsCrsTransformer(CRS.decode(applyWorkarounds(sourceCrs)), CRS.decode(applyWorkarounds(targetCrs)));
        } catch (FactoryException ex) {
            LOGGER.debug("GeoTools error", ex);
        }
        return null;
    }

    @Override
    public CrsTransformer getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
        try {
            return new GeoToolsCrsTransformer(CRS.decode(applyWorkarounds(sourceCrs.getAsSimple()), sourceCrs.isLongitudeFirst()), CRS.decode(applyWorkarounds(targetCrs.getAsSimple()), targetCrs.isLongitudeFirst()));
        } catch (FactoryException ex) {
            LOGGER.debug("GeoTools error", ex);
        }
        return null;
    }

    private String applyWorkarounds(String code) {
        // ArcGIS still uses code 102100, but GeoTools does not support it anymore
        if (code.endsWith("102100")) {
            return code.replace("102100", "3857");
        }
        return code;
    }

    @Override
    public double getUnitEquivalentInMeter(String crs) {
        try {
            Unit unit = CRS.getHorizontalCRS(CRS.decode(applyWorkarounds(crs))).getCoordinateSystem().getAxis(0).getUnit();

            if( unit != SI.METER) {
                return (Math.PI/180.00) * CRS.getEllipsoid(CRS.decode(applyWorkarounds(crs))).getSemiMajorAxis();
            }
        } catch (FactoryException ex) {
            LOGGER.debug("GeoTools error", ex);
        }

        return 1;
    }
}
