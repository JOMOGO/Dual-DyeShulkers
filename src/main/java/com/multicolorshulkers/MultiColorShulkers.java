package com.multicolorshulkers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
//? if MC: >=260300 {
/*import net.minecraft.world.item.component.SwingAnimation;
*///?}
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MultiColorShulkers implements ModInitializer {
	public static final String MOD_ID = "multicolor-shulkers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Config value accessible from common code (set by client, defaults to true for dedicated servers)
	public static boolean craftingEnabled = true;

	public record ShulkerColors(int topColor, int bottomColor) {
		public static final Codec<ShulkerColors> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
				Codec.INT.fieldOf("topColor").forGetter(ShulkerColors::topColor),
				Codec.INT.fieldOf("bottomColor").forGetter(ShulkerColors::bottomColor)
			).apply(instance, ShulkerColors::new)
		);

		public static final ShulkerColors DEFAULT = new ShulkerColors(-1, -1);

		public ShulkerColors withTopColor(int color) {
			return new ShulkerColors(color, this.bottomColor);
		}

		public ShulkerColors withBottomColor(int color) {
			return new ShulkerColors(this.topColor, color);
		}
	}

	public static final RecipeSerializer<com.multicolorshulkers.recipe.DualDyeShulkerRecipe> DUAL_DYE_SERIALIZER =
		com.multicolorshulkers.recipe.DualDyeShulkerRecipe.SERIALIZER;

	public static final AttachmentType<ShulkerColors> SHULKER_COLORS = AttachmentRegistry.createPersistent(
		Identifier.fromNamespaceAndPath(MOD_ID, "colors"),
		ShulkerColors.CODEC
	);

	// Server-side tracking of colored shulker positions per dimension
	private static final Map<ResourceKey<Level>, Set<BlockPos>> COLORED_SHULKERS = new ConcurrentHashMap<>();

	// Players waiting for color sync (processed next tick to avoid blocking join)
	private static final Set<ServerPlayer> PENDING_SYNCS = ConcurrentHashMap.newKeySet();

	private static Set<BlockPos> getColoredShulkers(ResourceKey<Level> dimension) {
		return COLORED_SHULKERS.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet());
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Dual-Dye Shulkers initialized!");

		// Load config and sync craftingEnabled (works on both client and dedicated server)
		try {
			com.multicolorshulkers.client.ModConfig config = com.multicolorshulkers.client.ModConfig.get();
			craftingEnabled = config.enableCrafting;
		} catch (Exception e) {
			LOGGER.warn("Could not load config, using defaults: {}", e.getMessage());
			craftingEnabled = true;
		}

		LOGGER.debug("Registering recipe serializer: {}:dual_dye", MOD_ID);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath(MOD_ID, "dual_dye"), DUAL_DYE_SERIALIZER);

		// Register packets
		PayloadTypeRegistry.clientboundPlay().register(ColorSyncPayload.ID, ColorSyncPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DyeRequestPayload.ID, DyeRequestPayload.CODEC);

		// Handle dye request from client
		ServerPlayNetworking.registerGlobalReceiver(DyeRequestPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			ServerLevel world = player.level();
			BlockPos pos = payload.pos();
			boolean colorBottom = payload.colorBottom();

			// Verify player is close enough to the block
			if (!player.blockPosition().closerThan(pos, 6)) {
				return;
			}

			// Verify block is a shulker box
			if (!(world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)) {
				return;
			}

			if (!(world.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBox)) {
				return;
			}

			// Get the dye from player's hand
			ItemStack mainHand = player.getMainHandItem();
			ItemStack offHand = player.getOffhandItem();
			ItemStack heldItem = null;
			InteractionHand hand = null;

			if (mainHand.getItem() instanceof DyeItem) {
				heldItem = mainHand;
				hand = InteractionHand.MAIN_HAND;
			} else if (offHand.getItem() instanceof DyeItem) {
				heldItem = offHand;
				hand = InteractionHand.OFF_HAND;
			}

			if (heldItem == null || !(heldItem.getItem() instanceof DyeItem)) {
				return;
			}

			DyeColor dyeColor = heldItem.get(DataComponents.DYE);
			if (dyeColor == null) {
				return;
			}
			int dyeColorId = dyeColor.getId();

			// Get current colors or create default
			ShulkerColors currentColors = shulkerBox.getAttachedOrCreate(SHULKER_COLORS, () -> ShulkerColors.DEFAULT);
			ShulkerColors newColors;

			if (colorBottom) {
				newColors = currentColors.withBottomColor(dyeColorId);
				LOGGER.debug("[DYE] Setting bottom color to {} ({})", dyeColor.getSerializedName(), dyeColorId);
			} else {
				newColors = currentColors.withTopColor(dyeColorId);
				LOGGER.debug("[DYE] Setting top color to {} ({})", dyeColor.getSerializedName(), dyeColorId);
			}

			// Set the new colors
			shulkerBox.setAttached(SHULKER_COLORS, newColors);
			shulkerBox.setChanged();

			// Track and sync to all nearby players
			getColoredShulkers(world.dimension()).add(pos);
			syncColorsToClients(world, pos, newColors);

			// Consume dye in survival
			if (!player.getAbilities().instabuild) {
				heldItem.shrink(1);
			}

			// Swing arm
			//? if MC: >=260300 {
			/*player.swing(hand, SwingAnimation.DEFAULT, true);
			*///?} else {
			player.swing(hand, true);
			//?}
		});

		// Register cauldron behavior to wash off custom colors from shulker box items
		registerCauldronBehavior();

		// Track colored shulker positions when block entities load
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
			if (!(blockEntity instanceof ShulkerBoxBlockEntity shulkerBox)) return;

			// Check colors directly - attachments are already loaded at this point
			ShulkerColors colors = shulkerBox.getAttached(SHULKER_COLORS);
			BlockPos pos = blockEntity.getBlockPos();

			if (colors != null && (colors.topColor() != -1 || colors.bottomColor() != -1)) {
				// Has custom colors - track it
				getColoredShulkers(world.dimension()).add(pos);
			}
		});

		// Remove from tracking when shulker box is unloaded/broken
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
			if (!(blockEntity instanceof ShulkerBoxBlockEntity)) return;

			// Just remove from tracking - client cache will be cleared naturally or on rejoin
			BlockPos pos = blockEntity.getBlockPos();
			getColoredShulkers(world.dimension()).remove(pos);
		});

		// Sync all nearby colored shulkers when a player joins (delayed to avoid blocking join)
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			// Store player reference for delayed sync
			PENDING_SYNCS.add(player);
		});

		// Process pending syncs each tick (safer than during join)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING_SYNCS.isEmpty()) return;

			for (ServerPlayer player : PENDING_SYNCS) {
				if (player.hasDisconnected()) continue;

				ServerLevel world = player.level();
				BlockPos playerPos = player.blockPosition();
				int radius = 64;

				Set<BlockPos> coloredPositions = getColoredShulkers(world.dimension());
				for (BlockPos pos : coloredPositions) {
					if (playerPos.closerThan(pos, radius)) {
						if (world.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBox) {
							ShulkerColors colors = shulkerBox.getAttached(SHULKER_COLORS);
							if (colors != null && (colors.topColor() != -1 || colors.bottomColor() != -1)) {
								ColorSyncPayload payload = new ColorSyncPayload(pos, colors.topColor(), colors.bottomColor());
								ServerPlayNetworking.send(player, payload);
								LOGGER.debug("[JOIN] Synced colors for {} to {}", pos, player.getName().getString());
							}
						}
					}
				}
			}
			PENDING_SYNCS.clear();
		});
	}


	public static void syncColorsToClients(ServerLevel world, BlockPos pos, ShulkerColors colors) {
		ColorSyncPayload payload = new ColorSyncPayload(pos, colors.topColor(), colors.bottomColor());

		// Send to all players tracking this block
		for (ServerPlayer player : world.players()) {
			if (player.blockPosition().closerThan(pos, 64)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	public static void trackShulker(ServerLevel world, BlockPos pos) {
		getColoredShulkers(world.dimension()).add(pos);
	}

	public static boolean isColored(ServerLevel world, BlockPos pos) {
		return getColoredShulkers(world.dimension()).contains(pos);
	}

	public static void clearColors(ServerLevel world, BlockPos pos) {
		// Remove from tracking
		getColoredShulkers(world.dimension()).remove(pos);

		// Send clear packet (colors -1, -1) to nearby clients
		ColorSyncPayload payload = new ColorSyncPayload(pos, -1, -1);
		for (ServerPlayer player : world.players()) {
			if (player.blockPosition().closerThan(pos, 64)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
		LOGGER.debug("[SERVER] Cleared colors for {}", pos);
	}

	private void registerCauldronBehavior() {
		// Register for all shulker box items (undyed + 16 dyed colors)
		java.util.List<Item> shulkerItems = new java.util.ArrayList<>();
		shulkerItems.add(Items.SHULKER_BOX);
		shulkerItems.addAll(Items.DYED_SHULKER_BOX.asList());

		for (Item shulkerItem : shulkerItems) {
			// Get the original vanilla behavior (if any) to chain to
			CauldronInteraction originalBehavior = CauldronInteractions.WATER.items.get(shulkerItem);

			CauldronInteraction washShulkerColors = (state, level, pos, player, hand, stack) -> {
				// Check if the item has our custom colors stored in block entity data
				ShulkerColors colors = getColorsFromItemStack(stack);

				if (colors != null && (colors.topColor() != -1 || colors.bottomColor() != -1)) {
					// We have custom colors - remove them
					if (!level.isClientSide()) {
						LOGGER.debug("[CAULDRON] Removing custom colors from item");
						removeColorsFromItemStack(stack);

						// Decrement cauldron water level
						LayeredCauldronBlock.lowerFillLevel(state, level, pos);

						// Stats
						player.awardStat(Stats.CLEAN_SHULKER_BOX);
					}
					return InteractionResult.SUCCESS;
				}

				// No custom colors - delegate to original vanilla behavior
				if (originalBehavior != null) {
					return originalBehavior.interact(state, level, pos, player, hand, stack);
				}
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			};

			CauldronInteractions.WATER.items.put(shulkerItem, washShulkerColors);
		}
	}

	/**
	 * Returns a mutable copy of an ItemStack's stored block entity data (without the "id" field),
	 * or an empty compound if the stack has none.
	 */
	public static CompoundTag copyBlockEntityTag(ItemStack stack) {
		TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		return data == null ? new CompoundTag() : data.copyTagWithoutId();
	}

	/**
	 * Writes block entity data back onto an ItemStack as a shulker box block entity component.
	 */
	public static void setBlockEntityTag(ItemStack stack, CompoundTag tag) {
		BlockEntityType<?> type = BlockEntityTypes.SHULKER_BOX;
		stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(type, tag));
	}

	/**
	 * Store the given colors into an ItemStack's block entity data (under Fabric's attachment key),
	 * preserving any existing block entity data (e.g. contents).
	 */
	public static void applyColorsToItemStack(ItemStack stack, ShulkerColors colors) {
		CompoundTag nbt = copyBlockEntityTag(stack);
		CompoundTag attachments = nbt.getCompoundOrEmpty("fabric:attachments");
		CompoundTag colorNbt = new CompoundTag();
		colorNbt.putInt("topColor", colors.topColor());
		colorNbt.putInt("bottomColor", colors.bottomColor());
		attachments.put(MOD_ID + ":colors", colorNbt);
		nbt.put("fabric:attachments", attachments);
		setBlockEntityTag(stack, nbt);
	}

	/**
	 * Get colors from an ItemStack's stored block entity data.
	 */
	public static ShulkerColors getColorsFromItemStack(ItemStack stack) {
		TypedEntityData<BlockEntityType<?>> beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (beData == null) {
			return null;
		}

		CompoundTag nbt = beData.copyTagWithoutId();
		if (!nbt.contains("fabric:attachments")) {
			return null;
		}

		CompoundTag attachments = nbt.getCompoundOrEmpty("fabric:attachments");
		String key = MOD_ID + ":colors";
		if (!attachments.contains(key)) {
			return null;
		}

		CompoundTag colorsNbt = attachments.getCompoundOrEmpty(key);
		int topColor = colorsNbt.getIntOr("topColor", -1);
		int bottomColor = colorsNbt.getIntOr("bottomColor", -1);

		if (topColor == -1 && bottomColor == -1) return null;
		return new ShulkerColors(topColor, bottomColor);
	}

	/**
	 * Remove colors from an ItemStack's stored block entity data.
	 */
	public static void removeColorsFromItemStack(ItemStack stack) {
		TypedEntityData<BlockEntityType<?>> beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (beData == null) return;

		CompoundTag nbt = beData.copyTagWithoutId();
		if (!nbt.contains("fabric:attachments")) return;
		CompoundTag attachments = nbt.getCompoundOrEmpty("fabric:attachments");

		String key = MOD_ID + ":colors";
		if (attachments.contains(key)) {
			attachments.remove(key);
			if (attachments.isEmpty()) {
				nbt.remove("fabric:attachments");
			} else {
				nbt.put("fabric:attachments", attachments);
			}
			setBlockEntityTag(stack, nbt);
			LOGGER.debug("[CAULDRON] Removed custom colors from item");
		}
	}
}
