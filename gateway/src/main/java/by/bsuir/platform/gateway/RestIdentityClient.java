package by.bsuir.platform.gateway;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Clients;
import by.bsuir.platform.contracts.Failures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class RestIdentityClient implements Clients.IdentityClient {

    private final RestClient http;
    private final ObjectMapper json = new ObjectMapper();

    public RestIdentityClient(@Value("${auth.service.url:http://127.0.0.1:8081}") String url) {
        this.http = RestClient.builder().baseUrl(url).build();
    }

    @Override
    public Api.UserView me(long userId) {
        try {
            return http.get().uri("/auth/users/{id}", userId).retrieve().body(Api.UserView.class);
        } catch (RestClientResponseException ex) {
            throw failure(ex);
        }
    }

    @Override
    public Api.AuthPayload register(String email, String password, String fullName, String phone) {
        try {
            return http.post().uri("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", password, "fullName", fullName, "phone", phone == null ? "" : phone))
                    .retrieve()
                    .body(Api.AuthPayload.class);
        } catch (RestClientResponseException ex) {
            throw failure(ex);
        }
    }

    @Override
    public Api.AuthPayload login(String email, String password) {
        try {
            return http.post().uri("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(Api.AuthPayload.class);
        } catch (RestClientResponseException ex) {
            throw failure(ex);
        }
    }

    @Override
    public Api.UserView userByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return http.post().uri("/auth/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("token", token))
                    .retrieve()
                    .body(Api.UserView.class);
        } catch (RestClientResponseException ex) {
            return null;
        }
    }

    @Override
    public void logout(String token) {
        try {
            http.post().uri("/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("token", token))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw failure(ex);
        }
    }

    private RuntimeException failure(RestClientResponseException ex) {
        String message = ex.getResponseBodyAsString();
        try {
            JsonNode node = json.readTree(message);
            if (node.hasNonNull("message")) {
                message = node.get("message").asText();
            }
        } catch (Exception ignored) {
            message = "Сервис аутентификации недоступен";
        }
        if (ex.getStatusCode().value() == 401) {
            return new Failures.UnauthorizedException(message);
        }
        if (ex.getStatusCode().value() == 404) {
            return new Failures.NotFoundException(message);
        }
        return new Failures.BadRequestException(message);
    }
}
