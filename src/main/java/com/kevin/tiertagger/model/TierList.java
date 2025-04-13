package com.kevin.tiertagger.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum TierList {
    MCTIERS("MCTiers", "https://api.uku3lig.net/tiers"),
    SUBTIERS("SubTiers", "https://subtiers.net/api"),
    ;

    private final String name;
    private final String url;

    public static Optional<TierList> findByUrl(String url) {
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        final String finalUrl = url; // i :heart: java
        return Arrays.stream(values()).filter(list -> list.url.equals(finalUrl)).findFirst();
    }
}
