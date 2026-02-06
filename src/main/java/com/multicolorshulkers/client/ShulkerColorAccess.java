package com.multicolorshulkers.client;

//? if MC: >=12106 {
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;

public interface ShulkerColorAccess {
    void multicolor$setShulkerColors(ShulkerColors colors);
    ShulkerColors multicolor$getShulkerColors();
}
//?} else {
/*public interface ShulkerColorAccess {}
*///?}
