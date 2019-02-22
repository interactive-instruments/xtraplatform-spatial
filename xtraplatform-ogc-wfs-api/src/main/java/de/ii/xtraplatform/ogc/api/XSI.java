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
package de.ii.xtraplatform.ogc.api;

/**
 *
 * @author zahnen
 */
public class XSI extends VersionedVocabulary {

    public enum VERSION {
        DEFAULT;
    }

    public enum VOCABULARY {

        NIL, NIL_TRUE;
    }

    static {
        addWord(VERSION.DEFAULT, NAMESPACE.PREFIX, "xsi");
        addWord(VERSION.DEFAULT, NAMESPACE.URI, "http://www.w3.org/2001/XMLSchema-instance");
        
        
        addWord(VERSION.DEFAULT, VOCABULARY.NIL, "nil");
        addWord(VERSION.DEFAULT, VOCABULARY.NIL_TRUE, "true");
    }
}
