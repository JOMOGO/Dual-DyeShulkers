package com.multicolorshulkers.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Cloth Config screen builder - isolated in a separate class to avoid class loading issues
 * when Cloth Config is not installed.
 *
 * Note: The two-key combo bindings are configured through the JSON config file
 * (dual-dye-shulkers.json). The GUI exposes the general toggles.
 */
public class ClothConfigScreen {

	public static Screen create(Screen parent) {
		ModConfig config = ModConfig.get();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.dual-dye-shulkers.title"))
				.setSavingRunnable(config::save);

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		// General category
		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.dual-dye-shulkers.category.general"));

		general.addEntry(entryBuilder.startBooleanToggle(
						Component.translatable("config.dual-dye-shulkers.showTooltip"),
						config.showTooltip)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.dual-dye-shulkers.showTooltip.tooltip"))
				.setSaveConsumer(value -> config.showTooltip = value)
				.build());

		general.addEntry(entryBuilder.startBooleanToggle(
						Component.translatable("config.dual-dye-shulkers.enableCrafting"),
						config.enableCrafting)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.dual-dye-shulkers.enableCrafting.tooltip"))
				.setSaveConsumer(value -> {
					config.enableCrafting = value;
					com.multicolorshulkers.MultiColorShulkers.craftingEnabled = value;
				})
				.build());

		general.addEntry(entryBuilder.startBooleanToggle(
						Component.translatable("config.dual-dye-shulkers.enableKeybinds"),
						config.enableKeybinds)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.dual-dye-shulkers.enableKeybinds.tooltip"))
				.setSaveConsumer(value -> config.enableKeybinds = value)
				.build());

		return builder.build();
	}
}
