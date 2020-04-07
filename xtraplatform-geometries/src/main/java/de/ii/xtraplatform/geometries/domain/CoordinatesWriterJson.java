/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import org.immutables.value.Value;

import java.io.IOException;

@Value.Immutable
public abstract class CoordinatesWriterJson implements CoordinatesWriter<JsonGenerator> {

    @Override
    public void onStart() throws IOException {
        getDelegate().writeStartArray();
    }

    @Override
    public void onSeparator() throws IOException {
        getDelegate().writeEndArray();
        getDelegate().writeStartArray();
    }

    @Override
    public void onX(char[] chars, int offset, int length) throws IOException {
        getDelegate().writeRawValue(chars, offset, length);
    }

    @Override
    public void onY(char[] chars, int offset, int length) throws IOException {
        getDelegate().writeRawValue(chars, offset, length);
    }

    @Override
    public void onZ(char[] chars, int offset, int length) throws IOException {
        getDelegate().writeRawValue(chars, offset, length);
    }

    @Override
    public void onEnd() throws IOException {
        getDelegate().writeEndArray();
    }
}
