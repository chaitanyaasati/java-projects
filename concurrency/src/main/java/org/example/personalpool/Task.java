package org.example.personalpool;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Task {
    private static final URI DEFAULT_API_URI =
            URI.create("https://jsonplaceholder.typicode.com/todos/1");

    private final HttpClient httpClient;
    private final URI apiUri;

    public Task() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build(), DEFAULT_API_URI);
    }

    Task(HttpClient httpClient, URI apiUri) {
        this.httpClient = httpClient;
        this.apiUri = apiUri;
    }

    public String execute() throws InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(apiUri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "API request failed with HTTP status " + response.statusCode());
            }

            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to call " + apiUri, e);
        }
    }
}
