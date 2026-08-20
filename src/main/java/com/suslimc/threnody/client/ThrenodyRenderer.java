package com.suslimc.threnody.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.suslimc.threnody.entity.ThrenodyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ThrenodyRenderer extends GeoEntityRenderer<ThrenodyEntity> {
    public ThrenodyRenderer(EntityRendererProvider.Context context) {
        super(context, new ThrenodyGeoModel());
        this.shadowRadius = 0.55F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void preRender(
            PoseStack poseStack,
            ThrenodyEntity animatable,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float stageScale = 0.97F + animatable.getStage().getId() * 0.028F;
        poseStack.scale(stageScale, stageScale, stageScale);

        super.preRender(
                poseStack,
                animatable,
                model,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );
    }
}
