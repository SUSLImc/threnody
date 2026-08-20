package com.suslimc.threnody.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.suslimc.threnody.ThrenodyMod;
import com.suslimc.threnody.entity.ThrenodyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ThrenodyRenderer extends MobRenderer<ThrenodyEntity, HumanoidModel<ThrenodyEntity>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[6];

    static {
        for (int stage = 0; stage < TEXTURES.length; stage++) {
            TEXTURES[stage] = ResourceLocation.fromNamespaceAndPath(
                    ThrenodyMod.MODID,
                    "textures/entity/threnody_stage" + stage + ".png"
            );
        }
    }

    public ThrenodyRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrenodyEntity entity) {
        return TEXTURES[entity.getStage().getId()];
    }

    @Override
    protected void scale(ThrenodyEntity entity, PoseStack poseStack, float partialTick) {
        float scale = 0.9F + entity.getStage().getId() * 0.05F;
        poseStack.scale(scale, scale, scale);
    }
}
