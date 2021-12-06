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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.nativ.proj.api.ProjLoader;
import de.ii.xtraplatform.runtime.domain.LogContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.measure.Unit;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.kortforsyningen.proj.CoordinateOperationContext;
import org.kortforsyningen.proj.Proj;
import org.kortforsyningen.proj.spi.EPSG;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.operation.CoordinateOperation;
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

    //private final CRSAuthorityFactory crsAuthorityFactory;
    //private final CRSFactory crsFactory;
    //private final Map<EpsgCrs, CoordinateReferenceSystem> crsCache;
    //private final Map<EpsgCrs, Map<EpsgCrs, CrsTransformer>> transformerCache;

    public CrsTransformerFactoryProj(@Requires ProjLoader projLoader) {
        try {
            projLoader.load();
            Proj.setSearchPath(projLoader.getDataDirectory().toString());
            Proj.version().ifPresent(version -> LOGGER.debug("PROJ version: {}", version));
        } catch (Throwable e) {
            LogContext.error(LOGGER, e, "PROJ");
        }

        //this.crsAuthorityFactory = EPSG.provider();
        //this.crsFactory = Proj.getFactory(CRSFactory.class);
        //this.crsCache = new ConcurrentHashMap<>();
        //this.transformerCache = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        return getProjCrs(crs).isPresent();
        /*try {
            crsCache.computeIfAbsent(crs, mayThrow(ignore -> {
                String code = String.valueOf(applyWorkarounds(crs).getCode());
                CoordinateReferenceSystem coordinateReferenceSystem = crsAuthorityFactory
                        .createCoordinateReferenceSystem(code);
                coordinateReferenceSystem = applyAxisOrder(coordinateReferenceSystem, crs.getForceAxisOrder());
                if (crs.getVerticalCode().isPresent()) {
                    String verticalCode = String.valueOf(crs.getVerticalCode().getAsInt());
                    CoordinateReferenceSystem verticalCrs = crsAuthorityFactory.createVerticalCRS(verticalCode);
                    return crsFactory.createCompoundCRS(ImmutableMap.of("name", String.format("%s + %s", coordinateReferenceSystem.getName(), verticalCrs.getName())), coordinateReferenceSystem, verticalCrs);
                } else {
                    return coordinateReferenceSystem;
                }

            }));
        } catch (Throwable e) {
            return false;
        }

        return true;*/
    }

    private synchronized Optional<CoordinateReferenceSystem> getProjCrs(EpsgCrs crs) {
        try {
                String code = String.valueOf(applyWorkarounds(crs).getCode());
                CoordinateReferenceSystem coordinateReferenceSystem = EPSG.provider()
                    .createCoordinateReferenceSystem(code);
                coordinateReferenceSystem = applyAxisOrder(coordinateReferenceSystem, crs.getForceAxisOrder());

                if (crs.getVerticalCode().isPresent()) {
                    String verticalCode = String.valueOf(crs.getVerticalCode().getAsInt());
                    CoordinateReferenceSystem verticalCrs = EPSG.provider().createVerticalCRS(verticalCode);
                    CoordinateReferenceSystem compoundCrs = Proj.getFactory(CRSFactory.class).createCompoundCRS(ImmutableMap.of("name", String.format("%s + %s", coordinateReferenceSystem.getName(), verticalCrs.getName())), coordinateReferenceSystem, verticalCrs);

                    return Optional.of(compoundCrs);
                }
                    return Optional.of(coordinateReferenceSystem);
        } catch (Throwable e) {
            LogContext.error(LOGGER, e, "PROJ");
            return Optional.empty();
        }
    }

    private synchronized Optional<CoordinateReferenceSystem> getProjCrs(EpsgCrs crs, CRSAuthorityFactory authorityFactory, CRSFactory crsFactory) {
        try {
            String code = String.valueOf(applyWorkarounds(crs).getCode());
            CoordinateReferenceSystem coordinateReferenceSystem = authorityFactory.createCoordinateReferenceSystem(code);
            coordinateReferenceSystem = applyAxisOrder(coordinateReferenceSystem, crs.getForceAxisOrder());

            if (crs.getVerticalCode().isPresent()) {
                String verticalCode = String.valueOf(crs.getVerticalCode().getAsInt());
                CoordinateReferenceSystem verticalCrs = authorityFactory.createVerticalCRS(verticalCode);
                CoordinateReferenceSystem compoundCrs = crsFactory.createCompoundCRS(ImmutableMap.of("name", String.format("%s + %s", coordinateReferenceSystem.getName(), verticalCrs.getName())), coordinateReferenceSystem, verticalCrs);

                return Optional.of(compoundCrs);
            }
            return Optional.of(coordinateReferenceSystem);
        } catch (Throwable e) {
            LogContext.error(LOGGER, e, "PROJ");
            return Optional.empty();
        }
    }

    @Override
    public boolean isCrs3d(EpsgCrs crs) {
        return getProjCrs(crs).filter(c -> c.getCoordinateSystem().getDimension() == 3).isPresent();
        //return isCrsSupported(crs) && crsCache.get(crs).getCoordinateSystem().getDimension() == 3;
    }

    private boolean isCrs3d(CoordinateReferenceSystem crs) {
        return crs.getCoordinateSystem().getDimension() == 3;
        //return isCrsSupported(crs) && crsCache.get(crs).getCoordinateSystem().getDimension() == 3;
    }

    @Override
    public Unit<?> getCrsUnit(EpsgCrs crs) {
        if (!isCrsSupported(crs)) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", Objects.nonNull(crs) ? crs.toSimpleString() : "null"));
        }

        SingleCRS horizontalCrs = getHorizontalCrs(getProjCrs(crs).get());
        //SingleCRS horizontalCrs = getHorizontalCrs(crsCache.get(crs));

        return horizontalCrs
            .getCoordinateSystem()
            .getAxis(0)
            .getUnit();
    }

    @Override
    public synchronized Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
        if (Objects.equals(sourceCrs, targetCrs)) {
            return Optional.empty();
        }
        CRSAuthorityFactory authorityFactory = EPSG.provider();
        CRSFactory crsFactory = Proj.getFactory(CRSFactory.class);
        Optional<CoordinateReferenceSystem> sourceProjCrs = getProjCrs(sourceCrs, authorityFactory,crsFactory);
        Optional<CoordinateReferenceSystem> targetProjCrs = getProjCrs(targetCrs, authorityFactory, crsFactory);
        if (/*!isCrsSupported(sourceCrs)*/sourceProjCrs.isEmpty()) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", Objects.nonNull(sourceCrs) ? sourceCrs.toSimpleString() : "null"));
        }
        if (/*!isCrsSupported(targetCrs)*/targetProjCrs.isEmpty()) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", Objects.nonNull(targetCrs) ? targetCrs.toSimpleString() : "null"));
        }

        //transformerCache.computeIfAbsent(sourceCrs, ignore -> new ConcurrentHashMap<>());
        //Map<EpsgCrs, CrsTransformer> transformerCacheForSourceCrs = transformerCache.get(sourceCrs);
        //transformerCacheForSourceCrs.computeIfAbsent(targetCrs, ignore -> createCrsTransformer(sourceCrs, targetCrs));


        return Optional.of(createCrsTransformer(sourceCrs, targetCrs, sourceProjCrs.get(), targetProjCrs.get()));
        //return Optional.of(createCrsTransformer(sourceCrs, targetCrs));
        //return Optional.ofNullable(transformerCacheForSourceCrs.get(targetCrs));
    }

    private synchronized CrsTransformer createCrsTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs, CoordinateReferenceSystem sourceProjCrs, CoordinateReferenceSystem targetProjCrs) {
        boolean is3dTo3d = isCrs3d(sourceProjCrs) && isCrs3d(targetProjCrs);
        int sourceDimension = isCrs3d(sourceProjCrs) ? 3 : 2;
        int targetDimension = is3dTo3d ? 3 : 2;

        try {
            CoordinateOperationContext coordinateOperationContext = new CoordinateOperationContext();
            coordinateOperationContext.setAuthority("EPSG");
            CoordinateOperation coordinateOperation = Proj.createCoordinateOperation(sourceProjCrs,
                targetProjCrs, coordinateOperationContext);
            return new CrsTransformerProj(sourceProjCrs, targetProjCrs, sourceCrs, targetCrs, sourceDimension, targetDimension, coordinateOperation);
            //return new CrsTransformerProj(getProjCrs(sourceCrs).get(), getProjCrs(targetCrs).get(), sourceCrs, targetCrs, sourceDimension, targetDimension);
            //return new CrsTransformerProj(crsCache.get(sourceCrs), crsCache.get(targetCrs), sourceCrs, targetCrs, sourceDimension, targetDimension);
        } catch (FactoryException ex) {
            LogContext.errorAsDebug(LOGGER, ex, "PROJ");
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private synchronized CrsTransformer createCrsTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
        boolean is3dTo3d = isCrs3d(sourceCrs) && isCrs3d(targetCrs);
        int sourceDimension = isCrs3d(sourceCrs) ? 3 : 2;
        int targetDimension = is3dTo3d ? 3 : 2;

        try {
            return new CrsTransformerProj(getProjCrs(sourceCrs).get(), getProjCrs(targetCrs).get(), sourceCrs, targetCrs, sourceDimension, targetDimension);
            //return new CrsTransformerProj(crsCache.get(sourceCrs), crsCache.get(targetCrs), sourceCrs, targetCrs, sourceDimension, targetDimension);
        } catch (FactoryException ex) {
            LogContext.errorAsDebug(LOGGER, ex, "PROJ");
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }


    private EpsgCrs applyWorkarounds(EpsgCrs crs) {
        // ArcGIS still uses code 102100 instead of 3857, but proj does not support it anymore
        if (crs.getCode() == 102100) {
            return new ImmutableEpsgCrs.Builder().from(crs).code(3857).build();
        }
        // FME uses code 900914 instead of 4979, but proj does not support this
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
        CoordinateReferenceSystem coordinateReferenceSystem = getCrsOrThrow(crs);
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
        CoordinateReferenceSystem coordinateReferenceSystem = getCrsOrThrow(crs);
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
        CoordinateReferenceSystem coordinateReferenceSystem = getCrsOrThrow(crs);
        ImmutableList.Builder<AxisDirection> axisDirections = new ImmutableList.Builder<>();
        axisDirections.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(0).getDirection());
        axisDirections.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(1).getDirection());
        if (is3d(crs)) {
            axisDirections.add(coordinateReferenceSystem.getCoordinateSystem().getAxis(2).getDirection());
        }

        return axisDirections.build();
    }

    private CoordinateReferenceSystem getCrsOrThrow(EpsgCrs crs) {
        if (!isCrsSupported(crs)) {
            throw new IllegalArgumentException(String.format("CRS %s is not supported.", Objects.nonNull(crs) ? crs.toSimpleString() : "null"));
        }
        return getProjCrs(crs).get();
        //return crsCache.get(crs);
    }
}
