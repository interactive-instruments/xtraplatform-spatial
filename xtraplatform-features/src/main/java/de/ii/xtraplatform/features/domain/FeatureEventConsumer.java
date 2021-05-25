package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public interface FeatureEventConsumer {

  void onStart(OptionalLong numberReturned, OptionalLong numberMatched);

  void onEnd();

  void onFeatureStart();

  void onFeatureEnd();

  void onObjectStart(List<String> path, Optional<SimpleFeatureGeometry> geometryType);

  void onObjectEnd();

  void onArrayStart(List<String> path);

  void onArrayEnd();

  void onValue(List<String> path, String value, SchemaBase.Type valueType);

}
