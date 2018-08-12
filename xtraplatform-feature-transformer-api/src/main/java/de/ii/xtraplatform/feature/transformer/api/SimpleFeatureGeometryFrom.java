package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;

/**
 * @author zahnen
 */
public interface SimpleFeatureGeometryFrom {

    SimpleFeatureGeometry toSimpleFeatureGeometry();

    boolean isValid();
}
