package com.loratechnologies.trtier.config;

import com.google.gson.internal.LinkedTreeMap;
import com.loratechnologies.trtier.TRTierCache;
import com.loratechnologies.trtier.model.GameMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.util.TranslatableOption;

import java.io.Serializable;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TRTierConfig implements Serializable {
    private boolean enabled = true;
    private String gameMode = "vanilla";
    private boolean showRetired = true;
    private HighestMode highestMode = HighestMode.NOT_FOUND;
    private boolean showIcons = true;
    private boolean playerList = true;
    private int retiredColor = 0xa2d6ff;
    // note: this is a GSON internal class. this *might* break in the future
    private LinkedTreeMap<String, Integer> tierColors = defaultColors();

    // === internal stuff ===

    private static final String DEFAULT_API_URL = "https://loratech.dev/api";
    private static final String OLD_MCTIERS_URL = "https://mctiers.com/api";
    
    private String apiUrl = DEFAULT_API_URL;

    public String getApiUrl() {
        if (apiUrl == null || apiUrl.contains("mctiers.com")) {
            apiUrl = DEFAULT_API_URL;
        }
        return apiUrl;
    }

    public GameMode getGameMode() {
        Optional<GameMode> opt = TRTierCache.findMode(this.gameMode);
        if (opt.isPresent()) {
            return opt.get();
        } else {
            GameMode first = TRTierCache.getGamemodes().get(0);
            if (!first.isNone()) this.gameMode = first.id();
            return first;
        }
    }

    private static LinkedTreeMap<String, Integer> defaultColors() {
        LinkedTreeMap<String, Integer> colors = new LinkedTreeMap<>();
        colors.put("HT1", 0xe8ba3a);
        colors.put("LT1", 0xd5b355);
        colors.put("HT2", 0xc4d3e7);
        colors.put("LT2", 0xa0a7b2);
        colors.put("HT3", 0xf89f5a);
        colors.put("LT3", 0xc67b42);
        colors.put("HT4", 0x81749a);
        colors.put("LT4", 0x655b79);
        colors.put("HT5", 0x8f82a8);
        colors.put("LT5", 0x655b79);

        return colors;
    }

    @Getter
    @AllArgsConstructor
    public enum HighestMode implements TranslatableOption {
        NEVER(0, "trtier.highest.never"),
        NOT_FOUND(1, "trtier.highest.not_found"),
        ALWAYS(2, "trtier.highest.always"),
        ;

        private final int id;
        private final String translationKey;
    }
}
