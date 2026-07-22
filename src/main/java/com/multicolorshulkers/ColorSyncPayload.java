package com.multicolorshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ColorSyncPayload(BlockPos pos, int topColor, int bottomColor) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ColorSyncPayload> ID =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MultiColorShulkers.MOD_ID, "color_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ColorSyncPayload> CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, ColorSyncPayload::pos,
		ByteBufCodecs.VAR_INT, ColorSyncPayload::topColor,
		ByteBufCodecs.VAR_INT, ColorSyncPayload::bottomColor,
		ColorSyncPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
