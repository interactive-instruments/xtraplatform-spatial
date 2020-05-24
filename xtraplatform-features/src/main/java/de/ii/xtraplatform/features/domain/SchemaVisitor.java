package de.ii.xtraplatform.features.domain;

import java.util.List;

public interface SchemaVisitor<T extends SchemaBase<T>, U> {

    U visit(T schema, List<U> visitedProperties);

}
