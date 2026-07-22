package com.multicolorshulkers.mixin;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ShulkerBoxBlockEntity.class)
public class ShulkerBoxBlockEntityMixin {

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void onLoadAdditional(ValueInput input, CallbackInfo ci) {
        ShulkerBoxBlockEntity self = (ShulkerBoxBlockEntity) (Object) this;

        // Check if we already have valid colors via Fabric's attachment system
        ShulkerColors existing = self.getAttached(MultiColorShulkers.SHULKER_COLORS);
        if (existing != null && (existing.topColor() != -1 || existing.bottomColor() != -1)) {
            return;
        }

        // Try to read colors from NBT - handles item-to-block-entity transfer.
        // Fabric stores attachments under the "fabric:attachments" key.
        Optional<ValueInput> attachmentsOpt = input.child("fabric:attachments");
        if (attachmentsOpt.isEmpty()) return;

        Optional<ValueInput> colorsOpt = attachmentsOpt.get().child(MultiColorShulkers.MOD_ID + ":colors");
        if (colorsOpt.isEmpty()) return;

        ValueInput colorsView = colorsOpt.get();
        int topColor = colorsView.getIntOr("topColor", -1);
        int bottomColor = colorsView.getIntOr("bottomColor", -1);
        if (topColor != -1 || bottomColor != -1) {
            ShulkerColors colors = new ShulkerColors(topColor, bottomColor);
            self.setAttached(MultiColorShulkers.SHULKER_COLORS, colors);
            MultiColorShulkers.LOGGER.debug("[MIXIN] Restored colors from NBT: top={}, bottom={}", topColor, bottomColor);
        }
    }
}
