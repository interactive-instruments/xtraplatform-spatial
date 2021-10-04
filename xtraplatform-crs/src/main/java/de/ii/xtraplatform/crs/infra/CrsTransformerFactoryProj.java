/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.infra;

import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.mayThrow;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.measure.Unit;

import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.geotools.referencing.crs.DefaultCompoundCRS;
import org.kortforsyningen.proj.Proj;
import org.kortforsyningen.proj.spi.EPSG;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.util.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class CrsTransformerFactoryProj implements CrsTransformerFactory, CrsInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrsTransformerFactoryProj.class);

    private final CRSAuthorityFactory crsFactory;
    private final Map<EpsgCrs, CoordinateReferenceSystem> crsCache;
    private final Map<EpsgCrs, Map<EpsgCrs, CrsTransformer>> transformerCache;

    public CrsTransformerFactoryProj() {
        this.crsFactory = EPSG.provider();
        this.crsCache = new ConcurrentHashMap<>();
        this.transformerCache = new ConcurrentHashMap<>();

        //TODO: check if warm-up has any effect for proj
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("warming up GeoTools ...");
        }

        try {
            new CrsTransformerProj(crsFactory.createCoordinateReferenceSystem("4326"), crsFactory.createCoordinateReferenceSystem("4258"), EpsgCrs.of(4326), EpsgCrs.of(4258), 2, 2);
        } catch (Throwable ex) {
            //ignore
            boolean br = true;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("done");
        }
    }

    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        try {
            crsCache.computeIfAbsent(crs, mayThrow(ignore -> {
                String code = String.valueOf(applyWorkarounds(crs).getCode());
                CoordinateReferenceSystem coordinateReferenceSystem = crsFactory
                        .createCoordinateReferenceSystem(code);
                coordinateReferenceSystem = applyAxisOrder(coordinateReferenceSystem, crs.getForceAxisOrder());
                if (crs.getVerticalCode().isPresent()) {
                    String verticalCode = String.valueOf(crs.getVerticalCode().getAsInt());
                    CoordinateReferenceSystem verticalCrs = crsFactory.createVerticalCRS(verticalCode);
                    return new DefaultCompoundCRS("", coordinateReferenceSystem, verticalCrs);
                } else {
                    return coordinateReferenceSystem;
                }

            }));
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
    public Unit<?> getCrsUnit(EpsgCrs crs) {
        if (!isCrsSupported(crs)) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", Objects.nonNull(crs) ? crs.toSimpleString() : "null"));
        }

        SingleCRS horizontalCrs = getHorizontalCrs(crsCache.get(crs));

        return horizontalCrs
            .getCoordinateSystem()
            .getAxis(0)
            .getUnit();
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
        int sourceDimension = isCrs3d(sourceCrs) ? 3 : 2;
        int targetDimension = is3dTo3d ? 3 : 2;

        try {
            return new CrsTransformerProj(crsCache.get(sourceCrs), crsCache.get(targetCrs), sourceCrs, targetCrs, sourceDimension, targetDimension);
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

    private SingleCRS getHorizontalCrs(CoordinateReferenceSystem crs) {

        return crs instanceof CompoundCRS
            ? (SingleCRS) ((CompoundCRS) crs).getComponents().get(0)
            : (SingleCRS) crs;
    }

    private CoordinateReferenceSystem applyAxisOrder(CoordinateReferenceSystem crs, EpsgCrs.Force axisOrder) {
        if (axisOrder ==  EpsgCrs.Force.LON_LAT) {
            return Proj.normalizeForVisualization(crs);
        }

        return crs;
    }

    @Override
    public boolean is3d(EpsgCrs crs) {
        return isCrs3d(crs);
    }

    @Override
    public Unit<?> getUnit(EpsgCrs crs) {
        return getCrsUnit(crs);
    }

    @Override
    public List<String> getAxisAbbreviations(EpsgCrs crs) {
        CoordinateReferenceSystem coordinateReferenceSystem = getCoordinateReferenceSystem(crs);
        ImmutableList.Builder<String> abbreviations = new ImmutableList.Builder<>();
        abbreviations.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(0).getAbbreviation());
        abbreviations.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(1).getAbbreviation());
        if (is3d(crs)) {
            abbreviations.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(2).getAbbreviation());
        }

        return abbreviations.build();
    }

    @Override
    public List<Unit<?>> getAxisUnits(EpsgCrs crs) {
        CoordinateReferenceSystem coordinateReferenceSystem = getCoordinateReferenceSystem(crs);
        ImmutableList.Builder<Unit<?>> axisUnits = new ImmutableList.Builder<>();
        axisUnits.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(0).getUnit());
        axisUnits.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(1).getUnit());
        if (is3d(crs)) {
            axisUnits.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(2).getUnit());
        }

        return axisUnits.build();
    }

    @Override
    public Optional<List<RangeMeaning>> getAxisRangeMeanings(EpsgCrs crs) {
        // PROJ-JNI always returns null for axis range meaning requests
        return Optional.empty();
    }

    @Override
    public List<AxisDirection> getAxisDirections(EpsgCrs crs) {
        CoordinateReferenceSystem coordinateReferenceSystem = getCoordinateReferenceSystem(crs);
        ImmutableList.Builder<AxisDirection> axisDirections = new ImmutableList.Builder<>();
        axisDirections.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(0).getDirection());
        axisDirections.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(1).getDirection());
        if (is3d(crs)) {
            axisDirections.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(2).getDirection());
        }

        return axisDirections.build();
    }

    private CoordinateReferenceSystem getCoordinateReferenceSystem(EpsgCrs crs) {
        if (!isCrsSupported(crs)) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", Objects.nonNull(crs) ? crs.toSimpleString() : "null"));
        }
        return crsCache.get(crs);
    }
}
