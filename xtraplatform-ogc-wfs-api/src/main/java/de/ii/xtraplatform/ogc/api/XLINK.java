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
