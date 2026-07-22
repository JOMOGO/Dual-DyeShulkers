package com.multicolorshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet to request dyeing a shulker box.
 * This allows the client to decide whether to color the top or bottom based on keybind state.
 */
public record DyeRequestPayload(BlockPos pos, boolean colorBottom) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DyeRequestPayload> ID =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MultiColorShulkers.MOD_ID, "dye_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DyeRequestPayload> CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, DyeRequestPayload::pos,
		ByteBufCodecs.BOOL, DyeRequestPayload::colorBottom,
		DyeRequestPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
