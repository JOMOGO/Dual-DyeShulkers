# Dual-Dye Shulkers

[![Modrinth](https://img.shields.io/modrinth/dt/dual-dye-shulkers?logo=modrinth&label=Modrinth)](https://modrinth.com/project/dual-dye-shulkers)

A Fabric mod for Minecraft 1.21.1 – 26.3 that lets you apply different colors to the **top (lid)** and **bottom (base)** of shulker boxes!

## Features

- **Dual-color shulker boxes** - Apply one color to the lid, another to the base
- **Configurable controls** (default):
  - Shift + right-click with dye → colors the **top (lid)**
  - Ctrl + right-click with dye → colors the **bottom (base)**
- **Crafting recipe** - Dye above the shulker colors the lid, dye below colors the base
- **Works with all 16 dye colors** (256 combinations!)
- **Colors persist** through break/place cycles
- **Creative pick-block** copies the exact colors
- **Cauldron washing** - Remove custom colors with a water cauldron
- **Tooltip display** - See applied colors when hovering over items
- **Full 3D rendering** - Colors display correctly on placed blocks and items

## Usage

1. Place a shulker box
2. Hold any dye and Shift+right-click the shulker to color the **lid**
3. Hold any dye and Ctrl+right-click to color the **base**
4. Or craft it: put the shulker box in a crafting grid with a dye above it (lid) and/or below it (base)
5. To remove colors, use a water cauldron on the shulker box item

## Configuration

Install [Cloth Config](https://modrinth.com/mod/cloth-config) and [Mod Menu](https://modrinth.com/mod/modmenu) for an in-game config screen.

**Options:**
- **Show Color Tooltip** - Toggle tooltip display on shulker items
- **Dye Top (Lid)** - Customize key combo for coloring the top (default: Shift + Right Click)
- **Dye Bottom (Base)** - Customize key combo for coloring the bottom (default: Ctrl + Right Click)

Key combos can be any combination of two keys/mouse buttons.

## Requirements

- Minecraft 1.21.1, 1.21.4 – 1.21.8, 26.2 or 26.3
- Fabric Loader 0.16.0+ (1.21.x) or 0.19.0+ with Java 25 (26.x)
- [Fabric API](https://modrinth.com/mod/fabric-api)

**Optional:**
- [Cloth Config](https://modrinth.com/mod/cloth-config) - For config screen
- [Mod Menu](https://modrinth.com/mod/modmenu) - For config screen access

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download this mod from [Modrinth](https://modrinth.com/project/dual-dye-shulkers)
4. Place both mods in your `mods` folder

## Building from Source

This project uses [Stonecutter](https://github.com/kikugie/stonecutter) for multi-version support.

```bash
# Windows
# Build for the active version (manage active version with ./gradlew chiseled)
gradlew.bat build

# Linux/macOS
./gradlew build
```

The output JAR will be in `versions/<mc-version>/build/libs/`.

Minecraft 1.21.x is built from the `master` branch, 26.x from the `26.x` branch.

## Tests

End-to-end tests use [Fabric GameTest](https://docs.fabricmc.net/develop/automatic-testing) and live in `src/gametest`.

```bash
# Server game tests (headless, also run by `build`)
./gradlew 26.3:runGameTest

# Client game tests: real client with simulated input and screenshot checks
./gradlew 26.3:runClientGameTest
```

Screenshots are saved to `versions/<mc-version>/build/run/clientGameTest/screenshots/`.

## License

MIT License - see [LICENSE](LICENSE) for details.
