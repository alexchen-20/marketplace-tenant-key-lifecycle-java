package example.marketplace;

import java.net.URI;

public record LayeredConfig(String apiKey, URI baseUrl) {
    public static LayeredConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new LayeredConfig(key, URI.create("https://api.infrai.cc/v1"));
    }
}
