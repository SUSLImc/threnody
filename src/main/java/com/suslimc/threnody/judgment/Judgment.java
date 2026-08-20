package com.suslimc.threnody.judgment;

import com.suslimc.threnody.config.ThrenodyConfig;
import com.suslimc.threnody.entity.PlayerMemory;
import com.suslimc.threnody.entity.ThrenodyEntity;
import com.suslimc.threnody.registration.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Threnody treats survival as a covenant. Players who break it by escaping into creative mode
 * or by conjuring resources with cheat commands are judged: they are pulled back into survival
 * and the entity turns its attention directly onto them.
 */
public final class Judgment {
    private static final int SEARCH_RADIUS = 96;

    private Judgment() {
    }

    public static boolean isEnabled() {
        return ThrenodyConfig.COMMON.judgmentEnabled.get();
    }

    public static boolean isExempt(ServerPlayer player) {
        return ThrenodyConfig.COMMON.judgmentExemptOperators.get()
                && player.getServer() != null
                && player.getServer().getPlayerList().isOp(player.getGameProfile());
    }

    /**
     * Records a transgression and escalates Threnody's response.
     *
     * @param revertGameModeNow when true the offender is pulled back into survival immediately;
     *                          callers that cancel a game mode change should pass false
     */
    public static void punish(ServerPlayer player, String reason, boolean revertGameModeNow) {
        if (!isEnabled() || isExempt(player)) {
            return;
        }

        int severity = PlayerMemory.recordTransgression(player, reason);
        PlayerMemory.recordEncounter(player, player.blockPosition(), 4 + severity);

        if (revertGameModeNow
                && ThrenodyConfig.COMMON.judgmentRevertGameMode.get()
                && (player.isCreative() || player.isSpectator())) {
            player.setGameMode(GameType.SURVIVAL);
            player.sendSystemMessage(Component.translatable("threnody.judgment.reverted")
                    .withStyle(ChatFormatting.DARK_RED));
        }

        applyAffliction(player, severity);
        announce(player, severity);

        if (severity >= 2) {
            dispatchJudge(player, severity);
        }
    }

    private static void applyAffliction(ServerPlayer player, int severity) {
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120 + severity * 60, 0));
        player.playNotifySound(SoundEvents.WARDEN_NEARBY_CLOSEST, SoundSource.HOSTILE, 1.0F, 0.5F);

        if (severity >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60 + severity * 20, 0));
        }
        if (severity >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200 + severity * 40, 0));
            player.playNotifySound(SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    private static void announce(ServerPlayer player, int severity) {
        String key = severity <= 1
                ? "threnody.judgment.noticed"
                : severity == 2 ? "threnody.judgment.marked" : "threnody.judgment.hunted";
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_RED));
    }

    private static void dispatchJudge(ServerPlayer player, int severity) {
        ServerLevel level = (ServerLevel) player.level();

        ThrenodyEntity judge = level
                .getEntitiesOfClass(ThrenodyEntity.class, player.getBoundingBox().inflate(SEARCH_RADIUS))
                .stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(player)))
                .orElse(null);

        if (judge == null) {
            if (severity < 3 || !ThrenodyConfig.COMMON.judgmentSummonOnRepeatOffense.get()) {
                return;
            }
            judge = summonJudge(level, player);
            if (judge == null) {
                return;
            }
        } else {
            Vec3 approach = findJudgmentPosition(level, player);
            if (approach != null) {
                judge.teleportTo(approach.x, approach.y, approach.z);
            }
        }

        judge.beginJudgment(player, severity);
    }

    private static @Nullable ThrenodyEntity summonJudge(ServerLevel level, ServerPlayer player) {
        Vec3 position = findJudgmentPosition(level, player);
        if (position == null) {
            return null;
        }

        ThrenodyEntity judge = ModEntities.THRENODY.get().create(level);
        if (judge == null) {
            return null;
        }

        judge.moveTo(position.x, position.y, position.z, level.random.nextFloat() * 360.0F, 0.0F);
        judge.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(BlockPos.containing(position)),
                MobSpawnType.EVENT,
                null,
                null
        );
        level.addFreshEntity(judge);
        return judge;
    }

    private static @Nullable Vec3 findJudgmentPosition(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.random;

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 5.0D + random.nextDouble() * 5.0D;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;

            BlockPos ground = findGround(level, x, player.getBlockY(), z);
            if (ground == null) {
                continue;
            }

            Vec3 candidate = new Vec3(ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D);
            AABB bounds = ModEntities.THRENODY.get().getAABB(candidate.x, candidate.y, candidate.z);
            if (level.noCollision(bounds)) {
                return candidate;
            }
        }
        return null;
    }

    private static @Nullable BlockPos findGround(ServerLevel level, double x, int startY, double z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int offset = 3; offset >= -5; offset--) {
            cursor.set(Math.floor(x), startY + offset, Math.floor(z));
            if (!level.getBlockState(cursor).isAir()
                    && level.getBlockState(cursor.above()).isAir()
                    && level.getBlockState(cursor.above(2)).isAir()) {
                return cursor.immutable();
            }
        }
        return null;
    }

    public static List<? extends String> watchedCommands() {
        return ThrenodyConfig.COMMON.judgmentWatchedCommands.get();
    }
}
