package com.loratechnologies.trtier.tierlist;

import com.loratechnologies.trtier.TRTier;
import com.loratechnologies.trtier.model.GameMode;
import com.loratechnologies.trtier.model.PlayerInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import com.loratechnologies.trtier.util.SkinTextureLoader;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class PlayerInfoScreen extends Screen {
    private final Screen parent;
    private final PlayerInfo info;
    private Identifier skinTexture = null;
    private boolean skinRequested = false;
    private int skinWidth = 0;
    private int skinHeight = 0;

    public PlayerInfoScreen(Screen parent, PlayerInfo info) {
        super(Text.of("Player Info"));
        this.parent = parent;
        this.info = info;
    }

    @Override
    protected void init() {
        this.addDrawableChild(
                ButtonWidget.builder(ScreenTexts.DONE, button -> MinecraftClient.getInstance().setScreen(parent))
                        .dimensions(this.width / 2 - 100, this.height - 27, 200, 20)
                        .build());

        if (this.info == null) {
            return;
        }

        if (!skinRequested) {
            skinRequested = true;
            SkinTextureLoader.loadSkin(this.info.uuid())
                    .thenAccept(result -> MinecraftClient.getInstance().execute(() -> {
                        if (result != null) {
                            this.skinTexture = result;
                            this.skinWidth = 64;
                            this.skinHeight = 144;
                        }
                    }));
        }

        int panelX = this.width / 2 - 80;
        java.util.List<PlayerInfo.NamedRanking> sortedTiers = this.info.getSortedTiers();
        int rankingHeight = Math.max(sortedTiers.size(), 1) * 12;
        int infoHeight = 70;
        int startY = (this.height - infoHeight - rankingHeight) / 2 + 15;
        int rankingY = startY + infoHeight;

        if (sortedTiers.isEmpty()) {
            TextWidget noRankText = new TextWidget(Text.literal("No rankings").styled(s -> s.withColor(0x888888)),
                    this.textRenderer);
            noRankText.setX(panelX);
            noRankText.setY(rankingY);
            this.addDrawableChild(noRankText);
        }

        for (PlayerInfo.NamedRanking namedRanking : sortedTiers) {
            TextWidget text = new TextWidget(formatTier(namedRanking.mode(), namedRanking.ranking()),
                    this.textRenderer);
            text.setX(panelX);
            text.setY(rankingY);

            String date = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)
                    .format(Instant.ofEpochSecond(namedRanking.ranking().attained()));
            Text tooltipText = Text.literal("Attained: " + date + "\nPoints: " + points(namedRanking.ranking()))
                    .formatted(Formatting.GRAY);
            text.setTooltip(Tooltip.of(tooltipText));
            this.addDrawableChild(text);
            rankingY += 12;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // ? if >=1.20.4 {
        /* this.renderBackground(context, mouseX, mouseY, delta); */
        // ?} else {
        this.renderBackground(context);
        // ?}
        super.render(context, mouseX, mouseY, delta);

        if (this.info == null) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Player not found").styled(s -> s.withColor(0xFF5555)),
                    this.width / 2, this.height / 2, 0xFFFFFFFF);
            return;
        }

        java.util.List<PlayerInfo.NamedRanking> renderTiers = this.info.getSortedTiers();
        int rankingHeight = Math.max(renderTiers.size(), 1) * 12;
        int infoHeight = 70;
        int panelHeight = infoHeight + rankingHeight + 30;
        int panelWidth = 220;
        int panelX = this.width / 2 - 90;
        int panelY = (this.height - panelHeight) / 2;

        int skinDisplayHeight = 100;
        int skinDisplayWidth = (int) (skinDisplayHeight * (64.0 / 144.0));
        int skinX = panelX - skinDisplayWidth - 15;
        int skinY = panelY + (panelHeight / 2) - (skinDisplayHeight / 2);

        if (this.skinTexture != null) {
            context.fill(skinX - 5, skinY - 5, skinX + skinDisplayWidth + 5, skinY + skinDisplayHeight + 5, 0x60000000);
            drawBorder(context, skinX - 5, skinY - 5, skinDisplayWidth + 10, skinDisplayHeight + 10, 0x80FFFFFF);

            // ? if >=1.21.4 {
            /*
             * context.drawTexture(RenderLayer::getGuiTextured, this.skinTexture, skinX,
             * skinY, 0, 0, skinDisplayWidth,
             * skinDisplayHeight, skinDisplayWidth, skinDisplayHeight);
             */
            // ?} else {
            context.drawTexture(this.skinTexture, skinX, skinY, 0, 0, skinDisplayWidth, skinDisplayHeight,
                    skinDisplayWidth, skinDisplayHeight);
            // ?}
        }

        context.fill(panelX - 10, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);
        drawBorder(context, panelX - 10, panelY, panelWidth + 10, panelHeight, 0x60FFFFFF);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(this.info.name() + "'s Profile").styled(s -> s.withBold(true)),
                panelX + panelWidth / 2 - 5, panelY + 8, 0xFFFFFFFF);

        int startY = panelY + 28;
        int textX = panelX;

        context.drawTextWithShadow(this.textRenderer, getRegionText(this.info), textX, startY, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, getPointsText(this.info), textX, startY + 14, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, getRankText(this.info), textX, startY + 28, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Rankings:").styled(s -> s.withColor(0xAAAAAA)),
                textX, startY + 46, 0xFFFFFFFF);
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private Text formatTier(@NotNull GameMode gamemode, PlayerInfo.Ranking ranking) {
        Text tierText = TRTier.getRankingText(ranking, true);

        return Text.empty()
                .append(gamemode.asStyled(true))
                .append(Text.literal(": ").formatted(Formatting.GRAY))
                .append(tierText);
    }

    private Text getRegionText(PlayerInfo info) {
        return Text.empty()
                .append(Text.literal("Region: "))
                .append(Text.literal(info.region()).styled(s -> s.withColor(info.getRegionColor())));
    }

    private Text getPointsText(PlayerInfo info) {
        PlayerInfo.PointInfo pointInfo = info.getPointInfo();

        return Text.empty()
                .append(Text.literal("Points: "))
                .append(Text.literal(info.points() + " ").styled(s -> s.withColor(pointInfo.getColor())))
                .append(Text.literal("(" + pointInfo.getTitle() + ")")
                        .styled(s -> s.withColor(pointInfo.getAccentColor())));
    }

    private Text getRankText(PlayerInfo info) {
        int color = switch (info.overall()) {
            case 1 -> 0xe5ba43;
            case 2 -> 0x808c9c;
            case 3 -> 0xb56326;
            default -> 0x1e2634;
        };

        return Text.empty()
                .append(Text.literal("Global rank: "))
                .append(Text.literal("#" + info.overall()).styled(s -> s.withColor(color)));
    }

    private int points(PlayerInfo.Ranking ranking) {
        return switch (ranking.tier()) {
            case 1 -> ranking.pos() == 0 ? 60 : 45;
            case 2 -> ranking.pos() == 0 ? 30 : 20;
            case 3 -> ranking.pos() == 0 ? 10 : 6;
            case 4 -> ranking.pos() == 0 ? 4 : 3;
            case 5 -> ranking.pos() == 0 ? 2 : 1;
            default -> 0;
        };
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
