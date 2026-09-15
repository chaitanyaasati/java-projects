package org.example.compfuture;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Calls Open-Meteo's public forecast API with latitude and longitude parameters.
 */
public class ThirdPartyApiExample {

    private static final String API_URL =
            "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=%f&longitude=%f&current=temperature_2m,wind_speed_10m";

    public static void main(String[] args) {
        double latitude = args.length > 0 ? Double.parseDouble(args[0]) : 17.3850;
        double longitude = args.length > 1 ? Double.parseDouble(args[1]) : 78.4867;

        CompletableFuture<Void> request = fetchCurrentWeather(latitude, longitude)
                .thenAccept(body -> System.out.println("API response:\n" + body))
                .exceptionally(error -> {
                    System.err.println("API call failed: " + error.getMessage());
                    return null;
                });

        // Wait for the HTTP call and every operation chained above to complete.
        request.join();
    }

    private static CompletableFuture<String> fetchCurrentWeather(
            double latitude, double longitude) {
        validateCoordinates(latitude, longitude);

        URI uri = URI.create(String.format(Locale.ROOT, API_URL, latitude, longitude));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException(
                                "HTTP " + response.statusCode() + ": " + response.body());
                    }
                    return response.body();
                });
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }
}
