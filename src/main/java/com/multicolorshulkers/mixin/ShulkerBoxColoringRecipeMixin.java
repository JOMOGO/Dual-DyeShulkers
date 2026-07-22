package com.multicolorshulkers.mixin;

// In 1.21.2+, ShulkerBoxColoringRecipe was replaced by the data-driven TransmuteRecipe.
// We block TransmuteRecipe for shulker+dye combinations so our custom recipe handles them.

import com.multicolorshulkers.MultiColorShulkers;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransmuteRecipe.class)
public class ShulkerBoxColoringRecipeMixin {

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    private void blockVanillaShulkerDye(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!MultiColorShulkers.craftingEnabled) return;

        boolean hasShulker = false;
        boolean hasDye = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock) {
                hasShulker = true;
            } else if (stack.getItem() instanceof DyeItem) {
                hasDye = true;
            }
        }

        // If this is a shulker + dye recipe, block it so our custom recipe handles it
        if (hasShulker && hasDye) {
            MultiColorShulkers.LOGGER.info("[MIXIN] Blocking TransmuteRecipe for shulker+dye combination");
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void stripCustomColorsFromOutput(CraftingInput input, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (!result.isEmpty() && Block.byItem(result.getItem()) instanceof ShulkerBoxBlock) {
            MultiColorShulkers.removeColorsFromItemStack(result);
        }
    }
}
