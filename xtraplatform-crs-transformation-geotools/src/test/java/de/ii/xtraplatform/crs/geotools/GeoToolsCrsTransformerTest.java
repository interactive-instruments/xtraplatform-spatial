/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.geotools;

import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.EpsgCrs;
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

        GeoToolsCrsTransformer gct = new GeoToolsCrsTransformer(scrs,tcrs, sourceCrs, new EpsgCrs(3857));
        
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
        t = gct.transform(tup, false);
        
        Assert.assertEquals( t.getXasString(), String.valueOf(rx));
        Assert.assertEquals( t.getYasString(), String.valueOf(ry) );
        
        Assert.assertEquals( t.getX(), rx);
        Assert.assertEquals( t.getY(), ry );
        
        double [] ra = new double[10];
        for ( int i = 0; i < 10; i += 2){
            ra[i] = x;
            ra[i+1] = y;
        }
        
        double [] re = gct.transform(ra, 5, false);
        
        for ( int i = 0; i < 10; i += 2){
            Assert.assertEquals( re[i], rx);
            Assert.assertEquals( re[i+1], ry );    
        }
        
    }
    
}
