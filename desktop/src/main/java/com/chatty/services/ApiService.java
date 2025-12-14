package com.chatty.services;

import com.google.gson.Gson;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ApiService {
    private static final String BASE_URL = "http://localhost:3000";
    private final OkHttpClient client;
    private final Gson gson;
    private final CookieJar cookieJar;
    public static String authToken = null;

    public ApiService() {
        this.cookieJar = new MemoryCookieJar();
        this.client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
        this.gson = new Gson();
    }

    // Simple in-memory cookie jar
    private static class MemoryCookieJar implements CookieJar {
        private final ConcurrentHashMap<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            cookieStore.put(url.host(), cookies);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new ArrayList<>();
        }
    }

    public <T> T get(String endpoint, Class<T> responseClass) throws IOException {
        return get(endpoint, responseClass, null);
    }

    public <T> T get(String endpoint, Class<T> responseClass, String cookie) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .get();
        // Cookies are handled by CookieJar automatically

        // Check token to add header
        if(authToken != null){
            requestBuilder.addHeader("Authorization", "Bearer " + authToken);
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Unexpected code " + response + ": " + errorBody);
            }
            String json = response.body().string();
            if (responseClass == String.class) {
                return (T) json;
            }
            return gson.fromJson(json, responseClass);
        }
    }

    public <T> T post(String endpoint, Object body, Class<T> responseClass) throws IOException {
        String json = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        
        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(requestBody)
                .addHeader("Content-Type", "application/json");

        // Check token to add header
        if(authToken != null){
            requestBuilder.addHeader("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                throw new IOException("Request failed: " + errorBody);
            }
            String responseJson = response.body().string();
            return gson.fromJson(responseJson, responseClass);
        }
    }

    public <T> T postWithCookies(String endpoint, Object body, Class<T> responseClass, String cookie) throws IOException {
        String json = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Cookie", cookie)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                throw new IOException("Request failed: " + errorBody);
            }
            String responseJson = response.body().string();
            return gson.fromJson(responseJson, responseClass);
        }
    }

    public OkHttpClient getClient() {
        return client;
    }

    public Gson getGson() {
        return gson;
    }
}

