package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableSchemaConstraints.Builder.class)
public interface SchemaConstraints {

    Optional<String> getCodelist();

    @JsonProperty(value = "enum")
    List<String> getEnumValues();

    Optional<String> getRegex();

    Optional<Boolean> getRequired();

    Optional<Double> getMin();

    Optional<Double> getMax();

    Optional<Integer> getMinOccurrence();

    Optional<Integer> getMaxOccurrence();

}
