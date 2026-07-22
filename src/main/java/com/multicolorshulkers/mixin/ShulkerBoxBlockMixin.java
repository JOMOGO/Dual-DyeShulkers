package com.multicolorshulkers.mixin;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;", at = @At("RETURN"))
    private void onGetDrops(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof ShulkerBoxBlockEntity shulkerBox)) {
            return;
        }

        ShulkerColors colors = shulkerBox.getAttached(MultiColorShulkers.SHULKER_COLORS);
        if (colors == null || (colors.topColor() == -1 && colors.bottomColor() == -1)) {
            return;
        }

        List<ItemStack> drops = cir.getReturnValue();
        for (ItemStack stack : drops) {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                addColorsToItemStack(stack, colors);
            }
        }
    }

    private void addColorsToItemStack(ItemStack stack, ShulkerColors colors) {
        CompoundTag nbt = MultiColorShulkers.copyBlockEntityTag(stack);

        CompoundTag attachments = nbt.getCompoundOrEmpty("fabric:attachments");
        CompoundTag colorsNbt = new CompoundTag();
        colorsNbt.putInt("topColor", colors.topColor());
        colorsNbt.putInt("bottomColor", colors.bottomColor());
        attachments.put(MultiColorShulkers.MOD_ID + ":colors", colorsNbt);
        nbt.put("fabric:attachments", attachments);

        MultiColorShulkers.setBlockEntityTag(stack, nbt);
    }

    @Inject(method = "affectNeighborsAfterRemoval", at = @At("HEAD"))
    private void onAffectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston, CallbackInfo ci) {
        // A shulker box block was removed at this position. Always tell nearby clients to drop
        // their cached colors for this spot, so a later shulker placed here does not inherit them.
        // (Do NOT gate on the tracked set: Fabric's BLOCK_ENTITY_UNLOAD event has usually already
        // removed this position from tracking by the time this runs.)
        if (!movedByPiston) {
            MultiColorShulkers.clearColors(level, pos);
        }
    }
}
