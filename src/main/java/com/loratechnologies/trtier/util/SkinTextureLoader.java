package com.loratechnologies.trtier.util;

import com.loratechnologies.trtier.TRTier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SkinTextureLoader {
    private static final String SKIN_API_URL = "https://mc-heads.net/body/%s/right";
    private static final Map<String, Identifier> LOADED_SKINS = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Identifier>> LOADING_SKINS = new ConcurrentHashMap<>();

    public static CompletableFuture<Identifier> loadSkin(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        String key = uuid.toLowerCase();
        
        if (LOADED_SKINS.containsKey(key)) {
            return CompletableFuture.completedFuture(LOADED_SKINS.get(key));
        }
        
        if (LOADING_SKINS.containsKey(key)) {
            return LOADING_SKINS.get(key);
        }
        
        CompletableFuture<Identifier> future = downloadSkin(uuid);
        LOADING_SKINS.put(key, future);
        
        future.whenComplete((id, ex) -> {
            LOADING_SKINS.remove(key);
            if (id != null) {
                LOADED_SKINS.put(key, id);
            }
        });
        
        return future;
    }

    private static CompletableFuture<Identifier> downloadSkin(String uuid) {
        String url = String.format(SKIN_API_URL, uuid);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        return TRTier.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenCompose(response -> {
                    if (response.statusCode() != 200) {
                        TRTier.getLogger().warn("Failed to fetch skin for {}: HTTP {}", uuid, response.statusCode());
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    try (InputStream is = response.body()) {
                        NativeImage image = NativeImage.read(is);
                        Identifier id = Identifier.of("trtier", "skin/" + uuid.toLowerCase());
                        
                        CompletableFuture<Identifier> textureFuture = new CompletableFuture<>();
                        MinecraftClient.getInstance().execute(() -> {
                            try {
                                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                                MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
                                textureFuture.complete(id);
                            } catch (Exception e) {
                                textureFuture.completeExceptionally(e);
                            }
                        });
                        
                        return textureFuture;
                    } catch (Exception e) {
                        TRTier.getLogger().error("Failed to load skin image for {}", uuid, e);
                        return CompletableFuture.completedFuture(null);
                    }
                })
                .exceptionally(ex -> {
                    TRTier.getLogger().error("Failed to download skin for {}", uuid, ex);
                    return null;
                });
    }

    public static Identifier getSkinIfLoaded(String uuid) {
        if (uuid == null || uuid.isEmpty()) return null;
        return LOADED_SKINS.get(uuid.toLowerCase());
    }

    public static void clearCache() {
        LOADED_SKINS.clear();
    }
}
