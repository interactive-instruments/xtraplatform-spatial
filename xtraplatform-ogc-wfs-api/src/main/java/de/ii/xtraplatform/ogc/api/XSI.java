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
