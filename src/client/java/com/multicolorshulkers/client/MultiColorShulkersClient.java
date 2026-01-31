package com.multicolorshulkers.client;

import com.multicolorshulkers.ColorSyncPayload;
import com.multicolorshulkers.DyeRequestPayload;
import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.DyeItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
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
		// Load config
		ModConfig.get();

		// Register packet receiver
		ClientPlayNetworking.registerGlobalReceiver(ColorSyncPayload.ID, (payload, context) -> {
			BlockPos pos = payload.pos();
			int topColor = payload.topColor();
			int bottomColor = payload.bottomColor();

			if (topColor == -1 && bottomColor == -1) {
				// Clear sync - remove from cache
				COLOR_CACHE.remove(pos);
				MultiColorShulkers.LOGGER.debug("[CLIENT] Cleared colors cache for {}", pos);
			} else {
				// Update cache with new colors
				ShulkerColors colors = new ShulkerColors(topColor, bottomColor);
				COLOR_CACHE.put(pos, colors);
				MultiColorShulkers.LOGGER.debug("[CLIENT] Received colors for {}: top={}, bottom={}",
					pos, topColor, bottomColor);
			}
		});

		// Register tooltip callback
		ItemTooltipCallback.EVENT.register(ShulkerBoxTooltipCallback::addTooltip);

        // Register custom item renderer for all shulker box items
        net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.DynamicItemRenderer renderer = (stack, mode, matrices, vertexConsumers, light, overlay) -> {
            ShulkerBoxBlockEntity blockEntity = new ShulkerBoxBlockEntity(BlockPos.ORIGIN, net.minecraft.block.Blocks.SHULKER_BOX.getDefaultState());
            
            // Set base color if it's a colored shulker box item
            if (net.minecraft.block.Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock shulkerBlock) {
                 blockEntity = new ShulkerBoxBlockEntity(BlockPos.ORIGIN, shulkerBlock.getDefaultState());
            }

            // Get custom colors
            ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(stack);
            setItemColors(colors);
            
            try {
                MinecraftClient.getInstance().getBlockEntityRenderDispatcher().renderEntity(blockEntity, matrices, vertexConsumers, light, overlay);
            } finally {
                clearItemColors();
            }
        };

        net.minecraft.item.Item[] shulkerItems = {
            net.minecraft.item.Items.SHULKER_BOX,
            net.minecraft.item.Items.WHITE_SHULKER_BOX, net.minecraft.item.Items.ORANGE_SHULKER_BOX, net.minecraft.item.Items.MAGENTA_SHULKER_BOX, net.minecraft.item.Items.LIGHT_BLUE_SHULKER_BOX,
            net.minecraft.item.Items.YELLOW_SHULKER_BOX, net.minecraft.item.Items.LIME_SHULKER_BOX, net.minecraft.item.Items.PINK_SHULKER_BOX, net.minecraft.item.Items.GRAY_SHULKER_BOX,
            net.minecraft.item.Items.LIGHT_GRAY_SHULKER_BOX, net.minecraft.item.Items.CYAN_SHULKER_BOX, net.minecraft.item.Items.PURPLE_SHULKER_BOX, net.minecraft.item.Items.BLUE_SHULKER_BOX,
            net.minecraft.item.Items.BROWN_SHULKER_BOX, net.minecraft.item.Items.GREEN_SHULKER_BOX, net.minecraft.item.Items.RED_SHULKER_BOX, net.minecraft.item.Items.BLACK_SHULKER_BOX
        };

        for (net.minecraft.item.Item item : shulkerItems) {
            net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(item, renderer);
        }

		// Register client-side use block handler for dyeing shulker boxes
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			// Only process on client side
			if (!world.isClient()) {
				return ActionResult.PASS;
			}

			var blockPos = hitResult.getBlockPos();
			var blockState = world.getBlockState(blockPos);

			// Check if it's a shulker box
			if (!(blockState.getBlock() instanceof ShulkerBoxBlock)) {
				return ActionResult.PASS;
			}

			var heldItem = player.getStackInHand(hand);

			// Check if holding a dye
			if (!(heldItem.getItem() instanceof DyeItem)) {
				return ActionResult.PASS;
			}

			// Check which binding is active to determine action
			boolean topKeyPressed = isTopColorBindingActive();
			boolean bottomKeyPressed = isBottomColorBindingActive();

			// If neither key is pressed, let vanilla handle it (open shulker)
			if (!topKeyPressed && !bottomKeyPressed) {
				return ActionResult.PASS;
			}

			// If both pressed, prefer bottom (or could pass - user decision)
			boolean colorBottom = bottomKeyPressed;

			// Send packet to server
			ClientPlayNetworking.send(new DyeRequestPayload(blockPos, colorBottom));

			// Swing hand visually
			player.swingHand(hand);

			// Return FAIL to prevent the client from sending a standard "interact block" packet
			// which would cause the shulker box to open on the server.
			return ActionResult.FAIL;
		});
	}

	public static ShulkerColors getColors(BlockPos pos) {
		return COLOR_CACHE.get(pos);
	}

	/**
	 * Check if the top color binding is currently active (both keys pressed).
	 */
	public static boolean isTopColorBindingActive() {
		KeyCombo combo = ModConfig.get().getTopCombo();
		return isKeyPressed(combo.getKey1()) && isKeyPressed(combo.getKey2());
	}

	/**
	 * Check if the bottom color binding is currently active (both keys pressed).
	 */
	public static boolean isBottomColorBindingActive() {
		KeyCombo combo = ModConfig.get().getBottomCombo();
		return isKeyPressed(combo.getKey1()) && isKeyPressed(combo.getKey2());
	}

	private static boolean isKeyPressed(InputUtil.Key key) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getWindow() == null) return false;
		long handle = client.getWindow().getHandle();

		if (key.getCategory() == InputUtil.Type.KEYSYM) {
			return InputUtil.isKeyPressed(handle, key.getCode());
		} else if (key.getCategory() == InputUtil.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
		}
		return false;
	}
}
