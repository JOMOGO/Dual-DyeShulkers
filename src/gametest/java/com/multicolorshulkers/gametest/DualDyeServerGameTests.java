package com.multicolorshulkers.gametest;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.multicolorshulkers.recipe.DualDyeShulkerRecipe;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Server-side game tests for the behaviors in RULES.md (persistence and crafting).
 */
public class DualDyeServerGameTests {

	private static final int RED = DyeColor.RED.getId();
	private static final int BLUE = DyeColor.BLUE.getId();

	private static ItemStack coloredShulker(int top, int bottom) {
		ItemStack stack = new ItemStack(Items.SHULKER_BOX);
		MultiColorShulkers.applyColorsToItemStack(stack, new ShulkerColors(top, bottom));
		return stack;
	}

	/** RULES 3: placing a colored shulker restores its colors to the block. */
	@GameTest
	public void placingColoredItemRestoresColors(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, Blocks.SHULKER_BOX);
		BlockPos abs = helper.absolutePos(pos);
		BlockState state = helper.getLevel().getBlockState(abs);
		state.getBlock().setPlacedBy(helper.getLevel(), abs, state, null, coloredShulker(RED, BLUE));

		ShulkerBoxBlockEntity be = helper.getBlockEntity(pos, ShulkerBoxBlockEntity.class);
		helper.assertValueEqual(be.getAttached(MultiColorShulkers.SHULKER_COLORS), new ShulkerColors(RED, BLUE), "block colors after placement");
		helper.succeed();
	}

	/** RULES 3: breaking a colored shulker drops an item that keeps the colors. */
	@GameTest
	public void breakingKeepsColorsOnDrop(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, Blocks.SHULKER_BOX);
		helper.getBlockEntity(pos, ShulkerBoxBlockEntity.class)
				.setAttached(MultiColorShulkers.SHULKER_COLORS, new ShulkerColors(RED, BLUE));

		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);

		helper.succeedWhen(() -> {
			List<ItemEntity> items = helper.getEntities(EntityTypes.ITEM, pos, 2.0);
			helper.assertTrue(items.size() == 1, "expected exactly one dropped item, got " + items.size());
			ItemStack drop = items.get(0).getItem();
			helper.assertTrue(drop.is(Items.SHULKER_BOX), "dropped item is not a shulker box: " + drop);
			helper.assertValueEqual(MultiColorShulkers.getColorsFromItemStack(drop), new ShulkerColors(RED, BLUE), "dropped item colors");
		});
	}

	/** RULES 2: crafting table spanning 3 rows; top row dye → lid, bottom row dye → base. */
	@GameTest
	public void craftingTableRowsColorTopAndBottom(GameTestHelper helper) {
		ItemStack e = ItemStack.EMPTY;
		CraftingInput input = CraftingInput.of(3, 3, List.of(
				e, new ItemStack(Items.DYE.red()), e,
				e, new ItemStack(Items.SHULKER_BOX), e,
				e, new ItemStack(Items.DYE.blue()), e));

		helper.assertTrue(DualDyeShulkerRecipe.INSTANCE.matches(input, helper.getLevel()), "recipe should match");
		ItemStack result = DualDyeShulkerRecipe.INSTANCE.assemble(input);
		helper.assertValueEqual(MultiColorShulkers.getColorsFromItemStack(result), new ShulkerColors(RED, BLUE), "crafted colors");
		helper.succeed();
	}

	/** RULES 2: 2x2 grid; dye above the shulker colors the lid only. */
	@GameTest
	public void smallGridDyeAboveColorsTop(GameTestHelper helper) {
		CraftingInput input = CraftingInput.of(1, 2, List.of(
				new ItemStack(Items.DYE.red()),
				new ItemStack(Items.SHULKER_BOX)));

		helper.assertTrue(DualDyeShulkerRecipe.INSTANCE.matches(input, helper.getLevel()), "recipe should match");
		ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(DualDyeShulkerRecipe.INSTANCE.assemble(input));
		helper.assertTrue(colors != null && colors.topColor() == RED, "top should be red, got " + colors);
		helper.succeed();
	}

	/** RULES 2: same final top and bottom color → plain vanilla single-color shulker. */
	@GameTest
	public void sameColorsGivePlainVanillaShulker(GameTestHelper helper) {
		ItemStack e = ItemStack.EMPTY;
		CraftingInput input = CraftingInput.of(3, 3, List.of(
				e, new ItemStack(Items.DYE.red()), e,
				e, new ItemStack(Items.SHULKER_BOX), e,
				e, new ItemStack(Items.DYE.red()), e));

		ItemStack result = DualDyeShulkerRecipe.INSTANCE.assemble(input);
		helper.assertTrue(result.is(Items.DYED_SHULKER_BOX.red()), "expected a red shulker box, got " + result);
		ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(result);
		helper.assertTrue(colors == null || colors.equals(ShulkerColors.DEFAULT), "expected no custom colors, got " + colors);
		helper.succeed();
	}
}
