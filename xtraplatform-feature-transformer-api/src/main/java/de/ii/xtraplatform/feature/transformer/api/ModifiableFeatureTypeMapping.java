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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A modifiable implementation of the {@link FeatureTypeMapping FeatureTypeMapping} type.
 * <p>Use the {@link #create()} static factory methods to create new instances.
 * Use the {@link #toImmutable()} method to convert to canonical immutable instances.
 * <p><em>ModifiableFeatureTypeMapping is not thread-safe</em>
 * @see ImmutableFeatureTypeMapping
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Modifiables.generator", "FeatureTypeMapping"})
@NotThreadSafe
public final class ModifiableFeatureTypeMapping
    extends FeatureTypeMapping {
  private final Map<String, SourcePathMapping> mappings = new LinkedHashMap<String, SourcePathMapping>();

  private ModifiableFeatureTypeMapping() {}

  /**
   * Construct a modifiable instance of {@code FeatureTypeMapping}.
   * @return A new modifiable instance
   */
  public static ModifiableFeatureTypeMapping create() {
    return new ModifiableFeatureTypeMapping();
  }

  /**
   * @return value of {@code mappings} attribute
   */
  @JsonAnyGetter
  @JsonMerge
  @Override
  public final Map<String, SourcePathMapping> getMappings() {
    return mappings;
  }

  /**
   * @return newly computed, not cached value of {@code mappingsWithPathAsList} attribute
   */
  @JsonIgnore
  @Override
  public final Map<List<String>, SourcePathMapping> getMappingsWithPathAsList() {
    return super.getMappingsWithPathAsList();
  }

  /**
   * Clears the object by setting all attributes to their initial values.
   * @return {@code this} for use in a chained invocation
   */
  @CanIgnoreReturnValue
  public ModifiableFeatureTypeMapping clear() {
    mappings.clear();
    return this;
  }

  /**
   * Fill this modifiable instance with attribute values from the provided {@link FeatureTypeMapping} instance.
   * Regular attribute values will be overridden, i.e. replaced with ones of an instance.
   * Any of the instance's absent optional values will not be copied (will not override current values).
   * Collection elements and entries will be added, not replaced.
   * @param instance The instance from which to copy values
   * @return {@code this} for use in a chained invocation
   */
  public ModifiableFeatureTypeMapping from(FeatureTypeMapping instance) {
    Objects.requireNonNull(instance, "instance");
    putAllMappings(instance.getMappings());
    return this;
  }

  /**
   * Put one entry to the {@link FeatureTypeMapping#getMappings() mappings} map.
   * @param key The key in mappings map
   * @param value The associated value in the mappings map
   * @return {@code this} for use in a chained invocation
   */
  @JsonAnySetter
  @CanIgnoreReturnValue
  public ModifiableFeatureTypeMapping putMappings(String key, SourcePathMapping value) {
    this.mappings.put(
        Objects.requireNonNull(key, "mappings key"),
        Objects.requireNonNull(value, "mappings value"));
    return this;
  }

  /**
   * Sets or replaces all mappings from the specified map as entries for the {@link FeatureTypeMapping#getMappings() mappings} map.
   * Nulls are not permitted as keys or values.
   * @param entries The entries that will be added to the mappings map
   * @return {@code this} for use in a chained invocation
   */
  @CanIgnoreReturnValue
  public ModifiableFeatureTypeMapping setMappings(Map<String, ? extends SourcePathMapping> entries) {
    this.mappings.clear();
    for (Map.Entry<String, ? extends SourcePathMapping> e : entries.entrySet()) {
      String k = e.getKey();
      SourcePathMapping v = e.getValue();
      this.mappings.put(
          Objects.requireNonNull(k, "mappings key"),
          Objects.requireNonNull(v, "mappings value"));
    }
    return this;
  }

  /**
   * Put all mappings from the specified map as entries to the {@link FeatureTypeMapping#getMappings() mappings} map.
   * Nulls are not permitted as keys or values.
   * @param entries to be added to mappings map
   * @return {@code this} for use in a chained invocation
   */
  @CanIgnoreReturnValue
  public ModifiableFeatureTypeMapping putAllMappings(Map<String, ? extends SourcePathMapping> entries) {
    for (Map.Entry<String, ? extends SourcePathMapping> e : entries.entrySet()) {
      String k = e.getKey();
      SourcePathMapping v = e.getValue();
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
   * Converts to {@link ImmutableFeatureTypeMapping ImmutableFeatureTypeMapping}.
   * @return An immutable instance of FeatureTypeMapping
   */
  public final ImmutableFeatureTypeMapping toImmutable() {
    return ImmutableFeatureTypeMapping.copyOf(this);
  }

  /**
   * This instance is equal to all instances of {@code ModifiableFeatureTypeMapping} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    if (!(another instanceof ModifiableFeatureTypeMapping)) return false;
    ModifiableFeatureTypeMapping other = (ModifiableFeatureTypeMapping) another;
    return equalTo(other);
  }

  private boolean equalTo(ModifiableFeatureTypeMapping another) {
    Map<List<String>, SourcePathMapping> mappingsWithPathAsList = getMappingsWithPathAsList();
    return mappings.equals(another.mappings)
        && mappingsWithPathAsList.equals(another.getMappingsWithPathAsList());
  }

  /**
   * Computes a hash code from attributes: {@code mappings}, {@code mappingsWithPathAsList}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + mappings.hashCode();
    Map<List<String>, SourcePathMapping> mappingsWithPathAsList = getMappingsWithPathAsList();
    h += (h << 5) + mappingsWithPathAsList.hashCode();
    return h;
  }

  /**
   * Generates a string representation of this {@code FeatureTypeMapping}.
   * If uninitialized, some attribute values may appear as question marks.
   * @return A string representation
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("ModifiableFeatureTypeMapping")
        .add("mappings", getMappings())
        .toString();
  }
}
