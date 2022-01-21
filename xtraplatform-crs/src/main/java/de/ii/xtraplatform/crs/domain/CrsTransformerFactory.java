/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.domain;

import java.util.Optional;
import javax.measure.unit.Unit;

/**
 *
 * @author zahnen
 */
public interface CrsTransformerFactory {

    Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs);

    boolean isCrsSupported(EpsgCrs crs);

    boolean isCrs3d(EpsgCrs crs);

    Unit<?> getCrsUnit(EpsgCrs crs);

}
