package com.qrrestaurant.auth.infrastructure.oauth;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sérialisation JSON (encodée base64url) d'une {@link OAuth2AuthorizationRequest}
 * pour stockage en cookie court — l'API étant stateless, la requête d'autorisation
 * ne peut pas vivre dans une session HTTP.
 *
 * <p>Sérialisation explicite champ par champ : les getters de l'interface en
 * dérivent certains ({@code getAuthorizationRequestUri()}), et la reconstruction
 * passe par le builder officiel, qui recalcule les valeurs dérivées.</p>
 */
final class OAuth2AuthorizationRequestCookieCodec {

    private final ObjectMapper mapper;

    OAuth2AuthorizationRequestCookieCodec() {
        this.mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(OAuth2AuthorizationRequest.class, new Serializer());
        module.addDeserializer(OAuth2AuthorizationRequest.class, new Deserializer());
        this.mapper.registerModule(module);
    }

    String encode(OAuth2AuthorizationRequest request) {
        try {
            String json = mapper.writeValueAsString(request);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Sérialisation de la requête d'autorisation impossible", e);
        }
    }

    OAuth2AuthorizationRequest decode(String cookieValue) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(cookieValue);
            return mapper.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (IOException | IllegalArgumentException e) {
            return null; // cookie corrompu ou expiré : traité comme absent, la danse OAuth repart de zéro.
        }
    }

    private static class Serializer extends JsonSerializer<OAuth2AuthorizationRequest> {
        @Override
        public void serialize(OAuth2AuthorizationRequest request, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField(OAuth2ParameterNames.CLIENT_ID, request.getClientId());
            gen.writeStringField(OAuth2ParameterNames.STATE, request.getState());
            gen.writeStringField(OAuth2ParameterNames.REDIRECT_URI, request.getRedirectUri());
            gen.writeStringField("authorization_uri", request.getAuthorizationUri());
            gen.writeArrayFieldStart(OAuth2ParameterNames.SCOPE);
            for (String scope : request.getScopes()) {
                gen.writeString(scope);
            }
            gen.writeEndArray();
            gen.writeObjectField("additional_parameters", request.getAdditionalParameters());
            gen.writeObjectField("attributes", request.getAttributes());
            gen.writeEndObject();
        }
    }

    private static class Deserializer extends StdDeserializer<OAuth2AuthorizationRequest> {
        Deserializer() {
            super(OAuth2AuthorizationRequest.class);
        }

        @Override
        public OAuth2AuthorizationRequest deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);

            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                    .clientId(text(node, OAuth2ParameterNames.CLIENT_ID))
                    .state(text(node, OAuth2ParameterNames.STATE))
                    .redirectUri(text(node, OAuth2ParameterNames.REDIRECT_URI))
                    .authorizationUri(text(node, "authorization_uri"))
                    .scopes(scopes(node))
                    .additionalParameters(map(node, "additional_parameters"))
                    .attributes(map(node, "attributes"));

            return builder.build();
        }

        private static String text(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? null : value.asText();
        }

        private static Set<String> scopes(JsonNode node) {
            Set<String> scopes = new HashSet<>();
            JsonNode array = node.get(OAuth2ParameterNames.SCOPE);
            if (array != null) {
                for (JsonNode scope : array) {
                    scopes.add(scope.asText());
                }
            }
            return scopes;
        }

        private static Map<String, Object> map(JsonNode node, String field) {
            Map<String, Object> values = new LinkedHashMap<>();
            JsonNode object = node.get(field);
            if (object != null && !object.isNull()) {
                Iterator<Map.Entry<String, JsonNode>> entries = object.fields();
                while (entries.hasNext()) {
                    Map.Entry<String, JsonNode> entry = entries.next();
                    JsonNode value = entry.getValue();
                    if (value.isBoolean()) {
                        values.put(entry.getKey(), value.asBoolean());
                    } else if (value.isNumber()) {
                        values.put(entry.getKey(), value.numberValue());
                    } else {
                        values.put(entry.getKey(), value.asText());
                    }
                }
            }
            return values;
        }
    }
}
