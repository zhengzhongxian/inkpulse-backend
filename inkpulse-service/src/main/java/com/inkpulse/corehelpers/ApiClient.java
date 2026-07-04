package com.inkpulse.corehelpers;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class ApiClient implements IApiClient {

    private final HttpClient httpClient;

    public ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public <T> CompletableFuture<T> getAsync(String endpoint, Map<String, String> headers, Class<T> responseType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .GET();
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleResponse(response, responseType));
    }

    @Override
    public CompletableFuture<String> getStringAsync(String endpoint, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .GET();
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    ensureSuccess(response);
                    return response.body();
                });
    }

    @Override
    public <TReq, TRes> CompletableFuture<TRes> postAsync(String endpoint, TReq data, Map<String, String> headers, Class<TRes> responseType) {
        String json = JsonHelper.serializeSafe(data);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleResponse(response, responseType));
    }

    @Override
    public <TReq> CompletableFuture<String> postStringAsync(String endpoint, TReq data, Map<String, String> headers) {
        String json = JsonHelper.serializeSafe(data);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    ensureSuccess(response);
                    return response.body();
                });
    }

    @Override
    public <T> CompletableFuture<T> postFormDataAsync(String endpoint, Map<String, Object> formData, Map<String, String> headers, Class<T> responseType) {
        String formBody = formData.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue() != null ? entry.getValue().toString() : "", StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody));
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleResponse(response, responseType));
    }

    @Override
    public <TReq, TRes> CompletableFuture<TRes> putAsync(String endpoint, TReq data, Map<String, String> headers, Class<TRes> responseType) {
        String json = JsonHelper.serializeSafe(data);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json));
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleResponse(response, responseType));
    }

    @Override
    public <T> CompletableFuture<T> putFormDataAsync(String endpoint, Map<String, Object> formData, Map<String, String> headers, Class<T> responseType) {
        String formBody = formData.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue() != null ? entry.getValue().toString() : "", StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(formBody));
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleResponse(response, responseType));
    }

    @Override
    public <TReq, TRes> CompletableFuture<TRes> patchAsync(String endpoint, TReq data, Map<String, String> headers, Class<TRes> responseType) {
        String json = JsonHelper.serializeSafe(data);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json));
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleResponse(response, responseType));
    }

    @Override
    public CompletableFuture<Boolean> deleteAsync(String endpoint, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .DELETE();
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> response.statusCode() >= 200 && response.statusCode() < 300);
    }

    @Override
    public <T> CompletableFuture<T> deleteAsync(String endpoint, Map<String, String> headers, Class<T> responseType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .DELETE();
        addHeaders(builder, headers);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleResponse(response, responseType));
    }

    private void addHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (headers == null) return;
        headers.forEach(builder::header);
    }

    private void ensureSuccess(HttpResponse<?> response) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new RuntimeException("HTTP Request failed with status code: " + status);
        }
    }

    private <T> T handleResponse(HttpResponse<String> response, Class<T> responseType) {
        ensureSuccess(response);
        return JsonHelper.deserializeSafe(response.body(), responseType);
    }
}
