/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.api.exceptions;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fischer
 */
public class Error {
    private int code;
    private String message;
    private List<String> details;

    public Error( int code, String message){ 
        this.code = code;
        this.message = message;
        this.details = new ArrayList();
    }
    
    public void addDetail(String detail){
        this.details.add(detail);
    }
    
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
    
    
}
