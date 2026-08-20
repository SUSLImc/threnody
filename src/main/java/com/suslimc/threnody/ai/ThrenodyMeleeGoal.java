package com.suslimc.threnody.ai;

import com.suslimc.threnody.entity.ThrenodyEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Ordinary melee pursuit, but only once Threnody has stopped stalking and committed to the kill.
 */
public class ThrenodyMeleeGoal extends MeleeAttackGoal {
    private final ThrenodyEntity threnody;

    public ThrenodyMeleeGoal(ThrenodyEntity threnody) {
        super(threnody, 1.0D, true);
        this.threnody = threnody;
    }

    @Override
    public boolean canUse() {
        return threnody.isHunting() && !threnody.isVanishing() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return threnody.isHunting() && !threnody.isVanishing() && super.canContinueToUse();
    }
}
