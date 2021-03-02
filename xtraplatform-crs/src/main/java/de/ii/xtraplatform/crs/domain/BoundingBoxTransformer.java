/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.domain;

/**
 *
 * @author fischer
 */
public abstract class BoundingBoxTransformer implements CrsTransformer{
        
    @Override
    public BoundingBox transformBoundingBox(BoundingBox boundingBox) throws CrsTransformationException {
                
        // DEBUG
        //System.out.println( bbox.getXmin()+" " +bbox.getYmin()+" "+bbox.getXmax()+" "+bbox.getYmax()+" 0;");
        CoordinateTuple ll = this.transform(boundingBox.getXmin(), boundingBox.getYmin());
        CoordinateTuple lr = this.transform(boundingBox.getXmax(), boundingBox.getYmin());
        CoordinateTuple ur = this.transform(boundingBox.getXmax(), boundingBox.getYmax());
        CoordinateTuple ul = this.transform(boundingBox.getXmin(), boundingBox.getYmax());

        if (ll.isNull() || ul.isNull() || lr.isNull() || ur.isNull()) {
            throw new CrsTransformationException("Failed to transform bounding box corner point coordinate(s): " + boundingBox.toString());
        }

        double xmin,ymin,xmax,ymax;
              
        // Die BoundingBox ins boxIn (lx,ly,hx,hy) im System crsIn wird in das System 
        // crsOut transformiert. Dabei wird sichergestellt, dass es sich wieder um eine
        // BoundingBox handelt. D.h. in Wirklichkeit werden alle vier Eckpunkte der
        // Input-BoundingBox transformiert und davon wird die Output-BoundingBox durch
        // Minmaxing bestimmt.
        
        if (ul.getX() < ll.getX()) {
            xmin = ul.getX();
        } else {
            xmin = ll.getX();
        }
        if (lr.getY() < ll.getY()) {
            ymin = lr.getY();
        } else {
            ymin = ll.getY();
        }
        if (ur.getX() > lr.getX()) {
            xmax = ur.getX();
        } else {
            xmax = lr.getX();
        }
        if (ul.getY() > ur.getY()) {
            ymax = ul.getY();
        } else {
            ymax = ur.getY();
        }

        return BoundingBox.of(xmin, ymin, xmax, ymax, getTargetCrs());
    }
}
