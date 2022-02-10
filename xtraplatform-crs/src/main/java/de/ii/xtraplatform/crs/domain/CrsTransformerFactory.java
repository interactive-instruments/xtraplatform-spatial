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
import javax.measure.Unit;

/**
 *
 * @author zahnen
 */
public interface CrsTransformerFactory {

    boolean isSupported(EpsgCrs crs);

    Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs);
    Optional<CrsTransformer> getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs, boolean force2d);
}
