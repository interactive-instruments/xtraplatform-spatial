package de.ii.xtraplatform.features.domain;

public interface SchemaVisitorWithFinalizer<T extends SchemaBase<T>, U, V> extends SchemaVisitorTopDown<T, U> {

  V finalize(T t, U u);

}
