package com.multicolorshulkers.mixin;

import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.multicolorshulkers.client.MultiColorShulkersClient;
import com.multicolorshulkers.client.ShulkerLidModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxRenderer.class)
public class ShulkerBoxBlockEntityRendererMixin {

    @Shadow @Final private ShulkerBoxRenderer.ShulkerBoxModel model;
    @Shadow @Final private SpriteGetter sprites;

    @Unique
    private ShulkerLidModel multicolor$lidModel;

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSubmit(ShulkerBoxRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        ShulkerColors colors = MultiColorShulkersClient.getColors(state.blockPos);
        if (colors == null || (colors.topColor() == -1 && colors.bottomColor() == -1)) {
            return;
        }
        ci.cancel();
        renderWithColors(state, poseStack, collector, colors);
    }

    @Unique
    private void renderWithColors(ShulkerBoxRenderState state, PoseStack poseStack, SubmitNodeCollector collector, ShulkerColors colors) {
        DyeColor baseColor = state.color;
        DyeColor topDye = colors.topColor() == -1 ? baseColor : DyeColor.byId(colors.topColor());
        DyeColor bottomDye = colors.bottomColor() == -1 ? baseColor : DyeColor.byId(colors.bottomColor());

        SpriteId topSprite = topDye == null ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxSprite(topDye);
        SpriteId bottomSprite = bottomDye == null ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxSprite(bottomDye);

        poseStack.pushPose();
        poseStack.mulPose(ShulkerBoxRenderer.modelTransform(state.direction));

        ModelPart root = this.model.root();
        ModelPart basePart = root.getChild("base");

        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;

        // Base is static: render it directly with the bottom sprite.
        RenderType bottomRenderType = bottomSprite.renderType(this.model.renderType());
        //? if MC: >=260300 {
        /*collector.submitModelPart(basePart, poseStack, bottomRenderType, light, overlay, this.sprites.get(bottomSprite), -1, 0);

        // Lid animates with the open progress: submit it as a stateful model so the deferred
        // renderer re-applies the animation per shulker at draw time (fixes shared-lid "linking").
        collector.submitModel(lidModel(root), state.progress, poseStack, light, overlay, -1, topSprite, this.sprites, 0);

        // 26.3 moved block-breaking cracks into a separate submit; mirror vanilla with the full model.
        if (state.breakProgress != null) {
            collector.order(1).submitCrumblingOverlay(this.model, state.progress, poseStack,
                    bottomSprite.renderType(this.model.renderType()), light, overlay, -1, state.breakProgress);
        }
        *///?} else {
        collector.submitModelPart(basePart, poseStack, bottomRenderType, light, overlay, this.sprites.get(bottomSprite), -1, state.breakProgress, 0);

        // Lid animates with the open progress: submit it as a stateful model so the deferred
        // renderer re-applies the animation per shulker at draw time (fixes shared-lid "linking").
        collector.submitModel(lidModel(root), state.progress, poseStack, light, overlay, -1, topSprite, this.sprites, 0, state.breakProgress);
        //?}

        poseStack.popPose();
    }

    @Unique
    private ShulkerLidModel lidModel(ModelPart root) {
        if (this.multicolor$lidModel == null) {
            this.multicolor$lidModel = new ShulkerLidModel(root.getChild("lid"));
        }
        return this.multicolor$lidModel;
    }
}
