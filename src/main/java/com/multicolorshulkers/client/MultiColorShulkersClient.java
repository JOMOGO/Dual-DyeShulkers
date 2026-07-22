package com.multicolorshulkers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.multicolorshulkers.ColorSyncPayload;
import com.multicolorshulkers.DyeRequestPayload;
import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiColorShulkersClient implements ClientModInitializer {

	// Client-side cache of shulker colors for placed blocks
	public static final Map<BlockPos, ShulkerColors> COLOR_CACHE = new ConcurrentHashMap<>();

	// ThreadLocal for passing item colors through the render pipeline
	private static final ThreadLocal<ShulkerColors> ITEM_COLORS = new ThreadLocal<>();

	public static void setItemColors(ShulkerColors colors) {
		ITEM_COLORS.set(colors);
	}

	public static ShulkerColors getItemColors() {
		return ITEM_COLORS.get();
	}

	public static void clearItemColors() {
		ITEM_COLORS.remove();
	}

	@Override
	public void onInitializeClient() {
		// Load config and sync to common code
		ModConfig config = ModConfig.get();
		MultiColorShulkers.craftingEnabled = config.enableCrafting;

		// Register packet receiver
		ClientPlayNetworking.registerGlobalReceiver(ColorSyncPayload.ID, (payload, context) -> {
			BlockPos pos = payload.pos();
			int topColor = payload.topColor();
			int bottomColor = payload.bottomColor();

			if (topColor == -1 && bottomColor == -1) {
				COLOR_CACHE.remove(pos);
				MultiColorShulkers.LOGGER.debug("[CLIENT] Cleared colors cache for {}", pos);
			} else {
				ShulkerColors colors = new ShulkerColors(topColor, bottomColor);
				COLOR_CACHE.put(pos, colors);
				MultiColorShulkers.LOGGER.debug("[CLIENT] Received colors for {}: top={}, bottom={}",
					pos, topColor, bottomColor);
			}
		});

		// Register tooltip callback
		ItemTooltipCallback.EVENT.register(ShulkerBoxTooltipCallback::addTooltip);

		// Register client-side use block handler for dyeing shulker boxes
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			// Only process on client side
			if (!world.isClientSide()) {
				return InteractionResult.PASS;
			}

			var blockPos = hitResult.getBlockPos();
			var blockState = world.getBlockState(blockPos);

			// Check if it's a shulker box
			if (!(blockState.getBlock() instanceof ShulkerBoxBlock)) {
				return InteractionResult.PASS;
			}

			var heldItem = player.getItemInHand(hand);

			// Check if holding a dye
			if (!(heldItem.getItem() instanceof DyeItem)) {
				return InteractionResult.PASS;
			}

			// Check which binding is active to determine action
			if (!ModConfig.get().enableKeybinds) return InteractionResult.PASS;
			boolean topKeyPressed = isTopColorBindingActive();
			boolean bottomKeyPressed = isBottomColorBindingActive();

			// If neither key is pressed, let vanilla handle it (open shulker)
			if (!topKeyPressed && !bottomKeyPressed) {
				return InteractionResult.PASS;
			}

			// If both pressed, prefer bottom
			boolean colorBottom = bottomKeyPressed;

			// Send packet to server
			ClientPlayNetworking.send(new DyeRequestPayload(blockPos, colorBottom));

			// Swing hand visually
			player.swing(hand);

			// Return FAIL to prevent the client from opening the shulker box.
			return InteractionResult.FAIL;
		});
	}

	public static ShulkerColors getColors(BlockPos pos) {
		return COLOR_CACHE.get(pos);
	}

	/**
	 * Check if the top color binding is currently active (both keys pressed).
	 */
	public static boolean isTopColorBindingActive() {
		KeyCombo combo = ClientConfigHelper.getTopCombo(ModConfig.get());
		return isKeyPressed(combo.getKey1()) && isKeyPressed(combo.getKey2());
	}

	/**
	 * Check if the bottom color binding is currently active (both keys pressed).
	 */
	public static boolean isBottomColorBindingActive() {
		KeyCombo combo = ClientConfigHelper.getBottomCombo(ModConfig.get());
		return isKeyPressed(combo.getKey1()) && isKeyPressed(combo.getKey2());
	}

	private static boolean isKeyPressed(InputConstants.Key key) {
		Minecraft client = Minecraft.getInstance();
		Window window = client.getWindow();
		if (window == null) return false;

		if (key.getType() == InputConstants.Type.KEYSYM) {
			return InputConstants.isKeyDown(window, key.getValue());
		} else if (key.getType() == InputConstants.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(window.handle(), key.getValue()) == GLFW.GLFW_PRESS;
		}
		return false;
	}
}
