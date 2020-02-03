package de.ii.xtraplatform.geometries.domain;

import java.io.IOException;

public interface SeperateStringsProcessor {

    void onStart() throws IOException;

    void onX(char[] chars, int offset, int length) throws IOException;

    void onY(char[] chars, int offset, int length) throws IOException;

    void onZ(char[] chars, int offset, int length) throws IOException;

    void onEnd() throws IOException;

}
