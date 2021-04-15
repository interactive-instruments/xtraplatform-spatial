package de.ii.xtraplatform.features.domain;

public interface WithConnectionInfo<T extends ConnectionInfo> {

  T getConnectionInfo();

}
