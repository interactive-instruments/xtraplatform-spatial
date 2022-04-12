/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.Buildable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableBuilder;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @en Definition of object types, see [below](#feature-provider-types).
 * @de Ein Objekt mit der Spezifikation zu jeder Objektart. Sehe [unten](#feature-provider-types).
 * @default `{}`
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@BuildableMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureSchema.Builder.class)
@JsonPropertyOrder({"sourcePath", "type", "role", "valueType", "geometryType", "objectType", "label", "description", "transformations", "constraints", "properties"})
public interface FeatureSchema extends SchemaBase<FeatureSchema>, Buildable<FeatureSchema> {

    Logger LOGGER = LoggerFactory.getLogger(FeatureSchema.class);

    @JsonIgnore
    @Override
    String getName();

    /**
     * @en
     * @de
     * @default
     */
    @JsonIgnore
    @Override
    List<String> getPath();

    /**
     * @en
     * @de
     * @default
     */
    @JsonIgnore
    @Override
    List<String> getParentPath();

    /**
     * @en The relative path for this schema object. The syntax depends on the provider types, see
     * [SQL](sql.md#path-syntax) or [WFS](wfs.md#path-syntax).
     * @de Der relative Pfad zu diesem Schemaobjekt. Die Pfadsyntax ist je nach Provider-Typ unterschiedlich
     * ([SQL](sql.md#path-syntax) und [WFS](wfs.md#path-syntax)).
     * @default
     */
    @JsonAlias("path")
    @Override
    Optional<String> getSourcePath();

    /**
     * @en
     * @de
     * @default
     */
    @JsonMerge(OptBoolean.FALSE)
    @JsonProperty(access = Access.WRITE_ONLY)
    @Override
    List<String> getSourcePaths();

    /**
     * @en Data type of the schema object. Default is `OBJECT` when `properties` is set, otherwise it is `STRING`.
     * Possible values:<ul><li>`FLOAT`, `INTEGER`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE` for simple
     * values.</li><li>`GEOMETRY` for geometries.</li><li>`OBJECT` for objects.</li><li>`OBJECT_ARRAY` a list
     * of objects.</li><li>`VALUE_ARRAY` a list of simple values.</li></ul>
     * @de Der Datentyp des Schemaobjekts. Der Standardwert ist `STRING`, sofern nicht auch die Eigenschaft
     * `properties` angegeben ist, dann ist es `OBJECT`. Erlaubt sind:<ul><li>`FLOAT`, `INTEGER`, `STRING`, `BOOLEAN`,
     * `DATETIME`, `DATE` für einen einfachen Wert des entsprechenden Datentyps.</li><li>`GEOMETRY` für eine
     * Geometrie.</li><li>`OBJECT` für ein Objekt.</li><li>`OBJECT_ARRAY` für eine Liste von Objekten.</li><li>`VALUE_ARRAY`
     * für eine Liste von einfachen Werten.</li></ul>
     * @default `STRING` / `OBJECT`
     */
    @Value.Default
    @Override
    default Type getType() {
        return getPropertyMap().isEmpty() ? Type.STRING : Type.OBJECT;
    }

    @Override
    Optional<Role> getRole();

    /**
     * @en
     * @de
     * @default
     */
    @Override
    Optional<Type> getValueType();

    @Override
    Optional<SimpleFeatureGeometry> getGeometryType();

    /**
     * @en Optional name for an object type, used for example in JSON Schema. For properties that
     * should be mapped as links according to *RFC 8288*, use `Link`.
     * @de Optional kann ein Name für den Typ spezifiziert werden. Der Name hat i.d.R. nur informativen
     * Charakter und wird z.B. bei der Erzeugung von JSON-Schemas verwendet. Bei Eigenschaften,
     * die als Web-Links nach RFC 8288 abgebildet werden sollen, ist immer "Link" anzugeben.
     * @default
     */
    Optional<String> getObjectType();

    /**
     * @en Label for the schema object, used for example in HTML representations.
     * @de Eine Bezeichnung des Schemaobjekts, z.B. für die Angabe in der HTML-Ausgabe.
     * @default
     */
    Optional<String> getLabel();

    /**
     * @en Description for the schema object, used for example in HTML representations or JSON Schema.
     * @de Eine Beschreibung des Schemaobjekts, z.B. für die HTML-Ausgabe oder das JSON-Schema.
     * @default
     */
    Optional<String> getDescription();

    /**
     * @en Might be used instead of `sourcePath` to define a property with a constant value.
     * @de Alternativ zu `sourcePath` kann diese Eigenschaft verwendet werden, um im Feature-Provider eine Eigenschaft
     * mit einem festen Wert zu belegen.
     * @default `null`
     */
    Optional<String> getConstantValue();

    List<PropertyTransformation> getTransformations();

    @Override
    Optional<SchemaConstraints> getConstraints();

    /**
     * @en Option to disable enforcement of counter-clockwise orientation for exterior rings and a clockwise orientation for interior rings (only for SQL).
     * @de Option zum Erzwingen der Orientierung von Polygonen, gegen den Uhrzeigersinn für äußere Ringe und mit dem Uhrzeigersinn für innere Ringe (nur für SQL).
     * @default `true`
     */
    @Override
    Optional<Boolean> getForcePolygonCCW();

    /**
     * @en
     * @de
     * @default
     */
    //behaves exactly like Map<String, FeaturePropertyV2>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonProperty(value = "properties")
    BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getPropertyMap();

    // custom builder to automatically use keys of properties as name
    abstract class Builder implements BuildableBuilder<FeatureSchema> {

        public abstract ImmutableFeatureSchema.Builder putPropertyMap(String key,
                                                                      ImmutableFeatureSchema.Builder builder);

        //@JsonMerge
        @JsonProperty(value = "properties")
        public ImmutableFeatureSchema.Builder putProperties2(Map<String, ImmutableFeatureSchema.Builder> builderMap) {
            ImmutableFeatureSchema.Builder builder1 = null;
            for (Map.Entry<String, ImmutableFeatureSchema.Builder> entry : builderMap.entrySet()) {
                String key = entry.getKey();
                ImmutableFeatureSchema.Builder builder = entry.getValue();
                builder1 = putPropertyMap(key, builder.name(key));
            }
            return builder1;
            //return putPropertyMap(key, builder.name(key));
        }

        //@JsonProperty(value = "properties")
        //@JsonAnySetter
        public ImmutableFeatureSchema.Builder putProperties2(String key, ImmutableFeatureSchema.Builder builder) {
            return putPropertyMap(key, builder.name(key));
        }
    }

    @Override
    default ImmutableFeatureSchema.Builder getBuilder() {
        return new ImmutableFeatureSchema.Builder().from(this);
    }

    Map<String, String> getAdditionalInfo();

    /**
     * @en Only for `OBJECT` and `OBJECT_ARRAY`. Object with the property names as keys and schema objects as values.
     * @de Nur bei `OBJECT` und `OBJECT_ARRAY`. Ein Objekt mit einer Eigenschaft pro Objekteigenschaft.
     * Der Schüssel ist der Name der Objekteigenschaft, der Wert das Schema-Objekt zu der Objekteigenschaft.
     * @default
     */
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default List<FeatureSchema> getProperties() {
        return getPropertyMap().values()
                               .stream()
                               .map(featureSchema -> {
                                   ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder()
                                       .from(featureSchema);

                                   if (getFullPath().size() > featureSchema.getParentPath().size()) {
                                       builder.parentPath(getFullPath());
                                   }

                                   if (featureSchema.getPath().isEmpty()) {
                                       builder.addPath(featureSchema.getName());
                                   }

                                   return builder.build();
                               })
                               .collect(Collectors.toList());
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default boolean isFeature() {
        return isObject() && (!getEffectiveSourcePaths().isEmpty() && getEffectiveSourcePaths().get(0).startsWith("/"));
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isConstant() {
        return (isValue() && getConstantValue().isPresent()) || (isObject() && getProperties().stream().allMatch(FeatureSchema::isConstant));
    }

    @Value.Check
    default FeatureSchema normalizeConstants() {
        if (!getPropertyMap().isEmpty() && getPropertyMap().values()
                                                           .stream()
                                                           .anyMatch(property -> property.getConstantValue()
                                                                                         .isPresent() && property.getSourcePaths()
                                                                                                                  .isEmpty())) {
            final int[] constantCounter = {0};

            Map<String, FeatureSchema> properties = getPropertyMap().entrySet()
                                                                                     .stream()
                                                                                     .map(entry -> {
                                                                                         if (entry.getValue().getConstantValue().isPresent() && !entry.getValue().getSourcePath().isPresent()) {
                                                                                             String constantValue = entry.getValue().getType() == Type.STRING ? String.format("'%s'", entry.getValue()
                                                                                                                                                                                           .getConstantValue()
                                                                                                                                                                                           .get()) : entry.getValue().getConstantValue().get();
                                                                                             String constantSourcePath = String.format(
                                                                                                 "constant_%s_%d{constant=%s}",
                                                                                                 getName(),
                                                                                                 constantCounter[0]++,
                                                                                                 constantValue);
                                                                                             return new AbstractMap.SimpleEntry<>(entry.getKey(), new ImmutableFeatureSchema.Builder().from(entry.getValue())
                                                                                                                                                                                      .addSourcePaths(constantSourcePath)
                                                                                                                                                                                      .build());
                                                                                         }
                                                                                         return entry;
                                                                                     })
                                                                                     .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

            return new ImmutableFeatureSchema.Builder().from(this)
                                                       .propertyMap(properties)
                                                       .build();
        }

        return this;
    }


    @Value.Check
    default FeatureSchema backwardsCompatibility() {
        // migrate double column syntax to multiple sourcePaths, ignore wfs mappings
        if (!getParentPath().isEmpty() && !getParentPath().get(0).contains(":")
            && getSourcePath().filter(path -> path.lastIndexOf(':') > path.lastIndexOf('/')).isPresent()) {
            @Deprecated(since = "3.1.0")
            String path1 = getSourcePath().get().substring(0, getSourcePath().get().lastIndexOf(':'));
            String path2 = path1.substring(0, path1.lastIndexOf('/') + 1) + getSourcePath().get().substring(getSourcePath().get().lastIndexOf(':') + 1);

            LOGGER.info("The sourcePath '{}' in property '{}' uses a deprecated style that includes a colon to merge two columns. Please use multiple sourcePaths instead, one for each column.", getSourcePath().get(), getName());

            return new ImmutableFeatureSchema.Builder().from(this)
                .sourcePath(Optional.empty())
                .sourcePaths(ImmutableList.of(path1, path2))
                .addTransformations(new ImmutablePropertyTransformation.Builder().stringFormat(String.format("{{%s}} ||| {{%s}}", path1, path2)).build())
                .build();
        }

        return this;
    }

    @Value.Check
    default FeatureSchema primaryGeometry() {
        if (isFeature() && getPrimaryGeometry().isPresent()
            && getPrimaryGeometry().filter(FeatureSchema::isPrimaryGeometry).isEmpty()) {
            FeatureSchema primaryGeometry = getPrimaryGeometry().get();
            ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder().from(this).propertyMap(new HashMap<>());

            getPropertyMap().forEach((name, property) -> {
                if (property.isSpatial() && Objects.equals(property.getName(), primaryGeometry.getName())) {
                    builder.putPropertyMap(name, new ImmutableFeatureSchema.Builder().from(property).role(Role.PRIMARY_GEOMETRY).build());
                } else {
                    builder.putPropertyMap(name, property);
                }
            });

            return builder.build();
        }

        return this;
    }

    @Value.Check
    default FeatureSchema primaryInstant() {
        if (isFeature() && getPrimaryInstant().isPresent()
            && getPrimaryInstant().filter(FeatureSchema::isPrimaryInstant).isEmpty()) {
            FeatureSchema primaryInstant = getPrimaryInstant().get();
            ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder().from(this).propertyMap(new HashMap<>());

            getPropertyMap().forEach((name, property) -> {
                if (property.isTemporal() && Objects.equals(property.getName(), primaryInstant.getName())) {
                    builder.putPropertyMap(name, new ImmutableFeatureSchema.Builder().from(property).role(Role.PRIMARY_INSTANT).build());
                } else {
                    builder.putPropertyMap(name, property);
                }
            });

            return builder.build();
        }

        return this;
    }

    @Value.Check
    default FeatureSchema primaryInterval() {
        if (isFeature() && getPrimaryInterval().isPresent()
            && getPrimaryInterval().filter(interval -> interval.first().isPrimaryIntervalStart() && interval.second().isPrimaryIntervalEnd()).isEmpty()) {
            Tuple<FeatureSchema, FeatureSchema> primaryInterval = getPrimaryInterval().get();
            ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder().from(this).propertyMap(new HashMap<>());

            getPropertyMap().forEach((name, property) -> {
                if (property.isTemporal() && Objects.equals(property.getName(), primaryInterval.first().getName())) {
                    builder.putPropertyMap(name, new ImmutableFeatureSchema.Builder().from(property).role(Role.PRIMARY_INTERVAL_START).build());
                } else if (property.isTemporal() && Objects.equals(property.getName(), primaryInterval.second().getName())) {
                    builder.putPropertyMap(name, new ImmutableFeatureSchema.Builder().from(property).role(Role.PRIMARY_INTERVAL_END).build());
                } else {
                    builder.putPropertyMap(name, property);
                }
            });

            return builder.build();
        }

        return this;
    }
}
