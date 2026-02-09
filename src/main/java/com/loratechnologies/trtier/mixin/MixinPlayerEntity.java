package com.loratechnologies.trtier.mixin;

import com.loratechnologies.trtier.TRTier;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    public Text prependTier(Text original) {
        if (TRTier.getConfig().isEnabled()) {
            PlayerEntity self = (PlayerEntity) (Object) this;
            return TRTier.appendTier(self.getUuid(), self.getGameProfile().getName(), original);
        } else {
            return original;
        }
    }
}
