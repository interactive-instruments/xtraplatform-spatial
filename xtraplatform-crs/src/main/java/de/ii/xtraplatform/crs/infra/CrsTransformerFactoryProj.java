/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
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

  private final Map<EpsgCrs, CoordinateReferenceSystem> crsCache;
  private final Map<EpsgCrs, Map<EpsgCrs, CrsTransformer>> transformerCache;
  private final boolean useCaches = false;

  public CrsTransformerFactoryProj(@Requires ProjLoader projLoader) {
    this.crsCache = new ConcurrentHashMap<>();
    this.transformerCache = new ConcurrentHashMap<>();

    try {
      projLoader.load();
      Proj.setSearchPath(projLoader.getDataDirectory().toString());
      Proj.version().ifPresent(version -> LOGGER.debug("PROJ version: {}", version));
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "PROJ");
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
      CoordinateOperation coordinateOperation = Proj.createCoordinateOperation(sourceProjCrs,
          targetProjCrs, coordinateOperationContext);

      return new CrsTransformerProj(sourceProjCrs, targetProjCrs, sourceCrs, targetCrs,
          sourceDimension, targetDimension, coordinateOperation);
    } catch (FactoryException ex) {
      LogContext.errorAsDebug(LOGGER, ex, "PROJ");
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }
}
