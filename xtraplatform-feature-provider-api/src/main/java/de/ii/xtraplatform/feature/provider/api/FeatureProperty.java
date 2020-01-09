package de.ii.xtraplatform.feature.provider.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilder;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueInstance;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Map;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableFeatureProperty.Builder.class)
public interface FeatureProperty extends ValueInstance {

    //TODO: Role with ID, SPATIAL, TEMPORAL, REFERENCE, REFERENCE_EMBED
    //TODO: more specific types, in addition or instead of Type

    enum Type {
        INTEGER,
        FLOAT,
        STRING,
        BOOLEAN,
        DATETIME,
        GEOMETRY,
        UNKNOWN
    }

    abstract class Builder implements ValueBuilder<FeatureProperty> {
    }

    @Override
    default ImmutableFeatureProperty.Builder toBuilder() {
        return new ImmutableFeatureProperty.Builder().from(this);
    }

    //@Nullable
    String getName();

    String getPath();

    @Value.Default
    default Type getType() {
        return Type.STRING;
    }

    Map<String, String> getAdditionalInfo();

    //TODO
    @JsonIgnore
    @Value.Derived
    default boolean isId() {
        return "id".equalsIgnoreCase(getName());
    }

    @JsonIgnore
    @Value.Derived
    default boolean isSpatial() {
        return getType() == Type.GEOMETRY;
    }

    @JsonIgnore
    @Value.Derived
    default boolean isTemporal() {
        return getType() == Type.DATETIME;
    }

    //TODO
    @JsonIgnore
    @Value.Derived
    default boolean isReference() {
        return false;
    }

    //TODO
    @JsonIgnore
    @Value.Derived
    default boolean isReferenceEmbed() {
        return false;
    }

    //TODO
    @JsonIgnore
    @Value.Derived
    default boolean isForceReversePolygon() {
        return false;
    }
}
