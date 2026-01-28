package com.multicolorshulkers.mixin;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntity.class)
public class ShulkerBoxBlockEntityMixin {

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
        ShulkerBoxBlockEntity self = (ShulkerBoxBlockEntity) (Object) this;

        MultiColorShulkers.LOGGER.info("[MIXIN] readNbt called, NBT keys: {}", nbt.getKeys());

        // Check if we already have the attachment set by Fabric
        ShulkerColors existing = self.getAttached(MultiColorShulkers.SHULKER_COLORS);
        MultiColorShulkers.LOGGER.info("[MIXIN] Existing attachment: {}", existing);
        if (existing != null && (existing.topColor() != -1 || existing.bottomColor() != -1)) {
            MultiColorShulkers.LOGGER.info("[MIXIN] Already have colors, skipping");
            return;
        }

        // Try to read colors from NBT (for items placed from colored shulkers)
        if (nbt.contains("fabric:attachments")) {
            NbtCompound attachments = nbt.getCompound("fabric:attachments");
            MultiColorShulkers.LOGGER.info("[MIXIN] Found attachments, keys: {}", attachments.getKeys());
            String key = MultiColorShulkers.MOD_ID + ":colors";
            if (attachments.contains(key)) {
                NbtCompound colorsNbt = attachments.getCompound(key);
                int topColor = colorsNbt.contains("topColor") ? colorsNbt.getInt("topColor") : -1;
                int bottomColor = colorsNbt.contains("bottomColor") ? colorsNbt.getInt("bottomColor") : -1;
                MultiColorShulkers.LOGGER.info("[MIXIN] Found colors in NBT: top={}, bottom={}", topColor, bottomColor);
                if (topColor != -1 || bottomColor != -1) {
                    ShulkerColors colors = new ShulkerColors(topColor, bottomColor);
                    self.setAttached(MultiColorShulkers.SHULKER_COLORS, colors);
                    MultiColorShulkers.LOGGER.info("[MIXIN] Restored colors!");
                }
            } else {
                MultiColorShulkers.LOGGER.info("[MIXIN] No colors key found in attachments");
            }
        } else {
            MultiColorShulkers.LOGGER.info("[MIXIN] No fabric:attachments in NBT");
        }
    }
}
