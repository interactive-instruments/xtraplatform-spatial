/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonDeserialize(using = Operand.OperandDeserializer.class)
public interface Operand extends CqlNode {

    class OperandDeserializer extends StdDeserializer<Operand> {

        private static final List<String> ARRAY =
                Arrays.stream(ArrayOperator.values()).map(op -> op.toString().toLowerCase()).collect(Collectors.toUnmodifiableList());
        private static final List<String> SPATIAL =
                Arrays.stream(SpatialOperator.values()).map(op -> op.toString().toLowerCase()).collect(Collectors.toUnmodifiableList());
        private static final List<String> TEMPORAL =
                Arrays.stream(TemporalOperator.values()).map(op -> op.toString().toLowerCase()).collect(Collectors.toUnmodifiableList());
        private static final List<String> SCALAR = ImmutableList.of("value", "list", "operands", "eq", "neq", "gt", "gte", "lt", "lte", "between", "in", "isnull");

        protected OperandDeserializer() {
            this(null);
        }

        protected OperandDeserializer(Class<?> vc) {
            super(vc);
        }

        private Scalar getScalar(JsonNode node) {
            if (node.isBoolean())
                return ScalarLiteral.of(node.asBoolean());
            if (node.isInt())
                return ScalarLiteral.of(node.asInt());
            if (node.isLong())
                return ScalarLiteral.of(node.asLong());
            if (node.isDouble())
                return ScalarLiteral.of(node.asDouble());

            return ScalarLiteral.of(node.asText());
        }

        private Operand getOperand(JsonParser parser, JsonNode node, String parent) throws JsonProcessingException {
            ObjectCodec oc = parser.getCodec();
            // TODO support ArrayLiteral
            if (node.isObject()) {
                if (Objects.nonNull(node.get("property"))) {
                    return oc.treeToValue(node, Property.class);
                } else if (Objects.nonNull(node.get("function"))) {
                    List<Operand> list = new ArrayList<>();
                    Iterator<JsonNode> iterator = node.get("function").get("arguments").elements();
                    while (iterator.hasNext()) {
                        JsonNode listNode = iterator.next();
                        list.add(getOperand(parser, listNode, "arguments"));
                    }
                    return Function.of(node.get("function").get("name").textValue(), list);
                } else if (SPATIAL.contains(parent)) {
                    return SpatialLiteral.of(oc.treeToValue(node, Geometry.class));
                }
            } else if (node.isArray()) {
                if (TEMPORAL.contains(parent)) {
                    return oc.treeToValue(node, TemporalLiteral.class);
                }
            } else if (node.isValueNode()) {
                if (SCALAR.contains(parent)) {
                    return getScalar(node);
                } else if (TEMPORAL.contains(parent)) {
                    return oc.treeToValue(node, TemporalLiteral.class);
                } else {
                    // we have to guess, try temporal first
                    try {
                        return oc.treeToValue(node, TemporalLiteral.class);
                    } catch (JsonProcessingException e) {
                        return getScalar(node);
                    }
                }
            }

            throw new JsonParseException(parser, String.format("Unexpected operand of type %s in member %s.", node.getNodeType(), parent));
        }

        @Override
        public Operand deserialize(JsonParser parser, DeserializationContext ctxt)
                throws IOException, JsonMappingException {

            // Parse "object" node into Jackson's tree model
            JsonNode node = parser.getCodec().readTree(parser);

            // Get name of the parent key
            String parent = parser.getParsingContext()
                                  .getParent()
                                  .getCurrentName()
                                  .toLowerCase();

            return getOperand(parser, node, parent);
        }

    }
}
