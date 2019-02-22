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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.ii.xtraplatform.util.json;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author fischer
 */
public class JSONPOutputStream extends OutputStream {

    private final OutputStream os;
    private boolean callbackWritten;
    private String callback;
    private boolean isCallback;

    public JSONPOutputStream(OutputStream os, String callback) {
        this.os = os;
        this.callback = callback;
        callbackWritten = false;
        isCallback = false;
    }

    private void startCallback() throws IOException {
        if (!callbackWritten) {
            callbackWritten = true;
            isCallback = callback != null && !callback.isEmpty();
            if (isCallback) {
                callback += "(";
                os.write(callback.getBytes());
            }
        }
    }
    
    private void endCallback() throws IOException {
         if (isCallback) {
            os.write(")".getBytes());
        }
    }

    @Override
    public void write(int i) throws IOException {
        startCallback();
        os.write(i);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        startCallback();
        os.write(bytes);
    }

    @Override
    public void write(byte[] bytes, int i, int i1) throws IOException {
        startCallback();
        os.write(bytes, i, i1);
    }

    @Override
    public void flush() throws IOException {
        os.flush();
    }

    @Override
    public void close() throws IOException {
        endCallback();
        os.close();
    }
}
