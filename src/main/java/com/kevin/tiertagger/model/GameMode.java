package com.kevin.tiertagger.model;

import com.google.gson.JsonObject;
import com.kevin.tiertagger.TierTagger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record GameMode(String id, String title) {
    public static CompletableFuture<List<GameMode>> fetchGamemodes(HttpClient client) {
        String endpoint = TierTagger.getManager().getConfig().getBaseUrl() + "/tierlists";
        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    JsonObject obj = TierTagger.GSON.fromJson(r.body(), JsonObject.class);

                    return obj.entrySet().stream().map(e -> {
                        String title = e.getValue().getAsJsonObject().get("title").getAsString();
                        return new GameMode(e.getKey(), title);
                    }).toList();
                });
    }
}
