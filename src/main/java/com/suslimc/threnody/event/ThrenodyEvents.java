package com.suslimc.threnody.event;

import com.suslimc.threnody.ThrenodyMod;
import com.suslimc.threnody.config.ThrenodyConfig;
import com.suslimc.threnody.entity.ThrenodyEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Once its prey is dead there is nothing to stalk, so Threnody leaves rather than standing
 * over the corpse waiting for the player to respawn.
 */
@Mod.EventBusSubscriber(modid = ThrenodyMod.MODID)
public final class ThrenodyEvents {
    private static final double CLEANUP_RADIUS = 96.0D;

    private ThrenodyEvents() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!ThrenodyConfig.COMMON.vanishAfterKill.get()
                || !(event.getEntity() instanceof Player player)
                || player.level().isClientSide) {
            return;
        }

        if (event.getSource().getEntity() instanceof ThrenodyEntity killer) {
            killer.beginVanish();
        }

        // Any other Threnody that was hunting this player has also lost its reason to be here.
        AABB area = player.getBoundingBox().inflate(CLEANUP_RADIUS);
        for (ThrenodyEntity nearby : player.level().getEntitiesOfClass(ThrenodyEntity.class, area)) {
            if (nearby.getTarget() == player) {
                nearby.beginVanish();
            }
        }
    }
}
