/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A modifiable implementation of the {@link SourcePathMapping SourcePathMapping} type.
 * <p>Use the {@link #create()} static factory methods to create new instances.
 * Use the {@link #toImmutable()} method to convert to canonical immutable instances.
 * <p><em>ModifiableSourcePathMapping is not thread-safe</em>
 * @see ImmutableSourcePathMapping
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Modifiables.generator", "SourcePathMapping"})
@NotThreadSafe
public final class ModifiableSourcePathMapping
    extends SourcePathMapping {
  private final Map<String, TargetMapping> mappings = new LinkedHashMap<String, TargetMapping>();

  private ModifiableSourcePathMapping() {}

  /**
   * Construct a modifiable instance of {@code SourcePathMapping}.
   * @return A new modifiable instance
   */
  public static ModifiableSourcePathMapping create() {
    return new ModifiableSourcePathMapping();
  }

  /**
   * @return value of {@code mappings} attribute
   */
  @JsonAnyGetter
  @JsonMerge
  @Override
  public final Map<String, TargetMapping> getMappings() {
    return mappings;
  }

  /**
   * Clears the object by setting all attributes to their initial values.
   * @return {@code this} for use in a chained invocation
   */
  @CanIgnoreReturnValue
  public ModifiableSourcePathMapping clear() {
    mappings.clear();
    return this;
  }

  /**
   * Fill this modifiable instance with attribute values from the provided {@link SourcePathMapping} instance.
   * Regular attribute values will be overridden, i.e. replaced with ones of an instance.
   * Any of the instance's absent optional values will not be copied (will not override current values).
   * Collection elements and entries will be added, not replaced.
   * @param instance The instance from which to copy values
   * @return {@code this} for use in a chained invocation
   */
  public ModifiableSourcePathMapping from(SourcePathMapping instance) {
    Objects.requireNonNull(instance, "instance");
    putAllMappings(instance.getMappings());
    return this;
  }

  /**
   * Put one entry to the {@link SourcePathMapping#getMappings() mappings} map.
   * @param key The key in mappings map
   * @param value The associated value in the mappings map
   * @return {@code this} for use in a chained invocation
   */
  @JsonAnySetter
  @JsonMerge
  @CanIgnoreReturnValue
  public ModifiableSourcePathMapping putMappings(String key, TargetMapping value) {
    this.mappings.put(
        Objects.requireNonNull(key, "mappings key"),
        Objects.requireNonNull(value, "mappings value"));
    return this;
  }

  /**
   * Sets or replaces all mappings from the specified map as entries for the {@link SourcePathMapping#getMappings() mappings} map.
   * Nulls are not permitted as keys or values.
   * @param entries The entries that will be added to the mappings map
   * @return {@code this} for use in a chained invocation
   */
  @CanIgnoreReturnValue
  public ModifiableSourcePathMapping setMappings(Map<String, ? extends TargetMapping> entries) {
    this.mappings.clear();
    for (Map.Entry<String, ? extends TargetMapping> e : entries.entrySet()) {
      String k = e.getKey();
      TargetMapping v = e.getValue();
      this.mappings.put(
          Objects.requireNonNull(k, "mappings key"),
          Objects.requireNonNull(v, "mappings value"));
    }
    return this;
  }

  /**
   * Put all mappings from the specified map as entries to the {@link SourcePathMapping#getMappings() mappings} map.
   * Nulls are not permitted as keys or values.
   * @param entries to be added to mappings map
   * @return {@code this} for use in a chained invocation
   */
  @CanIgnoreReturnValue
  public ModifiableSourcePathMapping putAllMappings(Map<String, ? extends TargetMapping> entries) {
    for (Map.Entry<String, ? extends TargetMapping> e : entries.entrySet()) {
      String k = e.getKey();
      TargetMapping v = e.getValue();
      this.mappings.put(
          Objects.requireNonNull(k, "mappings key"),
          Objects.requireNonNull(v, "mappings value"));
    }
    return this;
  }


  /**
   * Returns {@code true} if all required attributes are set, indicating that the object is initialized.
   * @return {@code true} if set
   */
  public final boolean isInitialized() {
    return true;
  }

  /**
   * Converts to {@link ImmutableSourcePathMapping ImmutableSourcePathMapping}.
   * @return An immutable instance of SourcePathMapping
   */
  /*public final ImmutableSourcePathMapping toImmutable() {
    return ImmutableSourcePathMapping.copyOf(this);
  }*/

  /**
   * This instance is equal to all instances of {@code ModifiableSourcePathMapping} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    if (!(another instanceof ModifiableSourcePathMapping)) return false;
    ModifiableSourcePathMapping other = (ModifiableSourcePathMapping) another;
    return equalTo(other);
  }

  private boolean equalTo(ModifiableSourcePathMapping another) {
    return mappings.equals(another.mappings);
  }

  /**
   * Computes a hash code from attributes: {@code mappings}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + mappings.hashCode();
    return h;
  }

  /**
   * Generates a string representation of this {@code SourcePathMapping}.
   * If uninitialized, some attribute values may appear as question marks.
   * @return A string representation
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("ModifiableSourcePathMapping")
        .add("mappings", getMappings())
        .toString();
  }
}
