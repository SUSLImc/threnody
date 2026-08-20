package com.suslimc.threnody.ai;

import com.suslimc.threnody.config.ThrenodyConfig;
import com.suslimc.threnody.entity.ThrenodyEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Holds Threnody at a deliberate distance while it is stalking. It advances only while nobody is
 * looking at it, which produces the classic "it was closer last time I turned around" effect.
 */
public class ThrenodyStalkGoal extends Goal {
    private final ThrenodyEntity threnody;
    private int repathCooldown;

    public ThrenodyStalkGoal(ThrenodyEntity threnody) {
        this.threnody = threnody;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !threnody.isHunting()
                && !threnody.isVanishing()
                && threnody.getTarget() != null
                && threnody.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        repathCooldown = 0;
        threnody.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = threnody.getTarget();
        if (target == null) {
            return;
        }

        threnody.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (threnody.isFrozen()) {
            threnody.getNavigation().stop();
            return;
        }

        double stalkDistance = ThrenodyConfig.COMMON.stalkDistance.get();
        double distance = Math.sqrt(target.distanceToSqr(threnody));

        if (distance <= stalkDistance) {
            threnody.getNavigation().stop();
            return;
        }

        if (--repathCooldown > 0) {
            return;
        }
        repathCooldown = 10;

        // Approach slowly and deliberately; the speed comes later, when it stops pretending.
        threnody.getNavigation().moveTo(target, distance > stalkDistance * 2.5D ? 0.85D : 0.55D);
    }
}
