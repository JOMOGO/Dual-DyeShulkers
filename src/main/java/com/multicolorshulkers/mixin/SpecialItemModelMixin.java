package com.multicolorshulkers.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.multicolorshulkers.client.ShulkerColorAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpecialModelWrapper.class)
public class SpecialItemModelMixin {

    @Shadow @Final private SpecialModelRenderer<?> specialRenderer;

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdate(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                          ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed,
                          CallbackInfo ci, @Local ItemStackRenderState.LayerRenderState layer) {
        if (!(this.specialRenderer instanceof ShulkerBoxSpecialRenderer)) return;

        ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(item);
        if (colors == null || (colors.topColor() == -1 && colors.bottomColor() == -1)) return;

        // Carry colors on the layer so the renderer can pick them up at submit time,
        // and distinguish colored variants in the item render state cache.
        ((ShulkerColorAccess) (Object) layer).multicolor$setShulkerColors(colors);
        output.appendModelIdentityElement(colors);
    }
}
