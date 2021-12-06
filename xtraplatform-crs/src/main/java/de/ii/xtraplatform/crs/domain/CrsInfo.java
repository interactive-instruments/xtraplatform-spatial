/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.crs.domain;

import java.util.List;
import java.util.Optional;
import javax.measure.Unit;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;

public interface CrsInfo {

  boolean isSupported(EpsgCrs crs);

  boolean is3d(EpsgCrs crs);

  Unit<?> getUnit(EpsgCrs crs);

  List<String> getAxisAbbreviations(EpsgCrs crs);

  List<Unit<?>> getAxisUnits(EpsgCrs crs);

  List<Optional<RangeMeaning>> getAxisRangeMeanings(EpsgCrs crs);

  List<AxisDirection> getAxisDirections(EpsgCrs crs);

}
