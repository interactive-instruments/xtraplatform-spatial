/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.ogc.wfs.proxy;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
public class WfsProxyCrsTransformations {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(WfsProxyCrsTransformations.class);

    private final CrsTransformation crsTransformation;
    private EpsgCrs wfsDefaultCrs;
    private final EpsgCrs proxyDefaultCrs;
    private CrsTransformer defaultTransformer;
    private boolean reverseOutputAxisOrder;
    private boolean reverseInputAxisOrder;

    public WfsProxyCrsTransformations(CrsTransformation crsTransformation, EpsgCrs wfsDefaultCrs, EpsgCrs proxyDefaultCrs) {
        this.crsTransformation = crsTransformation;
        this.wfsDefaultCrs = wfsDefaultCrs;
        this.proxyDefaultCrs = proxyDefaultCrs;
        initDefaultTransformer();
    }

    private void initDefaultTransformer() {
        // TODO: handle transformation not available
        if (isAvailable() && wfsDefaultCrs != null && !wfsDefaultCrs.equals(proxyDefaultCrs)) {
            LOGGER.getLogger().debug("TRANSFORMER {} {} -> {} {}", wfsDefaultCrs.getCode(), wfsDefaultCrs.isLongitudeFirst() ? "lonlat" : "latlon", proxyDefaultCrs.getCode(), proxyDefaultCrs.isLongitudeFirst() ? "lonlat" : "latlon");
            this.defaultTransformer = crsTransformation.getTransformer(wfsDefaultCrs, proxyDefaultCrs);
        }
    }

    public boolean isAvailable() {
        return crsTransformation != null;
    }

    public CrsTransformer getDefaultTransformer() {
        return defaultTransformer;
    }

    public void setWfsDefaultCrs(EpsgCrs wfsDefaultCrs) {
        this.wfsDefaultCrs = wfsDefaultCrs;
        initDefaultTransformer();
    }
}
