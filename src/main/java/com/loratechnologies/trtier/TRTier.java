package com.loratechnologies.trtier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.loratechnologies.trtier.config.TRTierConfig;
import com.loratechnologies.trtier.model.GameMode;
import com.loratechnologies.trtier.model.PlayerInfo;
import com.mojang.brigadier.context.CommandContext;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TRTier implements ClientModInitializer {
    public static final String MOD_ID = "trtier";

    public static final Gson GSON = new GsonBuilder().create();

    private static final TRTierConfig config = new TRTierConfig(); // Placeholder for config management
    @Getter
    private static final Logger logger = LoggerFactory.getLogger(TRTier.class);
    @Getter
    private static final HttpClient client = HttpClient.newHttpClient();

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TRTier-AutoRefresh");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void onInitializeClient() {
        TRTierCache.init();

        scheduler.scheduleAtFixedRate(() -> {
            logger.info("Auto-refreshing tier data...");
            TRTierCache.fetchAllPlayers()
                    .thenRun(() -> logger.info("Tier data refreshed successfully"));
        }, 20, 20, TimeUnit.MINUTES);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(
                literal(MOD_ID)
                        .then(argument("player", StringArgumentType.word())
                                .executes(TRTier::displayTierInfo))));

        KeyBinding gamemodeKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("TRTier.keybind.gamemode", GLFW.GLFW_KEY_UNKNOWN, "TRTier.name"));
        
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (gamemodeKey.wasPressed()) {
                GameMode next = TRTierCache.findNextMode(config.getGameMode());
                config.setGameMode(next.id());

                if (mc.player != null) {
                    Text message = Text.literal("Displayed gamemode: ").append(next.asStyled(false));
                    mc.player.sendMessage(message, true);
                }
            }
        });
    }

    public static Text appendTier(UUID uuid, String playerName, Text text) {
        MutableText following = getPlayerTier(uuid, playerName)
                .map(entry -> {
                    Text tierText = getRankingText(entry.ranking(), false);

                    if (config.isShowIcons() && entry.mode() != null && entry.mode().icon().isPresent()) {
                        return Text.literal(entry.mode().icon().get().toString()).append(tierText);
                    } else {
                        return tierText.copy();
                    }
                })
                .orElse(null);

        if (following != null) {
            following.append(Text.literal(" | ").formatted(Formatting.GRAY));
            return following.append(text);
        }

        return text;
    }

    public static Optional<PlayerInfo.NamedRanking> getPlayerTier(UUID uuid, String playerName) {
        GameMode mode = config.getGameMode();

        Optional<Map<String, PlayerInfo.Ranking>> cached = TRTierCache.getPlayerRankings(uuid);
        if (cached == null || cached.isEmpty()) {
            Optional<Map<String, PlayerInfo.Ranking>> byName = TRTierCache.getPlayerRankingsByName(playerName);
            if (byName.isPresent()) {
                TRTierCache.cachePlayerRankings(uuid, byName.get());
                cached = byName;
            } else {
                return Optional.empty();
            }
        }

        return cached.map(rankings -> {
            PlayerInfo.Ranking ranking = rankings.get(mode.id());
            Optional<PlayerInfo.NamedRanking> highest = PlayerInfo.getHighestRanking(rankings);
            TRTierConfig.HighestMode highestMode = config.getHighestMode();

            if (ranking == null) {
                if (highestMode != TRTierConfig.HighestMode.NEVER && highest.isPresent()) {
                    return highest.get();
                } else {
                    return null;
                }
            } else {
                if (highestMode == TRTierConfig.HighestMode.ALWAYS && highest.isPresent()) {
                    return highest.get();
                } else {
                    return ranking.asNamed(mode);
                }
            }
        });
    }

    private static MutableText getTierText(int tier, int pos, boolean retired) {
        StringBuilder text = new StringBuilder();
        if (retired) text.append("R");
        text.append(pos == 0 ? "H" : "L").append("T").append(tier);

        int color = TRTier.getTierColor(text.toString());
        return Text.literal(text.toString()).styled(s -> s.withColor(color));
    }

    public static Text getRankingText(PlayerInfo.Ranking ranking, boolean showPeak) {
        if (ranking.retired() && ranking.peakTier() != null && ranking.peakPos() != null) {
            return getTierText(ranking.peakTier(), ranking.peakPos(), true);
        } else {
            MutableText tierText = getTierText(ranking.tier(), ranking.pos(), false);

            if (showPeak && ranking.comparablePeak() < ranking.comparableTier()) {
                // warning caused by potential NPE by unboxing of peak{Tier,Pos} which CANNOT happen, see impl of comparablePeak
                // noinspection DataFlowIssue
                tierText.append(Text.literal(" (peak: ").styled(s -> s.withColor(Formatting.GRAY)))
                        .append(getTierText(ranking.peakTier(), ranking.peakPos(), false))
                        .append(Text.literal(")").styled(s -> s.withColor(Formatting.GRAY)));
            }

            return tierText;
        }
    }

    private static int displayTierInfo(CommandContext<FabricClientCommandSource> ctx) {
        try {
            // We can't use EntityArgumentType.getPlayer directly with FabricClientCommandSource in this context easily without casting or using a different approach.
            // However, since we registered it as EntityArgumentType.player(), the argument is a EntitySelector.
            // But for client commands, it's often easier to just take a string or use a different approach if EntityArgumentType is server-side only in some mappings.
            // Actually, EntityArgumentType works on client but getPlayer expects ServerCommandSource usually.
            // Let's try to get the name directly from the argument if possible, or use a workaround.
            // A common workaround for client commands is to use StringArgumentType if EntityArgumentType is problematic, but let's try to fix the usage first.
            
            // In client commands, the source is FabricClientCommandSource.
            // EntityArgumentType.getPlayer(ctx, "player") tries to cast source to ServerCommandSource.
            
            // We can use getPlayerName from the selector if we can access it, or just use the argument as is if we change the type.
            // But since we want to keep it as a player selector...
            
            // Let's switch to StringArgumentType.word() for simplicity on the client side to avoid the ServerCommandSource dependency issue entirely,
            // as resolving a player entity on the client side is limited to loaded players anyway.
            
            String playerName = ctx.getArgument("player", String.class);
            Optional<Map<String, PlayerInfo.Ranking>> rankings = TRTierCache.getPlayerRankingsByName(playerName);

            if (rankings.isPresent()) {
                ctx.getSource().sendFeedback(printPlayerInfo(playerName, rankings.get()));
            } else {
                ctx.getSource().sendError(Text.of("Could not find player " + playerName + " in TRTier rankings."));
            }
        } catch (Exception e) {
            ctx.getSource().sendError(Text.of("Error retrieving player info."));
        }

        return 0;
    }

    private static Text printPlayerInfo(String name, Map<String, PlayerInfo.Ranking> rankings) {
        if (rankings.isEmpty()) {
            return Text.literal(name + " does not have any tiers.");
        } else {
            MutableText text = Text.empty().append("=== Rankings for " + name + " ===");

            rankings.forEach((m, r) -> {
                if (m == null) return;
                GameMode mode = TRTierCache.findModeOrUgly(m);
                Text tierText = getRankingText(r, true);
                text.append(Text.literal("\n").append(mode.asStyled(true)).append(": ").append(tierText));
            });

            return text;
        }
    }

    public static int getTierColor(String tier) {
        if (tier.startsWith("R")) {
            return config.getRetiredColor();
        } else {
            return config.getTierColors().getOrDefault(tier, 0xD3D3D3);
        }
    }

    public static TRTierConfig getConfig() {
        return config;
    }
}