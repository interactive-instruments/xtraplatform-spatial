package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ForwardingMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ApplyKeyToValueMap<T> extends ForwardingMap<String, T> implements Map<String, T> {

  private final Map<String, T> delegate;
  private final BiFunction<String, T, T> applyKeyToValue;

  public ApplyKeyToValueMap(Map<String, T> delegate, BiFunction<String, T, T> applyKeyToValue) {
    this.delegate = delegate;
    this.applyKeyToValue = applyKeyToValue;
  }

  @Override
  protected Map<String, T> delegate() {
    return delegate;
  }

  @Override
  public T put(String key, T value) {
    return delegate.put(key, applyKeyToValue.apply(key, value));
  }
}
