/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.google.common.collect.Range;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.services.domain.TaskContext;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.MediaType;

public interface TileWalker {

  interface TileVisitor {
    boolean visit(
        String layer,
        MediaType outputFormat,
        TileMatrixSetBase tileMatrixSet,
        int level,
        int row,
        int col)
        throws IOException;
  }

  interface LimitsVisitor {
    void visit(String layer, TileMatrixSetBase tileMatrixSet, TileMatrixSetLimits limits)
        throws IOException;
  }

  long getNumberOfTiles(
      Set<String> layers,
      List<MediaType> outputFormats,
      Map<String, Map<String, Range<Integer>>> tmsRanges,
      Map<String, Optional<BoundingBox>> boundingBoxes,
      TaskContext taskContext);

  void walkLayersAndTiles(
      Set<String> layers,
      List<MediaType> outputFormats,
      Map<String, Map<String, Range<Integer>>> tmsRanges,
      Map<String, Optional<BoundingBox>> boundingBoxes,
      TaskContext taskContext,
      TileVisitor tileWalker)
      throws IOException;

  void walkLayersAndLimits(
      Set<String> layers,
      Map<String, Map<String, Range<Integer>>> tmsRanges,
      LimitsVisitor limitsVisitor)
      throws IOException;

  void walkLayersAndLimits(
      Set<String> layers,
      Map<String, Map<String, Range<Integer>>> tmsRanges,
      Map<String, Optional<BoundingBox>> boundingBoxes,
      LimitsVisitor limitsVisitor)
      throws IOException;
}
