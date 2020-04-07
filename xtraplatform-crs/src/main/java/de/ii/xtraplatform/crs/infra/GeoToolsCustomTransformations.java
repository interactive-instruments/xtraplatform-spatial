package de.ii.xtraplatform.crs.infra;

import org.geotools.factory.Hints;
import org.geotools.referencing.factory.epsg.CoordinateOperationFactoryUsingWKT;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;

import java.net.URL;

/**
 * Authority allowing users to define their own CoordinateOperations in a separate file. Will
 * override EPSG definitions.
 *
 * @author Oscar Fonts
 */
public class GeoToolsCustomTransformations extends CoordinateOperationFactoryUsingWKT
        implements CoordinateOperationAuthorityFactory {

    public GeoToolsCustomTransformations() {
        super(null, MAXIMUM_PRIORITY);
    }

    public GeoToolsCustomTransformations(Hints userHints) {
        super(userHints, MAXIMUM_PRIORITY);
    }

    /**
     * Returns the URL to the property file that contains Operation definitions from
     * $GEOSERVER_DATA_DIR/user_projections/{@value #FILENAME}
     *
     * @return The URL, or {@code null} if none.
     */
    protected URL getDefinitionsURL() {
        return GeoToolsCustomTransformations.class.getResource("/" + FILENAME);
    }
}