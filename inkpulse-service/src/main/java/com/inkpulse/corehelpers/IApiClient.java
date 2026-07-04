package com.inkpulse.corehelpers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IApiClient {

    <T> CompletableFuture<T> getAsync(String endpoint, Map<String, String> headers, Class<T> responseType);

    CompletableFuture<String> getStringAsync(String endpoint, Map<String, String> headers);

    <TReq, TRes> CompletableFuture<TRes> postAsync(String endpoint, TReq data, Map<String, String> headers, Class<TRes> responseType);

    <TReq> CompletableFuture<String> postStringAsync(String endpoint, TReq data, Map<String, String> headers);

    <T> CompletableFuture<T> postFormDataAsync(String endpoint, Map<String, Object> formData, Map<String, String> headers, Class<T> responseType);

    <TReq, TRes> CompletableFuture<TRes> putAsync(String endpoint, TReq data, Map<String, String> headers, Class<TRes> responseType);

    <T> CompletableFuture<T> putFormDataAsync(String endpoint, Map<String, Object> formData, Map<String, String> headers, Class<T> responseType);

    <TReq, TRes> CompletableFuture<TRes> patchAsync(String endpoint, TReq data, Map<String, String> headers, Class<TRes> responseType);

    CompletableFuture<Boolean> deleteAsync(String endpoint, Map<String, String> headers);

    <T> CompletableFuture<T> deleteAsync(String endpoint, Map<String, String> headers, Class<T> responseType);
}
