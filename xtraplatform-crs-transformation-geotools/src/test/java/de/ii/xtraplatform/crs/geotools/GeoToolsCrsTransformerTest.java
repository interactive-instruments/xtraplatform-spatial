package de.ii.xtraplatform.crs.geotools;

import de.ii.xtraplatform.crs.api.CoordinateTuple;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author fischer
 */
public class GeoToolsCrsTransformerTest {
    
    public GeoToolsCrsTransformerTest() {
    }
       
    @BeforeClass(groups = {"default"})
    public void setUp() {
    }

    @Test(groups = {"default"})
    public void testSomeMethod() throws FactoryException  {
        
        CoordinateReferenceSystem scrs = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem tcrs = CRS.decode("EPSG:3857");

        GeoToolsCrsTransformer gct = new GeoToolsCrsTransformer(scrs,tcrs);
        
        double x = 50.7164;
        double y = 7.086;
        
        double rx = 788809.9117611365;
        double ry = 6571280.658009371;
        
        CoordinateTuple t = gct.transform( x, y);
                
        Assert.assertEquals( t.getXasString(), String.valueOf(rx));
        Assert.assertEquals( t.getYasString(), String.valueOf(ry) );
        
        Assert.assertEquals( t.getX(), rx);
        Assert.assertEquals( t.getY(), ry );    
        
        CoordinateTuple tup = new CoordinateTuple(x, y);
        t = gct.transform(tup);
        
        Assert.assertEquals( t.getXasString(), String.valueOf(rx));
        Assert.assertEquals( t.getYasString(), String.valueOf(ry) );
        
        Assert.assertEquals( t.getX(), rx);
        Assert.assertEquals( t.getY(), ry );
        
        double [] ra = new double[10];
        for ( int i = 0; i < 10; i += 2){
            ra[i] = x;
            ra[i+1] = y;
        }
        
        double [] re = gct.transform(ra, 5);
        
        for ( int i = 0; i < 10; i += 2){
            Assert.assertEquals( re[i], rx);
            Assert.assertEquals( re[i+1], ry );    
        }
        
    }
    
}
