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
