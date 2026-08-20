package com.suslimc.threnody.client;

import com.suslimc.threnody.ThrenodyMod;
import com.suslimc.threnody.entity.ThrenodyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Binds the shared Threnody geometry and animation files while swapping the texture per evolution stage.
 */
public class ThrenodyGeoModel extends DefaultedEntityGeoModel<ThrenodyEntity> {
    private static final ResourceLocation[] STAGE_TEXTURES = new ResourceLocation[6];

    static {
        for (int stage = 0; stage < STAGE_TEXTURES.length; stage++) {
            STAGE_TEXTURES[stage] = ResourceLocation.fromNamespaceAndPath(
                    ThrenodyMod.MODID,
                    "textures/entity/threnody_stage" + stage + ".png"
            );
        }
    }

    public ThrenodyGeoModel() {
        super(ResourceLocation.fromNamespaceAndPath(ThrenodyMod.MODID, "threnody"), true);
    }

    @Override
    public ResourceLocation getTextureResource(ThrenodyEntity animatable) {
        return STAGE_TEXTURES[animatable.getStage().getId()];
    }
}
