package com.multicolorshulkers.mixin;

//? if MC: >=12106 {
import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.multicolorshulkers.client.MultiColorShulkersClient;
import com.multicolorshulkers.client.ShulkerColorAccess;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderState.LayerRenderState.class)
public class LayerRenderStateMixin implements ShulkerColorAccess {

    @Unique
    private ShulkerColors multicolor$shulkerColors;

    @Override
    public void multicolor$setShulkerColors(ShulkerColors colors) {
        this.multicolor$shulkerColors = colors;
    }

    @Override
    public ShulkerColors multicolor$getShulkerColors() {
        return this.multicolor$shulkerColors;
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/model/special/SpecialModelRenderer;render(Ljava/lang/Object;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IIZ)V"))
    private void beforeSpecialRender(CallbackInfo ci) {
        if (this.multicolor$shulkerColors != null) {
            MultiColorShulkersClient.setItemColors(this.multicolor$shulkerColors);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(CallbackInfo ci) {
        if (this.multicolor$shulkerColors != null) {
            MultiColorShulkersClient.clearItemColors();
        }
    }
}
//?} else if MC: >=12102 {
/*import com.multicolorshulkers.client.ShulkerColorAccess;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;

// Stub mixin for 1.21.2-1.21.5 - LayerRenderState color passing not needed
@Mixin(ItemRenderState.class)
public class LayerRenderStateMixin implements ShulkerColorAccess {}
*///?} else {
/*import com.multicolorshulkers.client.ShulkerColorAccess;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;

// Stub mixin for 1.21.1 - ItemRenderState doesn't exist
@Mixin(ItemRenderer.class)
public class LayerRenderStateMixin implements ShulkerColorAccess {}
*///?}
