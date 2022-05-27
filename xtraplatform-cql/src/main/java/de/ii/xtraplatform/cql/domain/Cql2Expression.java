package de.ii.xtraplatform.cql.domain;

public interface Cql2Expression extends Operand, CqlNode {

  default <T> T accept(CqlVisitor<T> visitor, boolean isRoot) {
    T visited = accept(visitor);
    return isRoot
        ? visitor.postProcess(this, visited)
        : visited;
  }

}
