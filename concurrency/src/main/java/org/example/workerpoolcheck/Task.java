package org.example.workerpoolcheck;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Task {
    private static final String API_URL =
            "https://jsonplaceholder.typicode.com/todos/";

    private final HttpClient httpClient;
    private final int todoId;

    public Task(int todoId) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build(), todoId);
    }

    Task(HttpClient httpClient, int todoId) {
        if (todoId <= 0) {
            throw new IllegalArgumentException("todoId must be greater than zero");
        }

        this.httpClient = httpClient;
        this.todoId = todoId;
    }

    public String execute() throws InterruptedException {
        URI requestUri = URI.create(API_URL + todoId);
        HttpRequest request = HttpRequest.newBuilder(requestUri)
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
            throw new IllegalStateException("Unable to call " + requestUri, e);
        }
    }
}
