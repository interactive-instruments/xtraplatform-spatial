/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.crs.domain;

import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface CrsTransformerFactory extends Volatile2 {

  boolean isSupported(EpsgCrs crs);

  Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs);

  Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs, boolean force2d);
}
