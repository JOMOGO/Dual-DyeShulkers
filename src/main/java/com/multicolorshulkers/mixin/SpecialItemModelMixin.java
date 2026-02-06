package com.multicolorshulkers.mixin;

//? if MC: >=12102 {
import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.multicolorshulkers.client.MultiColorShulkersClient;
import com.multicolorshulkers.client.ShulkerColorAccess;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.SpecialItemModel;
import net.minecraft.client.render.item.model.special.ShulkerBoxModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
//? if MC: >=12105 {
import net.minecraft.item.ItemDisplayContext;
//?} else {
/*import net.minecraft.item.ModelTransformationMode;
*///?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if MC: >=12106 {
import org.spongepowered.asm.mixin.injection.Redirect;
//?}

@Mixin(SpecialItemModel.class)
public class SpecialItemModelMixin<T> {

    @Shadow @Final private SpecialModelRenderer<T> specialModelType;

    //? if MC: >=12106 {
    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(ItemRenderState renderState, ItemStack stack, ItemModelManager itemModelManager,
                          ItemDisplayContext transformationMode, ClientWorld world,
                          LivingEntity user, int seed, CallbackInfo ci) {
        if (this.specialModelType instanceof ShulkerBoxModelRenderer) {
            ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(stack);
            MultiColorShulkersClient.setItemColors(colors);
            // Add colors to modelKey so the GUI atlas cache distinguishes
            // custom-colored shulkers from regular ones
            if (colors != null) {
                renderState.addModelKey(colors);
            }
        }
    }

    @Redirect(method = "update",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/render/item/ItemRenderState$LayerRenderState;setSpecialModel(Lnet/minecraft/client/render/item/model/special/SpecialModelRenderer;Ljava/lang/Object;)V"))
    private <S> void redirectSetSpecialModel(ItemRenderState.LayerRenderState layerState,
                                             SpecialModelRenderer<S> renderer, S data) {
        layerState.setSpecialModel(renderer, data);
        ShulkerColors colors = MultiColorShulkersClient.getItemColors();
        if (colors != null) {
            ((ShulkerColorAccess) (Object) layerState).multicolor$setShulkerColors(colors);
            MultiColorShulkersClient.clearItemColors();
        }
    }
    //?} else if MC: >=12105 {
    /*@Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(ItemRenderState renderState, ItemStack stack, ItemModelManager itemModelManager,
                          ItemDisplayContext transformationMode, ClientWorld world,
                          LivingEntity user, int seed, CallbackInfo ci) {
        if (this.specialModelType instanceof ShulkerBoxModelRenderer) {
            ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(stack);
            MultiColorShulkersClient.setItemColors(colors);
        }
    }
    *///?} else {
    /*@Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(ItemRenderState renderState, ItemStack stack, ItemModelManager itemModelManager,
                          ModelTransformationMode transformationMode, ClientWorld world,
                          LivingEntity user, int seed, CallbackInfo ci) {
        if (this.specialModelType instanceof ShulkerBoxModelRenderer) {
            ShulkerColors colors = MultiColorShulkers.getColorsFromItemStack(stack);
            MultiColorShulkersClient.setItemColors(colors);
        }
    }
    *///?}
}
//?} else {
/*
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;

// Stub mixin for 1.21.1 - SpecialItemModel doesn't exist
@Mixin(ItemRenderer.class)
public class SpecialItemModelMixin<T> {
    // No-op for 1.21.1 - item rendering handled by BuiltinModelItemRendererMixin
}
*///?}
