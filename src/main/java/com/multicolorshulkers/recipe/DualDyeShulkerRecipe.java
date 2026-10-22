package com.multicolorshulkers.recipe;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;

public class DualDyeShulkerRecipe extends CustomRecipe {

    public static final DualDyeShulkerRecipe INSTANCE = new DualDyeShulkerRecipe();
    public static final MapCodec<DualDyeShulkerRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, DualDyeShulkerRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<DualDyeShulkerRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return matchesShared(input.width(), input.height(), toList(input));
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return craftShared(input.width(), input.height(), toList(input));
    }

    private static List<ItemStack> toList(CraftingInput input) {
        List<ItemStack> stacks = new ArrayList<>(input.size());
        for (int i = 0; i < input.size(); i++) {
            stacks.add(input.getItem(i));
        }
        return stacks;
    }

    private boolean matchesShared(int width, int height, List<ItemStack> stacks) {
        if (!MultiColorShulkers.craftingEnabled) return false;

        ItemStack shulkerStack = ItemStack.EMPTY;
        int dyeCount = 0;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            if (Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock) {
                if (!shulkerStack.isEmpty()) {
                    return false; // Multiple shulkers
                }
                shulkerStack = stack;
            } else if (stack.getItem() instanceof DyeItem) {
                dyeCount++;
            } else {
                return false; // Non-shulker/non-dye item
            }
        }
        return !shulkerStack.isEmpty() && dyeCount > 0;
    }

    private ItemStack craftShared(int width, int height, List<ItemStack> stacks) {
        ItemStack shulkerStack = ItemStack.EMPTY;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock) {
                shulkerStack = stack;
                break;
            }
        }

        if (shulkerStack.isEmpty()) return ItemStack.EMPTY;

        ShulkerColors currentColors = MultiColorShulkers.getColorsFromItemStack(shulkerStack);
        if (currentColors == null) currentColors = ShulkerColors.DEFAULT;

        int baseColorId = getBaseColor(shulkerStack);
        int resolvedTop = currentColors.topColor();
        int resolvedBottom = currentColors.bottomColor();

        if (resolvedTop == -1) resolvedTop = baseColorId;
        if (resolvedBottom == -1) resolvedBottom = baseColorId;

        int newTopColor = -1;
        int newBottomColor = -1;
        int vanillaColor = -1;

        int size = stacks.size();

        if (height == 3) {
            // CRAFTING TABLE MODE - items span all 3 rows
            for (int i = 0; i < size; i++) {
                ItemStack stack = stacks.get(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof DyeItem)) continue;

                int dyeRow = i / width;
                int colorId = colorIdOf(stack);
                if (colorId == -1) continue;

                if (dyeRow == 0) {
                    newTopColor = colorId;
                } else if (dyeRow == 2) {
                    newBottomColor = colorId;
                } else {
                    vanillaColor = colorId;
                }
            }
        } else {
            // RELATIVE MODE - hand crafting (2x2) or crafting table when items don't span 3 rows
            int shulkerRow = -1;
            for (int i = 0; i < size; i++) {
                ItemStack stack = stacks.get(i);
                if (!stack.isEmpty() && Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock) {
                    shulkerRow = i / width;
                    break;
                }
            }

            for (int i = 0; i < size; i++) {
                ItemStack stack = stacks.get(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof DyeItem)) continue;

                int dyeRow = i / width;
                int colorId = colorIdOf(stack);
                if (colorId == -1) continue;

                if (dyeRow < shulkerRow) {
                    newTopColor = colorId;
                } else if (dyeRow > shulkerRow) {
                    newBottomColor = colorId;
                } else {
                    vanillaColor = colorId;
                }
            }
        }

        // Apply vanilla color first (colors whole box)
        if (vanillaColor != -1) {
            resolvedTop = vanillaColor;
            resolvedBottom = vanillaColor;
        }

        // Then apply position-specific colors (can override vanilla)
        if (newTopColor != -1) resolvedTop = newTopColor;
        if (newBottomColor != -1) resolvedBottom = newBottomColor;

        ItemStack resultStack;
        if (resolvedTop == resolvedBottom && resolvedTop != -1) {
            Item shulkerItem = Items.DYED_SHULKER_BOX.asList().get(resolvedTop);
            // Transmute keeps contents, name and other components (like vanilla's shulker dye recipe)
            resultStack = shulkerStack.transmuteCopy(shulkerItem, 1);
        } else {
            resultStack = shulkerStack.copy();
            resultStack.setCount(1);
        }

        if (resolvedTop != resolvedBottom) {
            ShulkerColors newColors = new ShulkerColors(resolvedTop, resolvedBottom);
            applyColorsToStack(resultStack, newColors);
        } else if (resultStack != shulkerStack) {
            removeColorsFromItemStack(resultStack);
        }

        return resultStack;
    }

    private static int colorIdOf(ItemStack dyeStack) {
        DyeColor color = dyeStack.get(DataComponents.DYE);
        return color == null ? -1 : color.getId();
    }

    private void applyColorsToStack(ItemStack stack, ShulkerColors colors) {
        CompoundTag nbt = MultiColorShulkers.copyBlockEntityTag(stack);

        CompoundTag attachments = nbt.getCompoundOrEmpty("fabric:attachments");
        CompoundTag colorNbt = new CompoundTag();
        colorNbt.putInt("topColor", colors.topColor());
        colorNbt.putInt("bottomColor", colors.bottomColor());

        attachments.put(MultiColorShulkers.MOD_ID + ":colors", colorNbt);
        nbt.put("fabric:attachments", attachments);

        MultiColorShulkers.setBlockEntityTag(stack, nbt);
    }

    private void removeColorsFromItemStack(ItemStack stack) {
        if (stack.get(DataComponents.BLOCK_ENTITY_DATA) == null) return;
        CompoundTag nbt = MultiColorShulkers.copyBlockEntityTag(stack);
        if (!nbt.contains("fabric:attachments")) return;

        CompoundTag attachments = nbt.getCompoundOrEmpty("fabric:attachments");
        attachments.remove(MultiColorShulkers.MOD_ID + ":colors");
        if (attachments.isEmpty()) {
            nbt.remove("fabric:attachments");
        } else {
            nbt.put("fabric:attachments", attachments);
        }
        MultiColorShulkers.setBlockEntityTag(stack, nbt);
    }

    private int getBaseColor(ItemStack stack) {
        if (Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock shulkerBlock) {
            DyeColor color = shulkerBlock.getColor();
            if (color != null) return color.getId();
        }
        return -1;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
