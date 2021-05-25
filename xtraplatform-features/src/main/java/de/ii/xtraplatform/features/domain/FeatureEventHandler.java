package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

//TODO: more comfortable variant of consumer, use in encoder/transformer
public abstract class FeatureEventHandler implements FeatureEventConsumer{

  interface Context {
    boolean inGeometry();
    boolean inObject();
    boolean inArray();
    List<String> path();
    Optional<SimpleFeatureGeometry> geometryType();
    String value();
    Type valueType();
  }

  @Override
  public final void onStart(OptionalLong numberReturned, OptionalLong numberMatched) {

  }

  @Override
  public final void onEnd() {

  }

  @Override
  public final void onFeatureStart() {

  }

  @Override
  public final void onFeatureEnd() {

  }

  @Override
  public final void onObjectStart(List<String> path, Optional<SimpleFeatureGeometry> geometryType) {

  }

  @Override
  public final void onObjectEnd() {

  }

  @Override
  public final void onArrayStart(List<String> path) {

  }

  @Override
  public final void onArrayEnd() {

  }

  @Override
  public final void onValue(List<String> path, String value, Type valueType) {

  }

  public abstract void onStart(Context context);

  public abstract void onEnd(Context context);

  public abstract void onFeatureStart(Context context);

  public abstract void onFeatureEnd(Context context);

  public abstract void onObjectStart(Context context);

  public abstract void onObjectEnd(Context context);

  public abstract void onArrayStart(Context context);

  public abstract void onArrayEnd(Context context);

  public abstract void onValue(Context context);
}
