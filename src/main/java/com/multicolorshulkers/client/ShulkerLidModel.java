package com.multicolorshulkers.client;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * A single-part {@link Model} that wraps a shulker box's lid and reproduces the vanilla lid
 * open/close animation in {@link #setupAnim(Float)}.
 *
 * <p>The dual-color renderer submits the lid through {@code submitModel(lidModel, progress, ...)}
 * rather than {@code submitModelPart}, so the deferred render pipeline re-applies the animation
 * for each shulker at draw time. Without this, all colored shulkers sharing the renderer's single
 * model instance would draw with whichever shulker's lid pose was set last (boxes appearing
 * "linked" / not animating).
 */
public class ShulkerLidModel extends Model<Float> {

    public ShulkerLidModel(ModelPart lid) {
        super(lid, RenderTypes::entityCutout);
    }

    @Override
    public void setupAnim(Float progress) {
        ModelPart lid = this.root();
        lid.setPos(0.0F, 24.0F - progress * 0.5F * 16.0F, 0.0F);
        lid.yRot = 270.0F * progress * (float) (Math.PI / 180.0);
    }
}
