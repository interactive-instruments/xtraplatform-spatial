package de.ii.xtraplatform.features.domain;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

@Value.Immutable
public interface SortKey {

  enum Direction {ASCENDING, DESCENDING}

  static SortKey of(String field) {
    return ImmutableSortKey.builder().field(field).build();
  }

  static SortKey of(String field, Direction direction) {
    return ImmutableSortKey.builder().field(field).direction(direction).build();
  }

  String getField();

  @Default
  default Direction getDirection() {
    return Direction.ASCENDING;
  }
}
