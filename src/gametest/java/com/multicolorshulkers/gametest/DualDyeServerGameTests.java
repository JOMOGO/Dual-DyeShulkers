package com.multicolorshulkers.gametest;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.multicolorshulkers.recipe.DualDyeShulkerRecipe;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Server-side game tests for the behaviors in RULES.md (persistence and crafting).
 */
public class DualDyeServerGameTests {

	private static final int RED = DyeColor.RED.getIndex();
	private static final int BLUE = DyeColor.BLUE.getIndex();
	private static final DualDyeShulkerRecipe RECIPE = new DualDyeShulkerRecipe(CraftingRecipeCategory.MISC);

	/** Builds a shulker item carrying custom colors, in the same format the mod writes. */
	static ItemStack coloredShulker(int top, int bottom) {
		NbtCompound colorNbt = new NbtCompound();
		colorNbt.putInt("topColor", top);
		colorNbt.putInt("bottomColor", bottom);
		NbtCompound attachments = new NbtCompound();
		attachments.put(MultiColorShulkers.MOD_ID + ":colors", colorNbt);
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", "minecraft:shulker_box");
		nbt.put("fabric:attachments", attachments);

		ItemStack stack = new ItemStack(Items.SHULKER_BOX);
		stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(nbt));
		return stack;
	}

	private static ItemStack craft(TestContext context, CraftingRecipeInput input) {
		return RECIPE.craft(input, context.getWorld().getRegistryManager());
	}

	/** RULES 3: placing a colored shulker restores its colors to the block. */
	@GameTest
	public void placingColoredItemRestoresColors(TestContext context) {
		BlockPos pos = new BlockPos(1, 1, 1);
		context.setBlockState(pos, Blocks.SHULKER_BOX);
		BlockPos abs = context.getAbsolutePos(pos);
		BlockState state = context.getWorld().getBlockState(abs);
		state.getBlock().onPlaced(context.getWorld(), abs, state, null, coloredShulker(RED, BLUE));

		ShulkerBoxBlockEntity be = context.getBlockEntity(pos, ShulkerBoxBlockEntity.class);
		context.assertEquals(be.getAttached(MultiColorShulkers.SHULKER_COLORS), new ShulkerColors(RED, BLUE), Text.literal("block colors after placement"));
		context.complete();
	}

	/** RULES 3: breaking a colored shulker drops an item that keeps the colors. */
	@GameTest
	public void breakingKeepsColorsOnDrop(TestContext context) {
		BlockPos pos = new BlockPos(1, 1, 1);
		context.setBlockState(pos, Blocks.SHULKER_BOX);
		context.getBlockEntity(pos, ShulkerBoxBlockEntity.class)
				.setAttached(MultiColorShulkers.SHULKER_COLORS, new ShulkerColors(RED, BLUE));

		context.getWorld().breakBlock(context.getAbsolutePos(pos), true);

		context.addInstantFinalTask(() -> {
			List<ItemEntity> items = context.getEntities(EntityType.ITEM);
			context.assertTrue(items.size() == 1, Text.literal("expected exactly one dropped item, got " + items.size()));
			ItemStack drop = items.get(0).getStack();
			context.assertTrue(drop.isOf(Items.SHULKER_BOX), Text.literal("dropped item is not a shulker box: " + drop));
			context.assertEquals(MultiColorShulkers.getColorsFromItemStack(drop), new ShulkerColors(RED, BLUE), Text.literal("dropped item colors"));
		});
	}

	/** RULES 2: crafting table spanning 3 rows; top row dye → lid, bottom row dye → base. */
	@GameTest
	public void craftingTableRowsColorTopAndBottom(TestContext context) {
		ItemStack e = ItemStack.EMPTY;
		CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, List.of(
				e, new ItemStack(Items.RED_DYE), e,
				e, new ItemStack(Items.SHULKER_BOX), e,
				e, new ItemStack(Items.BLUE_DYE), e));

		context.assertTrue(RECIPE.matches(input, context.getWorld()), Text.literal("recipe should match"));
		context.assertEquals(MultiColorShulkers.getColorsFromItemStack(craft(context, input)), new ShulkerColors(RED, BLUE), Text.literal("crafted colors"));
		context.complete();
	}

	/** RULES 2: 2x2 grid; dye above the shulker colors the lid only. */
	@GameTest
	public void smallGridDyeAboveColorsTop(TestContext context) {
		CraftingRecipeInput input = CraftingRecipeInput.create(1, 2, List.of(
				new ItemStack(Items.RED_DYE),
				new ItemStack(Items.SHULKER_BOX)));

		context.assertTrue(RECIPE.matches(input, context.getWorld()), Text.literal("recipe should match"));
		ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(craft(context, input));
		context.assertTrue(colors != null && colors.topColor() == RED, Text.literal("top should be red, got " + colors));
		context.complete();
	}

	/** RULES 2: same final top and bottom color → plain vanilla single-color shulker. */
	@GameTest
	public void sameColorsGivePlainVanillaShulker(TestContext context) {
		ItemStack e = ItemStack.EMPTY;
		CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, List.of(
				e, new ItemStack(Items.RED_DYE), e,
				e, new ItemStack(Items.SHULKER_BOX), e,
				e, new ItemStack(Items.RED_DYE), e));

		ItemStack result = craft(context, input);
		context.assertTrue(result.isOf(Items.RED_SHULKER_BOX), Text.literal("expected a red shulker box, got " + result));
		ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(result);
		context.assertTrue(colors == null || colors.equals(ShulkerColors.DEFAULT), Text.literal("expected no custom colors, got " + colors));
		context.complete();
	}

	/** RULES 2: contents carry through, also when the result becomes a plain single-color shulker. */
	@GameTest
	public void sameColorCraftKeepsContents(TestContext context) {
		ItemStack box = coloredShulker(RED, BLUE);
		box.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(List.of(new ItemStack(Items.DIAMOND, 5))));
		ItemStack e = ItemStack.EMPTY;
		CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, List.of(
				e, new ItemStack(Items.GREEN_DYE), e,
				e, box, e,
				e, new ItemStack(Items.GREEN_DYE), e));

		ItemStack result = craft(context, input);
		context.assertTrue(result.isOf(Items.GREEN_SHULKER_BOX), Text.literal("expected a green shulker box, got " + result));
		ContainerComponent contents = result.get(DataComponentTypes.CONTAINER);
		context.assertTrue(contents != null && contents.iterateNonEmpty().iterator().hasNext(), Text.literal("shulker contents were lost"));
		context.complete();
	}
}
