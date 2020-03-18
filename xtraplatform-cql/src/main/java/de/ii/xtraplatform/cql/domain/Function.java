package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = ImmutableFunction.class)
public interface Function extends CqlNode, Scalar, Temporal, Operand {

    String getName();

    @JsonDeserialize(using = OperandDeserializer.class)
    @JsonSerialize(using = OperandSerializer.class)
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
                    Operand temporalLiteral = TemporalLiteral.of(element);
                    operands.add(temporalLiteral);
                } catch (CqlParseException e) {
                    if (element.startsWith("'") && element.endsWith("'")) {
                        operands.add(ScalarLiteral.of(element.substring(1, element.length() - 1)));
                    } else {
                        operands.add(Property.of(element));
                    }
                }
            }

            return operands.build();
        }
    }

    class OperandSerializer extends JsonSerializer<List<Operand>> {

        @Override
        public void serialize(List<Operand> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray();
            for (Operand operand : value) {
                if (operand instanceof ScalarLiteral) {
                    gen.writeString(String.format("'%s'", ((ScalarLiteral) operand).getValue().toString()));
                } else {
                    gen.writeObject(operand);
                }
            }
            gen.writeEndArray();
        }
    }
}



