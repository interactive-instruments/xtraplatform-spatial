/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.api;

/**
 *
 * @author zahnen
 */
public interface CrsTransformation {

    CrsTransformer getTransformer(String sourceCrs, String targetCrs);

    CrsTransformer getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs);

    boolean isCrsAxisOrderEastNorth(String crs);

    boolean isCrsSupported(String crs);
    
}
