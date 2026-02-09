package com.loratechnologies.trtier.tierlist;

import com.loratechnologies.trtier.TRTierCache;
import com.loratechnologies.trtier.model.PlayerInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.toast.SystemToast;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget textField;
    private ButtonWidget searchButton;

    private boolean searching = false;

    public PlayerSearchScreen(Screen parent) {
        super(Text.of("Player Search"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        String username = I18n.translate("trtier.search.user");
        this.textField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 116, 200, 20, Text.of(username));
        this.textField.setMaxLength(32);
        this.addSelectableChild(this.textField);

        this.searchButton = this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("trtier.search"), button -> this.loadAndShowProfile())
                        .dimensions(this.width / 2 - 100, this.height / 4 + 96 + 12, 200, 20)
                        .build());

        this.addDrawableChild(
                ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.close())
                        .dimensions(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20)
                        .build());

        this.setInitialFocus(this.textField);
    }

    @Override
    public void tick() {
        super.tick();
        this.searchButton.active = !this.textField.getText().isEmpty() && !searching;
    }

    private void loadAndShowProfile() {
        String username = this.textField.getText();
        this.searching = true;
        this.searchButton.setMessage(Text.translatable("trtier.search.loading"));

        TRTierCache.searchPlayer(username)
                .thenApply(info -> new PlayerInfoScreen(this, info))
                .thenAccept(screen -> MinecraftClient.getInstance()
                        .execute(() -> MinecraftClient.getInstance().setScreen(screen)))
                .whenComplete((v, t) -> {
                    if (t != null) {
                        MinecraftClient.getInstance().getToastManager()
                                .add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION,
                                        Text.translatable("trtier.search.unknown"), null));
                    }
                    this.searching = false;
                    this.searchButton.setMessage(Text.translatable("trtier.search"));
                });
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        String string = this.textField.getText();
        this.init(client, width, height);
        this.textField.setText(string);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // ? if >=1.20.4 {
        /* this.renderBackground(context, mouseX, mouseY, delta); */
        // ?} else {
        this.renderBackground(context);
        // ?}
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);
        this.textField.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
