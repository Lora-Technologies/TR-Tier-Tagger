package com.loratechnologies.trtier;

import com.google.gson.reflect.TypeToken;
import com.loratechnologies.trtier.model.GameMode;
import com.loratechnologies.trtier.model.PlayerInfo;
import com.loratechnologies.trtier.model.TRTierPlayer;
import com.loratechnologies.trtier.util.UUIDFetcher;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TRTierCache {
    private static final List<GameMode> GAMEMODES = new ArrayList<>();
    private static final Map<String, TRTierPlayer> PLAYERS_BY_NAME = new ConcurrentHashMap<>();
    private static final Map<UUID, Optional<Map<String, PlayerInfo.Ranking>>> TIERS = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    public static void init() {
        fetchAllPlayers();
    }

    public static CompletableFuture<Void> fetchAllPlayers() {
        String endpoint = TRTier.getConfig().getApiUrl() + "/overall-ranking";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return TRTier.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> {
                    if (r.statusCode() != 200) {
                        TRTier.getLogger().warn("Failed to fetch overall ranking: HTTP {}", r.statusCode());
                        return;
                    }

                    String body = r.body();
                    if (body == null || body.isEmpty() || !body.trim().startsWith("[")) {
                        TRTier.getLogger().warn("Invalid overall ranking response");
                        return;
                    }

                    List<TRTierPlayer> players = TRTier.GSON.fromJson(body, new TypeToken<List<TRTierPlayer>>() {});
                    if (players == null || players.isEmpty()) {
                        TRTier.getLogger().warn("No players found in overall ranking");
                        return;
                    }

                    PLAYERS_BY_NAME.clear();
                    GAMEMODES.clear();
                    Set<String> kitIds = new LinkedHashSet<>();

                    for (TRTierPlayer player : players) {
                        PLAYERS_BY_NAME.put(player.name().toLowerCase(Locale.ROOT), player);
                        if (player.kitTiers() != null) {
                            kitIds.addAll(player.kitTiers().keySet());
                        }
                    }

                    for (String kitId : kitIds) {
                        String title = formatKitTitle(kitId);
                        GAMEMODES.add(new GameMode(kitId, title));
                    }

                    initialized = true;
                    TRTier.getLogger().info("Loaded {} players and {} kits from TRTier", PLAYERS_BY_NAME.size(), GAMEMODES.size());
                })
                .exceptionally(t -> {
                    TRTier.getLogger().error("Failed to fetch overall ranking", t);
                    return null;
                });
    }

    private static String formatKitTitle(String kitId) {
        return switch (kitId.toLowerCase(Locale.ROOT)) {
            case "nethpot" -> "Neth Pot";
            case "gapple" -> "Gapple";
            case "sword" -> "Sword";
            case "pot" -> "Pot";
            case "smp" -> "SMP";
            case "crystal" -> "Crystal";
            case "axe" -> "Axe";
            case "uhc" -> "UHC";
            case "mace" -> "Mace";
            case "diasmp" -> "Dia SMP";
            default -> kitId.substring(0, 1).toUpperCase() + kitId.substring(1);
        };
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static List<GameMode> getGamemodes() {
        if (GAMEMODES.isEmpty()) {
            return Collections.singletonList(GameMode.NONE);
        } else {
            return GAMEMODES;
        }
    }

    public static Optional<TRTierPlayer> getPlayerByName(String name) {
        return Optional.ofNullable(PLAYERS_BY_NAME.get(name.toLowerCase(Locale.ROOT)));
    }

    public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankings(UUID uuid) {
        return TIERS.get(uuid);
    }

    public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankingsByName(String name) {
        return getPlayerByName(name).map(TRTierPlayer::toRankingsMap);
    }

    public static void cachePlayerRankings(UUID uuid, Map<String, PlayerInfo.Ranking> rankings) {
        TIERS.put(uuid, Optional.ofNullable(rankings));
    }

    public static CompletableFuture<PlayerInfo> searchPlayer(String query) {
        Optional<TRTierPlayer> player = getPlayerByName(query);
        if (player.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return UUIDFetcher.fetchUUID(query)
                .thenApply(uuid -> player.get().toPlayerInfo(uuid));
    }

    public static Collection<TRTierPlayer> getAllPlayers() {
        return PLAYERS_BY_NAME.values();
    }

    public static void clearCache() {
        TIERS.clear();
    }

    public static void clearAll() {
        TIERS.clear();
        PLAYERS_BY_NAME.clear();
        GAMEMODES.clear();
        initialized = false;
        UUIDFetcher.clearCache();
    }

    public static GameMode findNextMode(GameMode current) {
        if (GAMEMODES.isEmpty()) {
            return GameMode.NONE;
        } else {
            int idx = GAMEMODES.indexOf(current);
            if (idx < 0) idx = -1;
            return GAMEMODES.get((idx + 1) % GAMEMODES.size());
        }
    }

    public static Optional<GameMode> findMode(String id) {
        return GAMEMODES.stream().filter(m -> m.id().equalsIgnoreCase(id)).findFirst();
    }

    public static GameMode findModeOrUgly(String id) {
        return findMode(id).orElseGet(() -> new GameMode(id, id));
    }

    private TRTierCache() {
    }
}
