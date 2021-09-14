/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
