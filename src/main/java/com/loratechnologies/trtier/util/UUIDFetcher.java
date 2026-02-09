package com.loratechnologies.trtier.util;

import com.google.gson.JsonObject;
import com.loratechnologies.trtier.TRTier;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UUIDFetcher {
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final Map<String, String> UUID_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<String>> LOADING_UUIDS = new ConcurrentHashMap<>();

    public static CompletableFuture<String> fetchUUID(String username) {
        if (username == null || username.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        String key = username.toLowerCase();

        if (UUID_CACHE.containsKey(key)) {
            return CompletableFuture.completedFuture(UUID_CACHE.get(key));
        }

        if (LOADING_UUIDS.containsKey(key)) {
            return LOADING_UUIDS.get(key);
        }

        CompletableFuture<String> future = fetchFromMojang(username);
        LOADING_UUIDS.put(key, future);

        future.whenComplete((uuid, ex) -> {
            LOADING_UUIDS.remove(key);
            if (uuid != null) {
                UUID_CACHE.put(key, uuid);
            }
        });

        return future;
    }

    private static CompletableFuture<String> fetchFromMojang(String username) {
        String url = String.format(MOJANG_API_URL, username);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        return TRTier.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        TRTier.getLogger().warn("Failed to fetch UUID for {}: HTTP {}", username, response.statusCode());
                        return null;
                    }

                    try {
                        JsonObject json = TRTier.GSON.fromJson(response.body(), JsonObject.class);
                        if (json != null && json.has("id")) {
                            String uuid = json.get("id").getAsString();
                            return formatUUID(uuid);
                        }
                    } catch (Exception e) {
                        TRTier.getLogger().error("Failed to parse UUID response for {}", username, e);
                    }

                    return null;
                })
                .exceptionally(ex -> {
                    TRTier.getLogger().error("Failed to fetch UUID for {}", username, ex);
                    return null;
                });
    }

    private static String formatUUID(String uuid) {
        if (uuid == null || uuid.length() != 32) {
            return uuid;
        }
        return uuid.substring(0, 8) + "-" +
               uuid.substring(8, 12) + "-" +
               uuid.substring(12, 16) + "-" +
               uuid.substring(16, 20) + "-" +
               uuid.substring(20, 32);
    }

    public static String getCachedUUID(String username) {
        if (username == null || username.isEmpty()) return null;
        return UUID_CACHE.get(username.toLowerCase());
    }

    public static void clearCache() {
        UUID_CACHE.clear();
    }
}
