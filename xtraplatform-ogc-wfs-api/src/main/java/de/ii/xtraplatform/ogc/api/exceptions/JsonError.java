/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.api.exceptions;

/**
 *
 * @author fischer
 */
public class JsonError {

    private Error error;

    public JsonError(int code, String msg) {   
        this.error = new Error(code, msg);
    }
    
    public void addDetail(String detail){
        this.error.addDetail(detail);
    }
 
    public Error getError() {
        return this.error;
    }

}
