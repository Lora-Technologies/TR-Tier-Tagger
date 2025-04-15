package com.kevin.tiertagger.config;

import com.kevin.tiertagger.TierCache;
import com.google.gson.internal.LinkedTreeMap;
import com.kevin.tiertagger.model.GameMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.util.TranslatableOption;

import java.io.Serializable;
import java.util.NoSuchElementException;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TierTaggerConfig implements Serializable {
    private boolean enabled = true;
    private String gameMode = "vanilla";
    private boolean showRetired = true;
    // private HighestMode highestMode = HighestMode.NOT_FOUND;
    private Statistic shownStatistic = Statistic.TIER;
    // private boolean showIcons = true;
    private int retiredColor = 0xa2d6ff;
    // note: this is a GSON internal class. this *might* break in the future
    private LinkedTreeMap<String, Integer> tierColors = defaultColors();

    // === internal stuff ===

    /**
     * <p>the field was renamed to do a little trolling and force it setting to the default value in players' config</p>
     * <p>previous name(s): {@code apiUrl}</p>
     */
    private String baseUrl = "https://api.uku3lig.net/tiers";

    public GameMode getGameMode() {
        try {
            return TierCache.findMode(this.gameMode);
        } catch (NoSuchElementException e) {
            GameMode first = TierCache.GAMEMODES.getFirst();
            this.gameMode = first.id();
            return first;
        }
    }

    private static LinkedTreeMap<String, Integer> defaultColors() {
        LinkedTreeMap<String, Integer> colors = new LinkedTreeMap<>();
        colors.put("HT1", 0xffc935);
        colors.put("LT1", 0xd5b355);
        colors.put("HT2", 0xa4b3c7);
        colors.put("LT2", 0x888d95);
        colors.put("HT3", 0xb56326);
        colors.put("LT3", 0x8f5931);
        colors.put("HT4", 0x655b79);
        colors.put("LT4", 0x655b79);
        colors.put("HT5", 0x655b79);
        colors.put("LT5", 0x655b79);

        return colors;
    }

    @Getter
    @AllArgsConstructor
    public enum Statistic implements TranslatableOption {
        TIER(0, "tiertagger.stat.tier"),
        RANK(1, "tiertagger.stat.rank"),
        ;

        private final int id;
        private final String translationKey;
    }

    @Getter
    @AllArgsConstructor
    public enum HighestMode implements TranslatableOption {
        NEVER(0, "tiertagger.highest.never"),
        NOT_FOUND(1, "tiertagger.highest.not_found"),
        ALWAYS(2, "tiertagger.highest.always"),
        ;

        private final int id;
        private final String translationKey;
    }
}
