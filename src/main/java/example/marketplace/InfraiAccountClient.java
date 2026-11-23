package example.marketplace;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiAccountClient {
    private final LayeredConfig config;
    private final HttpClient http;

    public InfraiAccountClient(LayeredConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String createScopedKey(String tenantId, String idempotencyKey) throws IOException, InterruptedException {
        String body = "{\"project_id\":\"" + json(tenantId) + "\",\"name\":\"course-tenant\","
                + "\"scopes\":[\"account.keys.create\",\"auth.user.create\"],\"idempotency_key\":\""
                + json(idempotencyKey) + "\"}";
        return write("POST", "/account/keys/create", body, idempotencyKey);
    }

    public String createOwner(String email, String idempotencyKey) throws IOException, InterruptedException {
        String body = "{\"email\":\"" + json(email) + "\",\"name\":\"course tenant owner\","
                + "\"metadata\":{\"role\":\"marketplace-tenant\"},\"idempotency_key\":\""
                + json(idempotencyKey) + "\"}";
        return write("POST", "/auth/user/create", body, idempotencyKey);
    }

    public String deleteOwner(String userId) throws IOException, InterruptedException {
        return write("DELETE", "/auth/user/delete/" + userId, "", "owner-delete-" + userId);
    }

    public String revokeTemporaryKey(String keyId) throws IOException, InterruptedException {
        return write("DELETE", "/account/keys/revoke/" + keyId, "", "temporary-revoke-" + keyId);
    }

    public static String responseId(String response, String... names) {
        for (String name : names) {
            String marker = "\"" + name + "\":\"";
            int start = response.indexOf(marker);
            if (start >= 0) {
                start += marker.length();
                int end = response.indexOf('"', start);
                if (end > start) {
                    return response.substring(start, end);
                }
            }
        }
        throw new IllegalStateException("Infrai response did not contain an identifier: " + response);
    }

    private String write(String method, String path, String body, String idempotencyKey)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) {
                throw new IllegalStateException("Infrai rejected request: " + envelope);
            }
            if (response.statusCode() == 429 && attempt < 3) {
                String retryAfter = response.headers().firstValue("Retry-After").orElse("");
                long seconds = retryAfter.matches("\\d+") ? Long.parseLong(retryAfter) : 1L << attempt;
                Thread.sleep(Math.min(seconds, 30) * 1000L);
                continue;
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response: " + response.statusCode());
            }
            return envelope;
        }
        throw new IOException("request retry budget exhausted");
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
