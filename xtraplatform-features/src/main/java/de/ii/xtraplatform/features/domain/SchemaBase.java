/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;

public interface SchemaBase<T extends SchemaBase<T>> {

  enum Role {
    ID,
    TYPE,
    PRIMARY_GEOMETRY,
    PRIMARY_INSTANT,
    PRIMARY_INTERVAL_START,
    PRIMARY_INTERVAL_END,
    SECONDARY_GEOMETRY
  }

  enum Type {
    INTEGER,
    FLOAT,
    STRING,
    BOOLEAN,
    DATETIME,
    DATE,
    GEOMETRY,
    VALUE,
    OBJECT,
    VALUE_ARRAY,
    OBJECT_ARRAY,
    FEATURE_REF,
    FEATURE_REF_ARRAY,
    UNKNOWN
  }

  /**
   * @langEn # Schema Scopes
   *     <p>Schemas are used with different scopes. Properties may be applicable only for a subset
   *     of the scopes depending on the characteristics of the property or the API design. The four
   *     scopes are discussed below.
   *     <p>## `RETURNABLE`
   *     <p>Returnable properties are the properties that are included in feature representations
   *     when features are fetched. By default, all properties are returnable unless the property is
   *     explicitly excluded. Eligible properties may be explicitly excluded, for example, if the
   *     property should be used only in queries (as a queryable or sortable), but never coded in
   *     the features themselves.
   *     <p>## `RECEIVABLE`
   *     <p>Receivable properties are the properties that may be included in feature representations
   *     when features are created or updated. By default, all properties are receivable unless the
   *     property is constant or explicitly excluded. Eligible properties may be explicitly
   *     excluded, for example, if the property is derived or uses a different representation in the
   *     data store than in the response.
   *     <p>## `QUERYABLE`
   *     <p>Queryable properties are the properties that may be used in filter expressions. By
   *     default, all properties may be queryable unless the property is explicitly excluded, uses
   *     `concat` / `coalesce`, or is of type `OBJECT` / `OBJECT_ARRAY`. Eligible properties may be
   *     explicitly excluded, for example, if the property is not optimized for use in queries.
   *     <p>## `SORTABLE`
   *     <p>Sortable properties are the properties that may be used to sort features in responses.
   *     By default, all direct properties of a feature type that are of type STRING, FLOAT,
   *     INTEGER, DATE, or DATETIME may be sortable unless the property is explicitly excluded, or
   *     uses `concat` / `coalesce`. Eligible properties may be explicitly excluded, for example, if
   *     the property is not optimized for use in queries.
   * @langDe # Schema-Anwendungsbereiche
   *     <p>Schemas werden mit unterschiedlichen Anwendungsbereichen verwendet. Objekteigenschaften
   *     können je nach den Merkmalen der Eigenschaft oder dem API-Design nur für eine Teilmenge der
   *     Bereiche anwendbar sein. Die vier Anwendungsbereiche werden im Folgenden erläutert.
   *     <p>## `RETURNABLE`
   *     <p>Rückgabefähige Eigenschaften sind die Eigenschaften, die in Feature-Darstellungen
   *     enthalten sind, wenn Features abgerufen werden. Standardmäßig sind alle Eigenschaften
   *     rückgabefähig, es sei denn, die Eigenschaft wird explizit ausgeschlossen. In Frage kommende
   *     Eigenschaften können explizit ausgeschlossen werden, z. B. wenn die Eigenschaft nur in
   *     Abfragen verwendet werden soll (als Queryable oder Sortable), aber niemals in den Features
   *     selbst kodiert werden soll.
   *     <p>## `RECEIVABLE`
   *     <p>Empfangbare Eigenschaften sind die Eigenschaften, die in Feature-Darstellungen enthalten
   *     sein können, wenn Features erzeugt oder aktualisiert werden. Standardmäßig sind alle
   *     Eigenschaften empfangbar, es sei denn, die Eigenschaft ist konstant oder explizit
   *     ausgeschlossen. In Frage kommende Eigenschaften können explizit ausgeschlossen werden, z.B.
   *     wenn die Eigenschaft abgeleitet ist oder eine andere Darstellung im Datenspeicher als in
   *     der Antwort verwendet.
   *     <p>## `QUERYABLE`
   *     <p>Abfragbare Eigenschaften sind die Eigenschaften, die in Filterausdrücken verwendet
   *     werden können. Standardmäßig können alle Eigenschaften abgefragt werden, es sei denn, die
   *     Eigenschaft ist explizit ausgeschlossen, verwendet `concat` / `coalesce` oder ist vom Typ
   *     `OBJECT` / `OBJECT_ARRAY`. In Frage kommende Eigenschaften können explizit ausgeschlossen
   *     werden, zum Beispiel, wenn die Eigenschaft nicht für die Verwendung in Abfragen optimiert
   *     ist.
   *     <p>## `SORTABLE`
   *     <p>Sortierbare Eigenschaften sind die Eigenschaften, die zum Sortieren von Features in
   *     Antworten verwendet werden können. Standardmäßig können alle direkten Eigenschaften einer
   *     Objektart, die vom Typ `STRING`, `FLOAT`, `INTEGER`, `DATE` oder `DATETIME` sind,
   *     sortierbar sein, es sei denn, die Eigenschaft wird explizit ausgeschlossen oder verwendet
   *     `concat` / `coalesce`. In Frage kommende Eigenschaften können explizit ausgeschlossen
   *     werden, zum Beispiel wenn die Eigenschaft nicht für die Verwendung in Abfragen optimiert
   *     ist.
   */
  @DocFile(path = "providers/details", name = "scopes.md")
  enum Scope {
    @Deprecated(since = "3.6")
    QUERIES,
    RETURNABLE,
    @Deprecated(since = "3.6")
    MUTATIONS,
    RECEIVABLE,
    QUERYABLE,
    SORTABLE;

    public static List<Scope> allBut(Scope... scopes) {
      return Arrays.stream(Scope.values())
          .filter(s -> Arrays.stream(scopes).noneMatch(scope -> scope == s))
          .collect(Collectors.toList());
    }
  }

  String getName();

  Type getType();

  Optional<Role> getRole();

  Optional<Type> getValueType();

  Optional<SimpleFeatureGeometry> getGeometryType();

  Optional<String> getRefType();

  Optional<String> getRefUriTemplate();

  Optional<String> getRefKeyTemplate();

  List<String> getPath();

  List<String> getParentPath();

  Optional<String> getSourcePath();

  List<String> getSourcePaths();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default List<String> getEffectiveSourcePaths() {
    return getSourcePath()
        .map(element -> (List<String>) ImmutableList.of(element))
        .orElse(getSourcePaths());
  }

  Optional<SchemaConstraints> getConstraints();

  Optional<Boolean> getForcePolygonCCW();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isForcePolygonCCW() {
    return getForcePolygonCCW().filter(force -> force == false).isEmpty();
  }

  @Deprecated(since = "3.6")
  Optional<Scope> getScope();

  Set<Scope> getExcludedScopes();

  @Deprecated(since = "3.6")
  Optional<Boolean> getIsQueryable();

  @Deprecated(since = "3.6")
  Optional<Boolean> getIsSortable();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean queryable() {
    return !isObject()
        && !Objects.equals(getType(), Type.UNKNOWN)
        && !getExcludedScopes().contains(Scope.QUERYABLE);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean sortable() {
    return !isSpatial()
        && !isObject()
        && !isArray()
        && !Objects.equals(getType(), Type.BOOLEAN)
        && !Objects.equals(getType(), Type.UNKNOWN)
        && !getExcludedScopes().contains(Scope.SORTABLE);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean returnable() {
    return !getExcludedScopes().contains(Scope.RETURNABLE);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean receivable() {
    return !getExcludedScopes().contains(Scope.RECEIVABLE);
  }

  default boolean hasOneOf(Set<Scope> scopes) {
    return scopes.stream()
        .anyMatch(
            s -> {
              switch (s) {
                case RETURNABLE:
                  return returnable();
                case RECEIVABLE:
                  return receivable();
                case QUERYABLE:
                  return queryable();
                case SORTABLE:
                  return sortable();
              }
              return false;
            });
  }

  Optional<Boolean> getIsLastModified();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean lastModified() {
    return Objects.equals(getType(), Type.DATETIME) && getIsLastModified().orElse(false);
  }

  List<T> getProperties();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default List<T> getAllNestedProperties() {
    return getProperties().stream()
        .flatMap(t -> Stream.concat(Stream.of(t), t.getAllNestedProperties().stream()))
        .collect(Collectors.toList());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default List<T> getAllObjects() {
    return Stream.concat(
            Stream.of((T) this),
            getAllNestedProperties().stream()
                .filter(SchemaBase::isObject)
                .filter(obj -> obj.getProperties().stream().anyMatch(SchemaBase::isValue)))
        .collect(Collectors.toList());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getIdProperty() {
    return getAllNestedProperties().stream().filter(SchemaBase::isId).findFirst();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getIdParent() {
    return getAllObjects().stream()
        .filter(schema -> schema.getProperties().stream().anyMatch(SchemaBase::isId))
        .findFirst();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getPrimaryGeometry() {
    return getAllNestedProperties().stream()
        .filter(SchemaBase::isPrimaryGeometry)
        .findFirst()
        .or(() -> getProperties().stream().filter(SchemaBase::isSpatial).findFirst());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getPrimaryGeometryParent() {
    return getAllObjects().stream()
        .filter(schema -> schema.getProperties().stream().anyMatch(SchemaBase::isPrimaryGeometry))
        .findFirst();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getPrimaryInstant() {
    return getAllNestedProperties().stream()
        .filter(SchemaBase::isPrimaryInstant)
        .findFirst()
        .or(
            () ->
                getPrimaryInterval().isEmpty()
                    ? getProperties().stream().filter(SchemaBase::isTemporal).findFirst()
                    : Optional.empty());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getPrimaryInstantParent() {
    return getAllObjects().stream()
        .filter(schema -> schema.getProperties().stream().anyMatch(SchemaBase::isPrimaryInstant))
        .findFirst();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<Tuple<T, T>> getPrimaryInterval() {
    Optional<T> start =
        getAllNestedProperties().stream().filter(SchemaBase::isPrimaryIntervalStart).findFirst();
    Optional<T> end =
        getAllNestedProperties().stream().filter(SchemaBase::isPrimaryIntervalEnd).findFirst();

    return start.isPresent() && end.isPresent()
        ? Optional.of(Tuple.of(start.get(), end.get()))
        : Optional.empty();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getPrimaryIntervalStartParent() {
    return getAllObjects().stream()
        .filter(
            schema -> schema.getProperties().stream().anyMatch(SchemaBase::isPrimaryIntervalStart))
        .findFirst();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getPrimaryIntervalEndParent() {
    return getAllObjects().stream()
        .filter(
            schema -> schema.getProperties().stream().anyMatch(SchemaBase::isPrimaryIntervalEnd))
        .findFirst();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getSecondaryGeometry() {
    return getAllNestedProperties().stream().filter(SchemaBase::isSecondaryGeometry).findFirst();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Optional<T> getSecondaryGeometryParent() {
    return getAllObjects().stream()
        .filter(schema -> schema.getProperties().stream().anyMatch(SchemaBase::isSecondaryGeometry))
        .findFirst();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default List<String> getFullPath() {
    return new ImmutableList.Builder<String>().addAll(getParentPath()).addAll(getPath()).build();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default String getFullPathAsString() {
    return getFullPathAsString(".");
  }

  default String getFullPathAsString(String delimiter) {
    return String.join(delimiter, getFullPath());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Set<String> getValueNames() {
    return getProperties().stream()
        .filter(SchemaBase::isValue)
        .map(SchemaBase::getName)
        .collect(ImmutableSet.toImmutableSet());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isObject() {
    return getType() == Type.OBJECT || getType() == Type.OBJECT_ARRAY;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isArray() {
    return getType() == Type.OBJECT_ARRAY
        || getType() == Type.VALUE_ARRAY
        || getType() == Type.FEATURE_REF_ARRAY;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isValue() {
    return !isObject() /*&& getType() != Type.VALUE_ARRAY*/;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isFeature() {
    return isObject() && getParentPath().isEmpty();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isSpatial() {
    return getType() == Type.GEOMETRY;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isTemporal() {
    return getType() == Type.DATETIME || getType() == Type.DATE;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isFeatureRef() {
    return getType() == Type.FEATURE_REF
        || getType() == Type.FEATURE_REF_ARRAY
        || (isObject()
            && (getRefType().isPresent()
                || getRefUriTemplate().isPresent()
                || getRefKeyTemplate().isPresent()));
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isId() {
    return getRole().filter(role -> role == Role.ID).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isPrimaryGeometry() {
    return getRole().filter(role -> role == Role.PRIMARY_GEOMETRY).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isPrimaryInstant() {
    return getRole().filter(role -> role == Role.PRIMARY_INSTANT).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isPrimaryIntervalStart() {
    return getRole().filter(role -> role == Role.PRIMARY_INTERVAL_START).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isPrimaryIntervalEnd() {
    return getRole().filter(role -> role == Role.PRIMARY_INTERVAL_END).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isSecondaryGeometry() {
    return getRole().filter(role -> role == Role.SECONDARY_GEOMETRY).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isType() {
    return getRole().filter(role -> role == Role.TYPE).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isRequired() {
    return getConstraints().filter(SchemaConstraints::isRequired).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isSimpleFeatureGeometry() {
    return getGeometryType().isPresent() && !is3dGeometry();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean is3dGeometry() {
    return getGeometryType().isPresent()
        && getGeometryType().get() == SimpleFeatureGeometry.MULTI_POLYGON
        && getConstraints().isPresent()
        && getConstraints().get().isClosed()
        && getConstraints().get().isComposite();
  }

  default <U> U accept(SchemaVisitor<T, U> visitor) {
    return visitor.visit(
        (T) this,
        getProperties().stream()
            .map(property -> property.accept(visitor))
            .collect(Collectors.toList()));
  }

  // TODO: replace SchemaVisitor with SchemaVisitorTopDown
  default <U> U accept(SchemaVisitorTopDown<T, U> visitor) {
    return accept(visitor, ImmutableList.of());
  }

  default <U, V> V accept(SchemaVisitorWithFinalizer<T, U, V> visitor) {
    return visitor.finalize((T) this, accept(visitor, ImmutableList.of()));
  }

  default <U> U accept(SchemaVisitorTopDown<T, U> visitor, List<T> parents) {
    return visitor.visit(
        (T) this,
        parents,
        getProperties().stream()
            .map(
                property ->
                    property.accept(
                        visitor,
                        new ImmutableList.Builder<T>().addAll(parents).add((T) this).build()))
            .collect(Collectors.toList()));
  }
}
