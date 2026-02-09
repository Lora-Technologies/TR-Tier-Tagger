package com.loratechnologies.trtier.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public record TRTierPlayer(
        String name,
        @SerializedName("totalPoints") int totalPoints,
        @SerializedName("kitTiers") Map<String, String> kitTiers,
        String region,
        int position,
        TitleInfo title
) {
    public record TitleInfo(String name, int points) {}

    public int parseTier(String tierStr) {
        if (tierStr == null || tierStr.length() < 3) return 5;
        try {
            return Integer.parseInt(tierStr.substring(2));
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public int parsePos(String tierStr) {
        if (tierStr == null || tierStr.length() < 2) return 1;
        char pos = Character.toUpperCase(tierStr.charAt(0));
        return pos == 'H' ? 0 : 1;
    }

    public PlayerInfo.Ranking toRanking(String kitId) {
        String tierStr = kitTiers.get(kitId);
        if (tierStr == null) return null;
        int tier = parseTier(tierStr);
        int pos = parsePos(tierStr);
        return new PlayerInfo.Ranking(tier, pos, null, null, 0, false);
    }

    public Map<String, PlayerInfo.Ranking> toRankingsMap() {
        java.util.Map<String, PlayerInfo.Ranking> rankings = new java.util.HashMap<>();
        if (kitTiers == null) return rankings;
        for (Map.Entry<String, String> entry : kitTiers.entrySet()) {
            if (entry.getKey() == null) continue;
            PlayerInfo.Ranking ranking = toRanking(entry.getKey());
            if (ranking != null) {
                rankings.put(entry.getKey(), ranking);
            }
        }
        return rankings;
    }

    public PlayerInfo toPlayerInfo(String uuid) {
        return new PlayerInfo(
                uuid,
                name,
                toRankingsMap(),
                region,
                totalPoints,
                position,
                java.util.List.of(),
                totalPoints >= 250
        );
    }
}
