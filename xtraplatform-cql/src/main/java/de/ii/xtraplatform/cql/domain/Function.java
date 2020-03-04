package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = ImmutableFunction.class)
public interface Function extends CqlNode, Scalar, Operand {

    String getName();

    @JsonDeserialize(using = OperandDeserializer.class)
    List<Operand> getArguments();

    static Function of(String name, List<Operand> arguments) {
        return new ImmutableFunction.Builder()
                .name(name)
                .arguments(arguments)
                .build();
    }

    @Override
    default <U> U accept(CqlVisitor<U> visitor) {

        List<U> arguments = getArguments()
                .stream()
                .map(argument -> argument.accept(visitor))
                .collect(Collectors.toList());

        return visitor.visit(this, arguments);
    }

    class OperandDeserializer extends JsonDeserializer<List<Operand>> {

        @Override
        public List<Operand> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            ImmutableList.Builder<Operand> operands = new ImmutableList.Builder<>();
            Iterator<JsonNode> nodes = ((JsonNode) p.readValueAsTree()).elements();
            while (nodes.hasNext()) {
                String element = nodes.next().asText();
                try {
                    TemporalLiteral temporalLiteral = TemporalLiteral.of(element);
                    operands.add(temporalLiteral);
                } catch (CqlParseException e) {
                    ScalarLiteral scalarLiteral = ScalarLiteral.of(element);
                    operands.add(scalarLiteral);
                }
            }

            return operands.build();
        }
    }
}



