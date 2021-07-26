package de.ii.xtraplatform.features.app;

import de.ii.xtraplatform.features.domain.FeatureEventHandler;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.geometries.domain.CoordinatesWriter;
import java.io.IOException;
import org.immutables.value.Value;

@Value.Immutable
public abstract class CoordinatesWriterFeatureTokens implements CoordinatesWriter<FeatureEventHandler<ModifiableContext>> {

  @Value.Parameter
  public abstract ModifiableContext getContext();

  @Override
  public void onStart() throws IOException {
    getDelegate().onArrayStart(getContext());
    getDelegate().onArrayStart(getContext());
  }

  @Override
  public void onSeparator() throws IOException {
    getDelegate().onArrayEnd(getContext());
    getDelegate().onArrayStart(getContext());
  }

  @Override
  public void onX(char[] chars, int offset, int length) throws IOException {
    getContext().setValue(String.valueOf(chars, offset, length));
    getDelegate().onValue(getContext());
  }

  @Override
  public void onY(char[] chars, int offset, int length) throws IOException {
    getContext().setValue(String.valueOf(chars, offset, length));
    getDelegate().onValue(getContext());
  }

  @Override
  public void onZ(char[] chars, int offset, int length) throws IOException {
    getContext().setValue(String.valueOf(chars, offset, length));
    getDelegate().onValue(getContext());
  }

  @Override
  public void onEnd() throws IOException {
    getDelegate().onArrayEnd(getContext());
    getDelegate().onArrayEnd(getContext());
  }
}
