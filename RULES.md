# Dual-Dye Shulkers — Behavior Rules

This document is the single reference for how the mod is supposed to behave. It applies to all
supported versions; version-specific implementation details live in the code.

## 1. Dyeing a placed shulker box (in-world)

- Hold a dye and aim at a **placed** shulker box, then:
  - **Shift + Right-click** → dyes the **top** (lid). *(default binding)*
  - **Ctrl + Right-click** → dyes the **bottom** (base). *(default binding)*
- Right-clicking **without** a binding held opens the shulker normally.
- If both bindings are somehow active at once, **bottom** wins.
- The player must be **within 6 blocks** of the box (verified server-side).
- One dye is **consumed** in survival; **free** in creative.
- In-world dyeing can be turned off (`enableKeybinds`). Bindings are configurable.

## 2. Crafting (dual-dye recipe)

Place a shulker box plus one or more dyes in a crafting grid. Dye **position relative to the
shulker** decides which part is colored:

- **Crafting table, items spanning all 3 rows** (absolute rows):
  - dye in the **top** row → top color
  - dye in the **bottom** row → bottom color
  - dye in the **middle** row → whole box (vanilla-style)
- **2×2 grid, or a table where items don't span 3 rows** (relative to the shulker):
  - dye **above** the shulker → top color
  - dye **below** the shulker → bottom color
  - dye in the **same row** as the shulker → whole box (vanilla-style)

Resolution order: a "whole box" dye is applied to both parts first, then position-specific
top/bottom dyes override it. The input shulker's existing colors/contents carry through as the base.

- If the final top and bottom are the **same** color → output is a **plain vanilla** single-color
  shulker (custom color data stripped).
- If they **differ** → output is a shulker carrying the two custom colors.
- The vanilla shulker+dye recipe is **blocked** for shulker+dye combinations so this recipe handles
  them; any vanilla-style output is stripped of custom colors.
- Crafting can be turned off (`enableCrafting`).

## 3. Color persistence & transfer

- Colors are stored as a **persistent attachment** on the shulker block entity and mirrored into the
  item's block-entity data, so they travel with the item.
- **Placing** a colored shulker restores its colors to the block.
- **Breaking** a colored shulker (survival) drops an item that **keeps** the colors.
- **Creative pick-block** (middle-click) copies the **exact** colors onto the picked item
  (with or without Ctrl).
- Colors **sync to nearby clients** (within 64 blocks) when dyed, when placed, and on player join.

## 4. Cauldron washing

- Right-click a **water cauldron** with a dual-colored shulker item → removes the custom colors,
  lowers the water level, and awards the *Clean Shulker Box* stat.
- A shulker with no custom colors falls through to normal cauldron behavior.

## 5. Vanilla (whole-box) dyeing

- Dyeing a shulker the vanilla way (whole box) **strips** any custom top/bottom colors, returning it
  to a single uniform color.

## 6. Rendering

- Placed blocks render the **lid with the top color** and the **base with the bottom color**.
- Shulker **items** (inventory, hand, dropped entity, item frame) render the same dual colors.
- If only one part has a custom color, the other part uses the box's base color.
- Lids **animate open/close independently** — opening one box never animates another.

## 7. No color bleed

- When a colored shulker is removed, nearby clients **clear the cached colors** for that spot, so a
  different shulker later placed there does **not** inherit the old colors.

## 8. Configuration (`config/dual-dye-shulkers.json`)

| Key | Meaning |
|-----|---------|
| `showTooltip` | Show a "Custom Colors" tooltip on colored shulker items |
| `enableCrafting` | Enable/disable the dual-dye crafting recipe |
| `enableKeybinds` | Enable/disable in-world dye-by-clicking |
| `topKey1` / `topKey2` | Two-key combo that dyes the top |
| `bottomKey1` / `bottomKey2` | Two-key combo that dyes the bottom |

A Cloth Config screen (via Mod Menu) exposes the toggles; the keybind combos are edited in the JSON.
