/**
 * Copyright 2020 interactive instruments GmbH
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
public class XLINK extends VersionedVocabulary {

    public enum VERSION {
        DEFAULT;
    }

    public enum VOCABULARY {

        HREF;
    }

    static {
        addWord(VERSION.DEFAULT, NAMESPACE.PREFIX, "xlink");
        addWord(VERSION.DEFAULT, NAMESPACE.URI, "http://www.w3.org/1999/xlink");
        
        
        addWord(VERSION.DEFAULT, VOCABULARY.HREF, "href");
    }
}
