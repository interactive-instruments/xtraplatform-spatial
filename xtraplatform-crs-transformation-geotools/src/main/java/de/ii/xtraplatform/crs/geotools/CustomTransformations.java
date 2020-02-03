package de.ii.xtraplatform.crs.geotools;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import org.geotools.referencing.factory.epsg.CoordinateOperationFactoryUsingWKT;
import org.geotools.util.URLs;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;

/**
 * Authority allowing users to define their own CoordinateOperations in a separate file. Will
 * override EPSG definitions.
 *
 * @author Oscar Fonts
 */
public class CustomTransformations extends CoordinateOperationFactoryUsingWKT
        implements CoordinateOperationAuthorityFactory {

    public CustomTransformations() {
        super(null, MAXIMUM_PRIORITY);
    }

    public CustomTransformations(Hints userHints) {
        super(userHints, MAXIMUM_PRIORITY);
    }

    /**
     * Returns the URL to the property file that contains Operation definitions from
     * $GEOSERVER_DATA_DIR/user_projections/{@value #FILENAME}
     *
     * @return The URL, or {@code null} if none.
     */
    protected URL getDefinitionsURL() {
        return CustomTransformations.class.getResource("/" + FILENAME);
    }
}