package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

@Value.Immutable
public interface FeatureChange {

  enum Action {
    CREATE,
    UPDATE,
    DELETE
  }

  Action getAction();

  String getFeatureType();

  List<String> getFeatureIds();

  Optional<BoundingBox> getBoundingBox();

  Optional<Interval> getInterval();

  @Value.Derived
  default Instant getModified() {
    return Instant.now().truncatedTo(ChronoUnit.SECONDS);
  }
}
