package com.multicolorshulkers.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import com.multicolorshulkers.MultiColorShulkers;

public class ClientConfigHelper {

    // Transient cached combos
    private static KeyCombo cachedTopCombo;
    private static KeyCombo cachedBottomCombo;

    // Default combos
    public static final KeyCombo DEFAULT_TOP_COMBO = new KeyCombo(
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT),
            InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_RIGHT)
    );
    public static final KeyCombo DEFAULT_BOTTOM_COMBO = new KeyCombo(
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL),
            InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_RIGHT)
    );

    public static KeyCombo getTopCombo(ModConfig config) {
        if (cachedTopCombo == null) {
            try {
                InputConstants.Key k1 = InputConstants.getKey(config.topKey1);
                InputConstants.Key k2 = InputConstants.getKey(config.topKey2);
                cachedTopCombo = new KeyCombo(k1, k2);
            } catch (Exception e) {
                MultiColorShulkers.LOGGER.warn("Invalid top combo, using default");
                cachedTopCombo = DEFAULT_TOP_COMBO;
            }
        }
        return cachedTopCombo;
    }

    public static void setTopCombo(ModConfig config, KeyCombo combo) {
        cachedTopCombo = combo;
        config.topKey1 = combo.getKey1().getName();
        config.topKey2 = combo.getKey2().getName();
    }

    public static KeyCombo getBottomCombo(ModConfig config) {
        if (cachedBottomCombo == null) {
            try {
                InputConstants.Key k1 = InputConstants.getKey(config.bottomKey1);
                InputConstants.Key k2 = InputConstants.getKey(config.bottomKey2);
                cachedBottomCombo = new KeyCombo(k1, k2);
            } catch (Exception e) {
                MultiColorShulkers.LOGGER.warn("Invalid bottom combo, using default");
                cachedBottomCombo = DEFAULT_BOTTOM_COMBO;
            }
        }
        return cachedBottomCombo;
    }

    public static void setBottomCombo(ModConfig config, KeyCombo combo) {
        cachedBottomCombo = combo;
        config.bottomKey1 = combo.getKey1().getName();
        config.bottomKey2 = combo.getKey2().getName();
    }

    public static void resetCache() {
        cachedTopCombo = null;
        cachedBottomCombo = null;
    }
}
