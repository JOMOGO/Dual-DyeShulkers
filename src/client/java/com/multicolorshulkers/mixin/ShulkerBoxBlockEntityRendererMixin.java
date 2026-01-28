package com.multicolorshulkers.mixin;

import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.multicolorshulkers.client.MultiColorShulkersClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.ShulkerBoxBlockEntityRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntityRenderer.class)
public class ShulkerBoxBlockEntityRendererMixin {

    @Shadow
    @Final
    public ShulkerBoxBlockEntityRenderer.ShulkerBoxBlockModel model;

    // For placed blocks - check COLOR_CACHE
    @Inject(method = "render(Lnet/minecraft/block/entity/ShulkerBoxBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onRender(ShulkerBoxBlockEntity shulkerBox, float tickDelta, MatrixStack matrices,
                          VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        ShulkerColors colors = MultiColorShulkersClient.getColors(shulkerBox.getPos());

        if (colors == null || (colors.topColor() == -1 && colors.bottomColor() == -1)) {
            return;
        }

        ci.cancel();
        renderWithColors(shulkerBox, tickDelta, matrices, vertexConsumers, light, overlay, colors);
    }

    private void renderWithColors(ShulkerBoxBlockEntity shulkerBox, float tickDelta, MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers, int light, int overlay, ShulkerColors colors) {
        Direction direction = Direction.UP;
        BlockState blockState = shulkerBox.getCachedState();
        if (blockState.contains(ShulkerBoxBlock.FACING)) {
            direction = blockState.get(ShulkerBoxBlock.FACING);
        }

        // Get the base color of the shulker (for fallback when only one color is set)
        DyeColor baseColor = shulkerBox.getColor();

        // Get the actual DyeColor for top and bottom
        DyeColor topDyeColor = getDyeColorFromId(colors.topColor(), baseColor);
        DyeColor bottomDyeColor = getDyeColorFromId(colors.bottomColor(), baseColor);

        // Use the actual vanilla colored textures instead of tinting
        SpriteIdentifier topTexture = getShulkerTexture(topDyeColor);
        SpriteIdentifier bottomTexture = getShulkerTexture(bottomDyeColor);

        float animationProgress = shulkerBox.getAnimationProgress(tickDelta);

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(0.9995F, 0.9995F, 0.9995F);
        matrices.multiply(direction.getRotationQuaternion());
        matrices.scale(1.0F, -1.0F, -1.0F);
        matrices.translate(0.0, -1.0, 0.0);

        this.model.animateLid(animationProgress);

        ModelPart lidPart = this.model.lid;
        ModelPart rootPart = this.model.root;

        // Get vertex consumers for each colored texture
        VertexConsumer topVertexConsumer = topTexture.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutoutNoCull);
        VertexConsumer bottomVertexConsumer = bottomTexture.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutoutNoCull);

        ModelPart basePart = null;
        try {
            basePart = rootPart.getChild("base");
        } catch (Exception e) {
            // Base part not found
        }

        // Render with white tint (0xFFFFFFFF) since textures are already colored
        if (basePart != null) {
            basePart.render(matrices, bottomVertexConsumer, light, overlay, 0xFFFFFFFF);
            lidPart.render(matrices, topVertexConsumer, light, overlay, 0xFFFFFFFF);
        } else {
            boolean lidVisible = lidPart.visible;
            lidPart.visible = false;
            rootPart.render(matrices, bottomVertexConsumer, light, overlay, 0xFFFFFFFF);
            lidPart.visible = lidVisible;
            lidPart.render(matrices, topVertexConsumer, light, overlay, 0xFFFFFFFF);
        }

        matrices.pop();
    }

    private DyeColor getDyeColorFromId(int colorId, DyeColor fallbackColor) {
        if (colorId == -1) {
            return fallbackColor; // Can be null for undyed shulker
        }
        return DyeColor.byId(colorId);
    }

    private SpriteIdentifier getShulkerTexture(DyeColor color) {
        if (color == null) {
            // Undyed shulker box
            return TexturedRenderLayers.SHULKER_TEXTURE_ID;
        }
        // Use the colored shulker texture list indexed by color ID
        return TexturedRenderLayers.COLORED_SHULKER_BOXES_TEXTURES.get(color.getId());
    }
}
