package com.multicolorshulkers.mixin;

import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.multicolorshulkers.client.MultiColorShulkersClient;
import com.multicolorshulkers.client.ShulkerColorAccess;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries custom shulker colors on a layer render state, and exposes them to the
 * special-model renderer via a ThreadLocal for the duration of the layer's submit call.
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
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

    @Inject(method = "clear", at = @At("HEAD"))
    private void onClear(CallbackInfo ci) {
        this.multicolor$shulkerColors = null;
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void beforeSubmit(CallbackInfo ci) {
        if (this.multicolor$shulkerColors != null) {
            MultiColorShulkersClient.setItemColors(this.multicolor$shulkerColors);
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void afterSubmit(CallbackInfo ci) {
        if (this.multicolor$shulkerColors != null) {
            MultiColorShulkersClient.clearItemColors();
        }
    }
}
