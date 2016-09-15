/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xtraplatform.crs.api;

/**
 *
 * @author zahnen
 */
public class EpsgCrs {

    private static final String SIMPLE_PREFIX = "EPSG:";
    private static final String URN_PREFIX = "urn:ogc:def:crs:EPSG::";
    private static final String URI_PREFIX = "http://www.opengis.net/def/crs/epsg/0/";
    private int code;
    private boolean longitudeFirst = false;

    public EpsgCrs() {
        this.code = -1;
        this.longitudeFirst = false;
    }

    public EpsgCrs(int code) {
        this();
        this.code = code;
    }

    public EpsgCrs(int code, boolean longitudeFirst) {
        this(code);
        this.longitudeFirst = longitudeFirst;
    }

    public EpsgCrs(String prefixedCode) {
        this(parsePrefixedCode(prefixedCode));
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isLongitudeFirst() {
        return longitudeFirst;
    }

    public void setLongiduteFirst(boolean longitudeFirst) {
        this.longitudeFirst = longitudeFirst;
    }

    public String getAsSimple() {
        return SIMPLE_PREFIX.concat(Integer.toString(code));
    }

    public String getAsUrn() {
        return URN_PREFIX.concat(Integer.toString(code));
    }

    public String getAsUri() {
        return URI_PREFIX.concat(Integer.toString(code));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final EpsgCrs other = (EpsgCrs) obj;
        if (code != other.getCode() || longitudeFirst != other.isLongitudeFirst())
            return false;
        return true;
    }

    private static int parsePrefixedCode(String prefixedCode) {
        int code = -1;

        try {
            code = Integer.valueOf(prefixedCode.substring(prefixedCode.lastIndexOf(":") + 1));
        } catch (NumberFormatException e) {
            try {
                code = Integer.valueOf(prefixedCode.substring(prefixedCode.lastIndexOf("/") + 1));
            } catch (NumberFormatException e2) {
                // ignore
            }
        }

        return code;
    }
}
