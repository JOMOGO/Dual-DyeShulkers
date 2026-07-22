package com.multicolorshulkers.mixin;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "setPlacedBy", at = @At("RETURN"))
    public void onSetPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack, CallbackInfo ci) {
        if (world.isClientSide()) return;

        Block block = state.getBlock();
        if (block instanceof ShulkerBoxBlock) {
            ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(itemStack);
            if (colors != null && (colors.topColor() != -1 || colors.bottomColor() != -1)) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof ShulkerBoxBlockEntity shulkerBox) {
                    // Ensure colors are set on the block entity
                    ((AttachmentTarget) shulkerBox).setAttached(MultiColorShulkers.SHULKER_COLORS, colors);
                    shulkerBox.setChanged();

                    if (world instanceof ServerLevel serverWorld) {
                        // Track and sync
                        MultiColorShulkers.trackShulker(serverWorld, pos);
                        MultiColorShulkers.syncColorsToClients(serverWorld, pos, colors);
                        MultiColorShulkers.LOGGER.debug("[PLACE] Restored and synced colors via BlockMixin: top={}, bottom={}", colors.topColor(), colors.bottomColor());
                    }
                }
            }
        }
    }
}
