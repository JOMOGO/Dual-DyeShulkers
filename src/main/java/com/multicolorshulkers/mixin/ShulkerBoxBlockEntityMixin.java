package com.multicolorshulkers.mixin;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
//? if MC: <12106 {
/*import net.minecraft.registry.RegistryWrapper;
*///?} else {
import net.minecraft.storage.ReadView;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntity.class)
public class ShulkerBoxBlockEntityMixin {

    //? if MC: >=12106 {
    @Inject(method = "readData", at = @At("TAIL"))
    private void onReadData(ReadView view, CallbackInfo ci) {
        ShulkerBoxBlockEntity self = (ShulkerBoxBlockEntity) (Object) this;
    //?} else {
    /*@Inject(method = "readNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
        ShulkerBoxBlockEntity self = (ShulkerBoxBlockEntity) (Object) this;
    *///?}

        // Check if we already have valid colors via Fabric's attachment system
        ShulkerColors existing = self.getAttached(MultiColorShulkers.SHULKER_COLORS);
        if (existing != null && (existing.topColor() != -1 || existing.bottomColor() != -1)) {
            return;
        }

        // Try to read colors from NBT - handles item-to-block-entity transfer
        // Fabric stores attachments under "fabric:attachments" key
        //? if MC: >=12106 {
        ReadView attachmentsView = view.getReadView("fabric:attachments");
        String key = MultiColorShulkers.MOD_ID + ":colors";
        ReadView colorsView = attachmentsView.getReadView(key);
        int topColor = colorsView.getInt("topColor", -1);
        int bottomColor = colorsView.getInt("bottomColor", -1);
        if (topColor != -1 || bottomColor != -1) {
            ShulkerColors colors = new ShulkerColors(topColor, bottomColor);
            self.setAttached(MultiColorShulkers.SHULKER_COLORS, colors);
            MultiColorShulkers.LOGGER.debug("[MIXIN] Restored colors from NBT: top={}, bottom={}", topColor, bottomColor);
        }
        //?} else if MC: >=12105 {
        /*if (nbt.contains("fabric:attachments")) {
            NbtCompound attachments = nbt.getCompoundOrEmpty("fabric:attachments");
            String key = MultiColorShulkers.MOD_ID + ":colors";
            if (attachments.contains(key)) {
                NbtCompound colorsNbt = attachments.getCompoundOrEmpty(key);
                int topColor = colorsNbt.getInt("topColor", -1);
                int bottomColor = colorsNbt.getInt("bottomColor", -1);
                if (topColor != -1 || bottomColor != -1) {
                    ShulkerColors colors = new ShulkerColors(topColor, bottomColor);
                    self.setAttached(MultiColorShulkers.SHULKER_COLORS, colors);
                    MultiColorShulkers.LOGGER.debug("[MIXIN] Restored colors from NBT: top={}, bottom={}", topColor, bottomColor);
                }
            }
        }
        *///?} else {
        /*if (nbt.contains("fabric:attachments", NbtElement.COMPOUND_TYPE)) {
            NbtCompound attachments = nbt.getCompound("fabric:attachments");
            String key = MultiColorShulkers.MOD_ID + ":colors";
            if (attachments.contains(key, NbtElement.COMPOUND_TYPE)) {
                NbtCompound colorsNbt = attachments.getCompound(key);
                int topColor = colorsNbt.contains("topColor", NbtElement.INT_TYPE) ? colorsNbt.getInt("topColor") : -1;
                int bottomColor = colorsNbt.contains("bottomColor", NbtElement.INT_TYPE) ? colorsNbt.getInt("bottomColor") : -1;
                if (topColor != -1 || bottomColor != -1) {
                    ShulkerColors colors = new ShulkerColors(topColor, bottomColor);
                    self.setAttached(MultiColorShulkers.SHULKER_COLORS, colors);
                    MultiColorShulkers.LOGGER.debug("[MIXIN] Restored colors from NBT: top={}, bottom={}", topColor, bottomColor);
                }
            }
        }
        *///?}
    }
}
