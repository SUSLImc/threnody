package com.suslimc.threnody;

import com.suslimc.threnody.config.ThrenodyConfig;
import com.suslimc.threnody.entity.ThrenodyEntity;
import com.suslimc.threnody.registration.ModEntities;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

@Mod(ThrenodyMod.MODID)
public class ThrenodyMod {
    public static final String MODID = "threnody";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ThrenodyMod(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ThrenodyConfig.register(context);
        bus.addListener(this::registerAttributes);
        bus.addListener(this::registerSpawnPlacements);

        LOGGER.info("Threnody mod constructor finished registration");
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.THRENODY.get(), ThrenodyEntity.createAttributes().build());
    }

    private void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(
                ModEntities.THRENODY.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ThrenodyEntity::checkThrenodySpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
    }
}
