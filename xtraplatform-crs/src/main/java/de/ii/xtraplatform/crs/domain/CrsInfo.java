package de.ii.xtraplatform.crs.domain;

import java.util.List;
import javax.measure.Unit;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;

public interface CrsInfo {

  //already implemented as isCrs3d
  boolean is3d(EpsgCrs crs);

  //already implemented as getCrsUnit
  Unit<?> getUnit(EpsgCrs crs);

  List<String> getAxisAbbreviations();

  List<Unit<?>> getAxisUnits();

  List<RangeMeaning> getAxisRangeMeanings();

  List<AxisDirection> getAxisDirections();


}
