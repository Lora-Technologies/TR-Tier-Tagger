package com.kevin.tiertagger.tierlist;

import com.kevin.tiertagger.TierCache;
import com.kevin.tiertagger.mixin.MinecraftClientAccessor;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.ApiServices;
import net.uku3lig.ukulib.config.option.widget.TextInputWidget;
import net.uku3lig.ukulib.config.screen.CloseableScreen;
import net.uku3lig.ukulib.utils.Ukutils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PlayerSearchScreen extends CloseableScreen {
    private TextInputWidget textField;
    private ButtonWidget searchButton;

    private boolean searching = false;

    public PlayerSearchScreen(Screen parent) {
        super("Player Search", parent);
    }

    @Override
    protected void init() {
        String username = I18n.translate("tiertagger.search.user");
        this.textField = this.addSelectableChild(new TextInputWidget(this.width / 2 - 100, 116, 200, 20,
                "", s -> {
        }, username, s -> s.matches("[a-zA-Z0-9_-]+"), 32));

        this.searchButton = this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("tiertagger.search"), button -> this.loadAndShowProfile())
                        .dimensions(this.width / 2 - 100, this.height / 4 + 96 + 12, 200, 20)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.close())
                        .dimensions(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20)
                        .build()
        );

        this.setInitialFocus(this.textField);
    }

    @Override
    public void tick() {
        super.tick();
        this.searchButton.active = this.textField.isValid() && !searching;
    }

    private void loadAndShowProfile() {
        String username = this.textField.getText();
        this.searching = true;
        this.searchButton.setMessage(Text.translatable("tiertagger.search.loading"));

        YggdrasilAuthenticationService service = ((MinecraftClientAccessor) MinecraftClient.getInstance()).getAuthenticationService();
        ApiServices services = ApiServices.create(service, MinecraftClient.getInstance().runDirectory);

        CompletableFuture<PlayerSkinWidget> skinFuture = fetchProfile(username, services).thenApply(p -> {
            GameProfile profile = Optional.ofNullable(services.sessionService().fetchProfile(p.getId(), true))
                    .map(ProfileResult::profile)
                    .orElseGet(() -> new GameProfile(UUID.randomUUID(), username));

            Supplier<SkinTextures> skinSupplier = MinecraftClient.getInstance().getSkinProvider().getSkinTexturesSupplier(profile);
            PlayerSkinWidget skin = new PlayerSkinWidget(60, 144, MinecraftClient.getInstance().getEntityModelLoader(), skinSupplier);
            skin.setPosition(this.width / 2 - 65, (this.height - 144) / 2);
            return skin;
        });

        TierCache.searchPlayer(username)
                .thenCombine(skinFuture, (info, skin) -> new PlayerInfoScreen(this, info, skin))
                .thenAccept(screen -> MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(screen)))
                .whenComplete((v, t) -> {
                    if (t != null) {
                        Ukutils.sendToast(Text.translatable("tiertagger.search.unknown"), null);
                    }
                    this.searching = false;
                    this.searchButton.setMessage(Text.translatable("tiertagger.search"));
                });
    }

    private CompletableFuture<GameProfile> fetchProfile(String username, ApiServices services) {
        CompletableFuture<GameProfile> future = new CompletableFuture<>();

        services.profileRepository().findProfilesByNames(new String[]{username}, new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile profile) {
                future.complete(profile);
            }

            @Override
            public void onProfileLookupFailed(String profileName, Exception exception) {
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        String string = this.textField.getText();
        this.init(client, width, height);
        this.textField.setText(string);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);
        this.textField.render(context, mouseX, mouseY, delta);
    }
}
