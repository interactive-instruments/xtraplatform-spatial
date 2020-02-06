/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.annotation.JsonTypeResolver;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.xtraplatform.dropwizard.cfg.JacksonProvider;
import org.immutables.value.Value;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "connectorType", visible = true)
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
@JsonTypeResolver(ConnectionInfo.CustomTypeResolver.class)
public interface ConnectionInfo {

    Optional<String> getConnectionUri();

    //@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    default String getConnectorType() {
        return "SLICK";
    }

    //TODO: do we need this for backwards compatibility?
    class CustomTypeResolver extends StdTypeResolverBuilder {
        @Override
        public StdTypeResolverBuilder init(JsonTypeInfo.Id idType, TypeIdResolver idRes) {
            super.init(idType, idRes);
            typeProperty("connectorType");
            return this;
        }

        @Override
        public TypeDeserializer buildTypeDeserializer(final DeserializationConfig config, final JavaType baseType,
                                                      final Collection<NamedType> subtypes) {
            return new CustomTypeDeserializer(baseType, _customIdResolver,
                    _typeProperty, _typeIdVisible, null);
        }
    }

    class CustomTypeDeserializer extends AsPropertyTypeDeserializer {

        public CustomTypeDeserializer(JavaType bt, TypeIdResolver idRes, String typePropertyName, boolean typeIdVisible,
                                      JavaType defaultImpl) {
            super(bt, idRes, typePropertyName, typeIdVisible, defaultImpl);
        }

        public CustomTypeDeserializer(JavaType bt, TypeIdResolver idRes, String typePropertyName, boolean typeIdVisible,
                                      JavaType defaultImpl, JsonTypeInfo.As inclusion) {
            super(bt, idRes, typePropertyName, typeIdVisible, defaultImpl, inclusion);
        }

        public CustomTypeDeserializer(AsPropertyTypeDeserializer src, BeanProperty property) {
            super(src, property);
        }

        @Override
        public TypeDeserializer forProperty(BeanProperty prop) {
            return (prop == _property) ? this : new CustomTypeDeserializer(this, prop);
        }

        @Override
        public Object deserializeTypedFromObject(JsonParser p, DeserializationContext ctxt) throws IOException {
            // 02-Aug-2013, tatu: May need to use native type ids
            if (p.canReadTypeId()) {
                Object typeId = p.getTypeId();
                if (typeId != null) {
                    return _deserializeWithNativeTypeId(p, ctxt, typeId);
                }
            }

            // but first, sanity check to ensure we have START_OBJECT or FIELD_NAME
            JsonToken t = p.getCurrentToken();
            if (t == JsonToken.START_OBJECT) {
                t = p.nextToken();
            } else if (/*t == JsonToken.START_ARRAY ||*/ t != JsonToken.FIELD_NAME) {
                /* This is most likely due to the fact that not all Java types are
                 * serialized as JSON Objects; so if "as-property" inclusion is requested,
                 * serialization of things like Lists must be instead handled as if
                 * "as-wrapper-array" was requested.
                 * But this can also be due to some custom handling: so, if "defaultImpl"
                 * is defined, it will be asked to handle this case.
                 */
                return _deserializeTypedUsingDefaultImpl(p, ctxt, null);
            }
            // Ok, let's try to find the property. But first, need token buffer...
            TokenBuffer tb = null;

            for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
                String name = p.getCurrentName();
                p.nextToken(); // to point to the value
                if (name.equals(_typePropertyName)) { // gotcha!
                    return _deserializeTypedForId(p, ctxt, tb);
                } else if (name.equals("version")) {
                    p = resetParser(p, tb, ctxt);

                    return _deserializeWithNativeTypeId(p, ctxt, "HTTP");
                } else if (name.equals("host")) {
                    p = resetParser(p, tb, ctxt);

                    return _deserializeWithNativeTypeId(p, ctxt, "SLICK");
                }
                if (tb == null) {
                    tb = new TokenBuffer(p, ctxt);
                }
                tb.writeFieldName(name);
                tb.copyCurrentStructure(p);
            }
            return _deserializeTypedUsingDefaultImpl(p, ctxt, tb);
        }

        private JsonParser resetParser(JsonParser p, TokenBuffer tb,
                                       DeserializationContext ctxt) throws IOException {
            if (tb == null) {
                tb = new TokenBuffer(p, ctxt);
            }
            //tb.writeStartObject();
            tb.writeFieldName(p.getCurrentName());
            tb.writeString(p.getText());
            p.clearCurrentToken();
            JsonParser p2 = JsonParserSequence.createFlattened(false, tb.asParser(p), p);
            p2.nextToken();
            return p2;
        }
    }
}
