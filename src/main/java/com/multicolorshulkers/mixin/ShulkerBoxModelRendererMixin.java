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
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
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

@Mixin(ShulkerBoxSpecialRenderer.class)
public class ShulkerBoxModelRendererMixin {

    @Shadow @Final private ShulkerBoxRenderer shulkerBoxRenderer;
    @Shadow @Final private float openness;
    @Shadow @Final private SpriteId sprite;

    @Unique
    private ShulkerLidModel multicolor$lidModel;

    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSubmit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay,
                          boolean hasFoil, int outlineColor, CallbackInfo ci) {
        ShulkerColors colors = MultiColorShulkersClient.getItemColors();
        if (colors == null || (colors.topColor() == -1 && colors.bottomColor() == -1)) return;
        ci.cancel();
        renderWithDualColors(poseStack, collector, light, overlay, outlineColor, colors);
    }

    @Unique
    private void renderWithDualColors(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay,
                                      int outlineColor, ShulkerColors colors) {
        SpriteId topSprite = colors.topColor() == -1 ? this.sprite : Sheets.getShulkerBoxSprite(DyeColor.byId(colors.topColor()));
        SpriteId bottomSprite = colors.bottomColor() == -1 ? this.sprite : Sheets.getShulkerBoxSprite(DyeColor.byId(colors.bottomColor()));

        ShulkerBoxRenderer.ShulkerBoxModel model = this.shulkerBoxRenderer.model;
        SpriteGetter sprites = this.shulkerBoxRenderer.sprites;

        ModelPart root = model.root();
        ModelPart basePart = root.getChild("base");

        // Base is static.
        RenderType bottomRenderType = bottomSprite.renderType(model.renderType());
        collector.submitModelPart(basePart, poseStack, bottomRenderType, light, overlay, sprites.get(bottomSprite), -1, null, outlineColor);

        // Lid: submit as a stateful model at this item's openness so the shared lid pose is
        // re-applied per instance (avoids leaking a placed shulker's animation onto item renders).
        collector.submitModel(lidModel(root), this.openness, poseStack, light, overlay, -1, topSprite, sprites, outlineColor, null);
    }

    @Unique
    private ShulkerLidModel lidModel(ModelPart root) {
        if (this.multicolor$lidModel == null) {
            this.multicolor$lidModel = new ShulkerLidModel(root.getChild("lid"));
        }
        return this.multicolor$lidModel;
    }
}
