package com.loratechnologies.trtier.mixin;

import com.loratechnologies.trtier.TRTier;
import com.loratechnologies.trtier.config.TRTierConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListHud.class)
public class MixinPlayerListHud {
    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    @Nullable
    public Text prependTier(Text original, PlayerListEntry entry) {
        TRTierConfig config = TRTier.getConfig();
        if (config.isEnabled() && config.isPlayerList()) {
            return TRTier.appendTier(entry.getProfile().getId(), entry.getProfile().getName(), original);
        } else {
            return original;
        }
    }
}
