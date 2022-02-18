/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.crs.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.proj.domain.ProjLoader;
import de.ii.xtraplatform.base.domain.LogContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import javax.measure.Unit;
import org.kortforsyningen.proj.CoordinateOperationContext;
import org.kortforsyningen.proj.GridAvailabilityUse;
import org.kortforsyningen.proj.Proj;
import org.kortforsyningen.proj.SpatialCriterion;
import org.kortforsyningen.proj.spi.EPSG;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.operation.CoordinateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zahnen
 */
@Singleton
@AutoBind
public class CrsTransformerFactoryProj implements CrsTransformerFactory, CrsInfo {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrsTransformerFactoryProj.class);

  private final Map<EpsgCrs, CoordinateReferenceSystem> crsCache;
  private final Map<EpsgCrs, Map<EpsgCrs, CrsTransformer>> transformerCache;
  private final boolean useCaches = true;

  @Inject
  public CrsTransformerFactoryProj(ProjLoader projLoader) {
    this.crsCache = new ConcurrentHashMap<>();
    this.transformerCache = new ConcurrentHashMap<>();

    try {
      projLoader.load();
      //TODO: to projLoader?
      if (!projLoader.getDataDirectory().toFile().exists() || !projLoader.getDataDirectory().resolve("proj.db").toFile().exists()) {
        throw new IllegalArgumentException("Not a valid PROJ location: " + projLoader.getDataDirectory());
      }
      Proj.setSearchPath(projLoader.getDataDirectory().toString());
      Proj.version().ifPresent(version -> LOGGER.debug("PROJ version: {}, location: {}", version, projLoader.getDataDirectory()));
      System.setProperty("org.kortforsyningen.proj.maxThreadsPerInstance", "16");
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "PROJ");
      throw e;
    }
  }

  @Override
  public boolean isSupported(EpsgCrs crs) {
    return getCrs(crs).isPresent();
  }

  @Override
  public boolean is3d(EpsgCrs crs) {
    return getCrs(crs).filter(this::isCrs3d).isPresent();
  }

  @Override
  public Unit<?> getUnit(EpsgCrs crs) {
    return getHorizontalCrs(getCrsOrThrow(crs))
        .getCoordinateSystem()
        .getAxis(0)
        .getUnit();
  }

  @Override
  public List<String> getAxisAbbreviations(EpsgCrs crs) {
    CoordinateReferenceSystem projCrs = getCrsOrThrow(crs);

    return IntStream.range(0, is3d(crs) ? 3 : 2)
        .mapToObj(i -> projCrs.getCoordinateSystem().getAxis(i).getAbbreviation())
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<Unit<?>> getAxisUnits(EpsgCrs crs) {
    CoordinateReferenceSystem projCrs = getCrsOrThrow(crs);

    return IntStream.range(0, is3d(crs) ? 3 : 2)
        .mapToObj(i -> projCrs.getCoordinateSystem().getAxis(i).getUnit())
        .collect(ImmutableList.toImmutableList());
  }

  //TODO: it seems that PROJ always returns null for axis range meaning requests
  @Override
  public List<Optional<RangeMeaning>> getAxisRangeMeanings(EpsgCrs crs) {
    CoordinateReferenceSystem projCrs = getCrsOrThrow(crs);

    return IntStream.range(0, is3d(crs) ? 3 : 2)
        .mapToObj(i -> projCrs.getCoordinateSystem().getAxis(i).getRangeMeaning())
        .map(Optional::ofNullable)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<AxisDirection> getAxisDirections(EpsgCrs crs) {
    CoordinateReferenceSystem projCrs = getCrsOrThrow(crs);

    return IntStream.range(0, is3d(crs) ? 3 : 2)
        .mapToObj(i -> projCrs.getCoordinateSystem().getAxis(i).getDirection())
        .collect(ImmutableList.toImmutableList());
  }
  @Override
  public Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs) {
    return getTransformer(sourceCrs, targetCrs, false);
  }

  @Override
  public Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs, boolean force2d) {
    if (Objects.equals(sourceCrs, targetCrs)) {
      return Optional.empty();
    }
    CoordinateReferenceSystem sourceProjCrs = getCrsOrThrow(sourceCrs);
    CoordinateReferenceSystem targetProjCrs = getCrsOrThrow(targetCrs);

    if (force2d) {
      sourceProjCrs = getHorizontalCrs(sourceProjCrs);
      targetProjCrs = getHorizontalCrs(targetProjCrs);
    }

    CoordinateReferenceSystem finalSourceProjCrs = sourceProjCrs;
    CoordinateReferenceSystem finalTargetProjCrs = targetProjCrs;
    CrsTransformer transformer = useCaches
        ? getCacheForSource(sourceCrs).computeIfAbsent(targetCrs, ignore -> createCrsTransformer(sourceCrs, targetCrs,
                                                                                                 finalSourceProjCrs, finalTargetProjCrs))
        : createCrsTransformer(sourceCrs, targetCrs, sourceProjCrs, targetProjCrs);

    return Optional.of(transformer);
  }

  private Optional<CoordinateReferenceSystem> getCrs(EpsgCrs crs) {
    if (Objects.isNull(crs)) {
      throw new IllegalArgumentException();
    }

    return useCaches
        ? Optional.ofNullable(crsCache.computeIfAbsent(crs, ignore -> createCrs(crs).orElse(null)))
        : createCrs(crs);
  }

  private CoordinateReferenceSystem getCrsOrThrow(EpsgCrs crs) {
    return getCrs(crs).orElseThrow(() -> new IllegalArgumentException(
        String.format("CRS %s is not supported.",
            Objects.nonNull(crs) ? crs.toSimpleString() : "null")));
  }

  private synchronized Optional<CoordinateReferenceSystem> createCrs(EpsgCrs crs) {
    return createCrs(crs, EPSG.provider(), Proj.getFactory(CRSFactory.class), false);
  }

  private synchronized Optional<CoordinateReferenceSystem> createCrs(EpsgCrs crs,
      CRSAuthorityFactory authorityFactory, CRSFactory crsFactory, boolean logError) {
    try {
      String code = String.valueOf(applyWorkarounds(crs).getCode());
      CoordinateReferenceSystem coordinateReferenceSystem = authorityFactory.createCoordinateReferenceSystem(
          code);
      coordinateReferenceSystem = applyAxisOrder(coordinateReferenceSystem,
          crs.getForceAxisOrder());

      if (crs.getVerticalCode().isPresent()) {
        String verticalCode = String.valueOf(crs.getVerticalCode().getAsInt());
        CoordinateReferenceSystem verticalCrs = authorityFactory.createVerticalCRS(verticalCode);
        CoordinateReferenceSystem compoundCrs = crsFactory.createCompoundCRS(ImmutableMap.of("name",
                String.format("%s + %s", coordinateReferenceSystem.getName(), verticalCrs.getName())),
            coordinateReferenceSystem, verticalCrs);

        return Optional.of(compoundCrs);
      }
      return Optional.of(coordinateReferenceSystem);
    } catch (Throwable e) {
      if (logError) {
        LogContext.error(LOGGER, e, "PROJ");
      }
      return Optional.empty();
    }
  }

  private boolean isCrs3d(CoordinateReferenceSystem crs) {
    return crs.getCoordinateSystem().getDimension() == 3;
  }

  private EpsgCrs applyWorkarounds(EpsgCrs crs) {
    // ArcGIS still uses code 102100 instead of 3857, but proj does not support it anymore
    if (crs.getCode() == 102100) {
      return new ImmutableEpsgCrs.Builder().from(crs).code(3857).build();
    }
    // FME uses code 900914 instead of 4979, but proj does not support this
    if (crs.getCode() == 900914) {
      return new ImmutableEpsgCrs.Builder().from(crs).code(4979).forceAxisOrder(Force.LON_LAT)
          .build();
    }
    return crs;
  }

  private SingleCRS getHorizontalCrs(CoordinateReferenceSystem crs) {

    /* TODO: generic solution for GeographicCRS, this is from GeoTools
    final GeodeticDatum datum = ((GeographicCRS) crs).getDatum();
    Map<String, ?> properties = CRSUtilities.changeDimensionInName(cs, "3D", "2D");
    EllipsoidalCS horizontalCS;
    try {
        horizontalCS =
                ReferencingFactoryFinder.getCSFactory(null)
                        .createEllipsoidalCS(properties, axis0, axis1);
    } catch (FactoryException e) {
        Logging.recoverableException(CRS.class, "getHorizontalCRS", e);
        horizontalCS = new DefaultEllipsoidalCS(properties, axis0, axis1);
    }
    properties = CRSUtilities.changeDimensionInName(crs, "3D", "2D");
    GeographicCRS horizontalCRS;
    try {
        horizontalCRS =
                ReferencingFactoryFinder.getCRSFactory(null)
                        .createGeographicCRS(properties, datum, horizontalCS);
    } catch (FactoryException e) {
        Logging.recoverableException(CRS.class, "getHorizontalCRS", e);
        horizontalCRS = new DefaultGeographicCRS(properties, datum, horizontalCS);
    }
    return horizontalCRS;
     */

    // workaround for CRS84h and EPSG:4979
    if (crs instanceof GeographicCRS && crs.getCoordinateSystem().getDimension() == 3) {
      String name = crs.getName().getCode();
      if (name.startsWith("WGS 84")) {
        if (name.contains("normalized for visualization")) {
          return (SingleCRS) getCrsOrThrow(OgcCrs.CRS84);
        } else {
          return (SingleCRS) getCrsOrThrow(EpsgCrs.of(4326));
        }
      }
    }

    return crs instanceof CompoundCRS
        ? (SingleCRS) ((CompoundCRS) crs).getComponents().get(0)
        : (SingleCRS) crs;
  }

  private CoordinateReferenceSystem applyAxisOrder(CoordinateReferenceSystem crs,
      EpsgCrs.Force axisOrder) {
    if (axisOrder == EpsgCrs.Force.LON_LAT) {
      return Proj.normalizeForVisualization(crs);
    }

    return crs;
  }

  private Map<EpsgCrs, CrsTransformer> getCacheForSource(EpsgCrs crs) {
    return transformerCache.computeIfAbsent(crs, ignore -> new ConcurrentHashMap<>());
  }

  private synchronized CrsTransformer createCrsTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs,
      CoordinateReferenceSystem sourceProjCrs,
      CoordinateReferenceSystem targetProjCrs) {

    boolean is3dTo3d = isCrs3d(sourceProjCrs) && isCrs3d(targetProjCrs);
    int sourceDimension = isCrs3d(sourceProjCrs) ? 3 : 2;
    int targetDimension = is3dTo3d ? 3 : 2;

    try {
      CoordinateOperationContext coordinateOperationContext = new CoordinateOperationContext();
      coordinateOperationContext.setAuthority("EPSG");
      coordinateOperationContext.setSpatialCriterion(SpatialCriterion.PARTIAL_INTERSECTION);
      coordinateOperationContext.setGridAvailabilityUse(GridAvailabilityUse.DISCARD_OPERATION_IF_MISSING_GRID);
      CoordinateOperation coordinateOperation = Proj.createCoordinateOperation(sourceProjCrs,
          targetProjCrs, coordinateOperationContext);

      checkForMissingGridFiles(coordinateOperation);

      CoordinateOperation horizontalCoordinateOperation = null;
      if (sourceDimension == 3) {
        horizontalCoordinateOperation = Proj.createCoordinateOperation(getHorizontalCrs(sourceProjCrs),
            getHorizontalCrs(targetProjCrs), coordinateOperationContext);
      }

      return new CrsTransformerProj(sourceProjCrs, targetProjCrs,
          sourceCrs, targetCrs,
          sourceDimension, targetDimension, coordinateOperation,
          Optional.ofNullable(horizontalCoordinateOperation));
    } catch (IllegalStateException ex) {
      //LogContext.error(LOGGER, ex, "PROJ");
      throw ex;
    } catch (Throwable ex) {
      LogContext.errorAsDebug(LOGGER, ex, "PROJ");
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }

  private void checkForMissingGridFiles(CoordinateOperation coordinateOperation) {
    try {
      double[] coordinates = new double[]{0,0};
      coordinateOperation.getMathTransform().transform(coordinates, 0, coordinates, 0, 1);
    } catch (OutOfMemoryError e) {
      Pattern pattern = Pattern.compile("PARAMETERFILE\\[\"(.*?)\",\"(.*?)\"\\]");
      Matcher matcher = pattern.matcher(coordinateOperation.toWKT());
      if (matcher.find()) {
        throw new IllegalStateException(String.format("Missing PROJ parameter file: %s (%s)", matcher.group(2), matcher.group(1)));
      }
      throw new IllegalStateException(e.getMessage());
    } catch (Throwable e) {
      //ignore
    }
  }
}
