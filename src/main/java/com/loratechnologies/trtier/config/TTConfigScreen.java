package com.loratechnologies.trtier.config;

import com.loratechnologies.trtier.TRTier;
import com.loratechnologies.trtier.TRTierCache;
import com.loratechnologies.trtier.model.GameMode;
import com.loratechnologies.trtier.tierlist.PlayerSearchScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class TTConfigScreen extends Screen {
        private final Screen parent;
        private final TRTierConfig config;

        public TTConfigScreen(Screen parent) {
                super(Text.translatable("trtier.config"));
                this.parent = parent;
                this.config = TRTier.getConfig();
        }

        @Override
        protected void init() {
                int y = 40;
                int center = this.width / 2;

                this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.isEnabled())
                                .build(center - 100, y, 200, 20, Text.translatable("trtier.config.enabled"),
                                                (button, value) -> config.setEnabled(value)));
                y += 24;

                this.addDrawableChild(CyclingButtonWidget.<GameMode>builder(m -> Text.literal(m.title()))
                                .values(TRTierCache.getGamemodes())
                                .initially(TRTierCache.findModeOrUgly(config.getGameMode().id()))
                                .build(center - 100, y, 200, 20, Text.translatable("trtier.config.gamemode"),
                                                (button, value) -> config.setGameMode(value.id())));
                y += 24;

                this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.isShowRetired())
                                .build(center - 100, y, 200, 20, Text.translatable("trtier.config.retired"),
                                                (button, value) -> config.setShowRetired(value)));
                y += 24;

                this.addDrawableChild(CyclingButtonWidget.<TRTierConfig.HighestMode>builder(m -> Text.literal(m.name()))
                                .values(TRTierConfig.HighestMode.values())
                                .initially(config.getHighestMode())
                                .build(center - 100, y, 200, 20, Text.translatable("trtier.config.highest"),
                                                (button, value) -> config.setHighestMode(value)));
                y += 24;

                this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.isShowIcons())
                                .build(center - 100, y, 200, 20, Text.translatable("trtier.config.icons"),
                                                (button, value) -> config.setShowIcons(value)));
                y += 24;

                this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.isPlayerList())
                                .build(center - 100, y, 200, 20, Text.translatable("trtier.config.playerList"),
                                                (button, value) -> config.setPlayerList(value)));
                y += 24;

                this.addDrawableChild(ButtonWidget
                                .builder(Text.translatable("trtier.clear"), button -> TRTierCache.clearCache())
                                .dimensions(center - 100, y, 200, 20)
                                .build());
                y += 24;

                this.addDrawableChild(ButtonWidget
                                .builder(Text.translatable("trtier.config.search"),
                                                button -> MinecraftClient.getInstance()
                                                                .setScreen(new PlayerSearchScreen(this)))
                                .dimensions(center - 100, y, 200, 20)
                                .build());
                y += 24;

                this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
                                .dimensions(center - 100, this.height - 27, 200, 20)
                                .build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                // ? if >=1.20.4 {
                /* this.renderBackground(context, mouseX, mouseY, delta); */
                // ?} else {
                this.renderBackground(context);
                // ?}
                context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
                super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public void close() {
                this.client.setScreen(this.parent);
        }
}