package com.suslimc.threnody.registration;

import com.suslimc.threnody.ThrenodyMod;
import com.suslimc.threnody.entity.ThrenodyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ThrenodyMod.MODID);

    public static final RegistryObject<EntityType<ThrenodyEntity>> THRENODY = ENTITIES.register("threnody",
            () -> EntityType.Builder.of(ThrenodyEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.85F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(ThrenodyMod.MODID + ":threnody")
    );

    private ModEntities() {
    }
}
