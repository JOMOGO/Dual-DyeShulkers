package com.multicolorshulkers.client;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.List;

public class ShulkerBoxTooltipCallback {

	public static void addTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag type, List<Component> tooltip) {
		if (!ModConfig.get().showTooltip) return;

		if (!(stack.getItem() instanceof BlockItem blockItem)) return;
		if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;

		ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(stack);
		if (colors == null) return;

		int topColor = colors.topColor();
		int bottomColor = colors.bottomColor();

		if (topColor != -1 || bottomColor != -1) {
			tooltip.add(Component.empty());
			tooltip.add(Component.literal("Custom Colors:").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

			if (topColor != -1) {
				DyeColor dyeColor = DyeColor.byId(topColor);
				String colorName = formatColorName(dyeColor.getSerializedName());
				tooltip.add(Component.literal("  Top: ").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(colorName).withStyle(getFormattingForDye(dyeColor))));
			}

			if (bottomColor != -1) {
				DyeColor dyeColor = DyeColor.byId(bottomColor);
				String colorName = formatColorName(dyeColor.getSerializedName());
				tooltip.add(Component.literal("  Bottom: ").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(colorName).withStyle(getFormattingForDye(dyeColor))));
			}
		}
	}

	private static String formatColorName(String name) {
		String[] words = name.replace('_', ' ').split(" ");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (!word.isEmpty()) {
				result.append(Character.toUpperCase(word.charAt(0)))
						.append(word.substring(1).toLowerCase())
						.append(" ");
			}
		}
		return result.toString().trim();
	}

	private static ChatFormatting getFormattingForDye(DyeColor color) {
		return switch (color) {
			case WHITE -> ChatFormatting.WHITE;
			case ORANGE -> ChatFormatting.GOLD;
			case MAGENTA, PINK -> ChatFormatting.LIGHT_PURPLE;
			case LIGHT_BLUE -> ChatFormatting.AQUA;
			case YELLOW -> ChatFormatting.YELLOW;
			case LIME -> ChatFormatting.GREEN;
			case GRAY -> ChatFormatting.DARK_GRAY;
			case LIGHT_GRAY -> ChatFormatting.GRAY;
			case CYAN -> ChatFormatting.DARK_AQUA;
			case PURPLE -> ChatFormatting.DARK_PURPLE;
			case BLUE -> ChatFormatting.BLUE;
			case BROWN -> ChatFormatting.GOLD;
			case GREEN -> ChatFormatting.DARK_GREEN;
			case RED -> ChatFormatting.RED;
			case BLACK -> ChatFormatting.DARK_GRAY;
		};
	}
}
