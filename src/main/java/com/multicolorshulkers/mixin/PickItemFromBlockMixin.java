package com.multicolorshulkers.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes creative "pick block" (middle-click) on a colored shulker box copy its exact top/bottom
 * colors onto the picked item, with or without holding Ctrl.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class PickItemFromBlockMixin {

    @Shadow @Final public ServerPlayer player;

    @Inject(
        method = "handlePickItemFromBlock",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;tryPickItem(Lnet/minecraft/world/item/ItemStack;)V"),
        require = 0
    )
    private void multicolor$copyShulkerColors(ServerboundPickItemFromBlockPacket packet, CallbackInfo ci, @Local ItemStack itemStack) {
        // Only in creative (pick-block only produces new items there).
        if (!this.player.hasInfiniteMaterials()) return;

        ServerLevel level = this.player.level();
        BlockPos pos = packet.pos();
        if (!(level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)) return;
        if (!(level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBox)) return;

        ShulkerColors colors = shulkerBox.getAttached(MultiColorShulkers.SHULKER_COLORS);
        if (colors == null || (colors.topColor() == -1 && colors.bottomColor() == -1)) return;

        MultiColorShulkers.applyColorsToItemStack(itemStack, colors);
    }
}
