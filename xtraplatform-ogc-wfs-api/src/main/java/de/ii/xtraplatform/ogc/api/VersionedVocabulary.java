package de.ii.xtraplatform.ogc.api;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public class VersionedVocabulary {
    
    public enum NAMESPACE {
        
        PREFIX, URI;
    }
    protected static final Map<Class, Map<Enum, Map<Enum, String>>> vocabulary;
    private static final Class[] children = {WFS.class, FES.class, GML.class};
    
    static {
        vocabulary = new HashMap();

        //TODO: this is a dirty hack to execute the static initializers of the subclasses
        for (Class c : children) {
            try {
                c.newInstance();
            } catch (InstantiationException ex) {
            } catch (IllegalAccessException ex) {
            }
        }
    }
    
    protected static void addWord(Enum v, Enum w, String s) {
        Class c = v.getDeclaringClass().getEnclosingClass();
        if (!vocabulary.containsKey(c)) {
            vocabulary.put(c, new HashMap());
        }
        if (!vocabulary.get(c).containsKey(v)) {
            vocabulary.get(c).put(v, new HashMap());
        }
        for (Map.Entry<Enum, Map<Enum, String>> e : vocabulary.get(c).entrySet()) {
            if (e.getKey().equals(v) || e.getKey().compareTo(v) > 0) {
                vocabulary.get(c).get(e.getKey()).put(w, s);
            }
        }
    }
    
    public static String getWord(Enum v, Enum w) {
        String word = "";
        Class c = v.getDeclaringClass().getEnclosingClass();
        if (vocabulary.containsKey(c) && vocabulary.get(c).containsKey(v) && vocabulary.get(c).get(v).containsKey(w)) {
            word = vocabulary.get(c).get(v).get(w);
        }
        return word;
    }
            
    public static String getQN(Enum v, Enum w) {
        return getWord(v, NAMESPACE.PREFIX) + ":" + getWord(v, w);
    }
    
    public static String getNS(Enum v) {
        return getWord(v, NAMESPACE.URI);
    }
    
    public static String getPR(Enum v) {
        return getWord(v, NAMESPACE.PREFIX);
    }
}
