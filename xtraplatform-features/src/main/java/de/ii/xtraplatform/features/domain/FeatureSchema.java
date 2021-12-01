/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

    @JsonIgnore
    @Override
    List<String> getPath();

    @JsonIgnore
    @Override
    List<String> getParentPath();

    @JsonAlias("path")
    @Override
    Optional<String> getSourcePath();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Value.Default
    default List<String> getSourcePaths() {
        return getSourcePath().map(ImmutableList::of).orElse(ImmutableList.of());
    }

    @Value.Default
    @Override
    default Type getType() {
        return getPropertyMap().isEmpty() ? Type.STRING : Type.OBJECT;
    }

    @Override
    Optional<Role> getRole();

    @Override
    Optional<Type> getValueType();

    @Override
    Optional<SimpleFeatureGeometry> getGeometryType();

    Optional<String> getObjectType();

    Optional<String> getLabel();

    Optional<String> getDescription();

    Optional<String> getConstantValue();

    List<PropertyTransformation> getTransformations();

    Optional<SchemaConstraints> getConstraints();

    @Override
    Optional<Boolean> getForcePolygonCCW();

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

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default List<FeatureSchema> getProperties() {
        return getPropertyMap().values()
                               .stream()
                               .map(featureSchema -> new ImmutableFeatureSchema.Builder().from(featureSchema)
                                                                                         .addPath(featureSchema.getName())
                                                                                         .parentPath(getFullPath())
                                                                                         .build())
                               .collect(Collectors.toList());
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default boolean isFeature() {
        return isObject() && (!getSourcePaths().isEmpty() && getSourcePaths().get(0).startsWith("/"));
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isConstant() {
        return (isValue() && getConstantValue().isPresent()) || (isObject() && getProperties().stream().allMatch(FeatureSchema::isConstant));
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isRequired() {
        return getConstraints().filter(SchemaConstraints::isRequired).isPresent();
    }

    @Value.Check
    default FeatureSchema normalizeConstants() {
        if (!getPropertyMap().isEmpty() && getPropertyMap().values()
                                                           .stream()
                                                           .anyMatch(property -> property.getConstantValue()
                                                                                         .isPresent() && !property.getSourcePath()
                                                                                                                  .isPresent())) {
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
                                                                                                                                                                                      .sourcePath(constantSourcePath)
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
