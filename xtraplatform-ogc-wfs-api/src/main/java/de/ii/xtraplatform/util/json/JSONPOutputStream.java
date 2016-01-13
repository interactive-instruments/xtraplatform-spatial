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
