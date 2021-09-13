/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.kortforsyningen.proj;

import java.util.Objects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ProjExtensions {

  public static CoordinateReferenceSystem normalizeForVisualization(final CoordinateReferenceSystem operation) {
    Objects.requireNonNull(operation);
    if (operation instanceof IdentifiableObject) {
      return (CoordinateReferenceSystem) ((IdentifiableObject) operation).impl.normalizeForVisualization();
    } else {
      throw new UnsupportedImplementationException("operation", operation);
    }
  }

}
