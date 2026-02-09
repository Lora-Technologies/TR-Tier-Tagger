package com.loratechnologies.trtier.mixin;

import com.loratechnologies.trtier.TRTier;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
    @Shadow
    protected abstract void renderLabelIfPresent(T entity, Text text, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light);

    @Unique
    private boolean trtier$rendering = false;

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void trtier$modifyLabel(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
            int light, CallbackInfo ci) {
        if (trtier$rendering)
            return;

        if (entity instanceof PlayerEntity player && TRTier.getConfig().isEnabled()) {
            Text modifiedText = TRTier.appendTier(player.getUuid(), player.getGameProfile().getName(), text);
            if (modifiedText != text) {
                ci.cancel();
                trtier$rendering = true;
                try {
                    this.renderLabelIfPresent(entity, modifiedText, matrices, vertexConsumers, light);
                } finally {
                    trtier$rendering = false;
                }
            }
        }
    }
}