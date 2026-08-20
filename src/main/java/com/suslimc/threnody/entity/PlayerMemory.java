package com.suslimc.threnody.entity;

import com.suslimc.threnody.config.ThrenodyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Bounded, decaying record of what each player has done to Threnody.
 * Recent and violent encounters raise that player's priority the next time it hunts.
 */
public final class PlayerMemory {
    private static final String MEMORY_KEY = "ThrenodyMemory";
    private static final String ENTRIES_KEY = "Encounters";
    private static final String GAME_TIME_KEY = "GameTime";
    private static final String THREAT_KEY = "Threat";

    private PlayerMemory() {
    }

    public static void recordEncounter(ServerPlayer player, BlockPos location, int threat) {
        CompoundTag memory = getMemory(player);
        ListTag entries = getActiveEntries(player);

        CompoundTag entry = new CompoundTag();
        entry.putLong("Location", location.asLong());
        entry.putLong(GAME_TIME_KEY, player.level().getGameTime());
        entry.putInt(THREAT_KEY, Math.max(1, threat));
        entries.add(entry);

        int capacity = ThrenodyConfig.COMMON.memoryCapacity.get();
        while (entries.size() > capacity) {
            entries.remove(0);
        }
        memory.put(ENTRIES_KEY, entries);
    }

    public static @Nullable ServerPlayer findPreferredTarget(ServerLevel level, ThrenodyEntity entity) {
        double followRange = entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        double maxDistanceSquared = followRange * followRange;

        return level.players().stream()
                .filter(player -> !player.isCreative() && !player.isSpectator() && player.isAlive())
                .filter(player -> player.distanceToSqr(entity) <= maxDistanceSquared)
                .max(Comparator.comparingDouble(player -> targetScore(player, entity)))
                .orElse(null);
    }

    public static int threatOf(ServerPlayer player) {
        int threat = 0;
        for (Tag tag : getActiveEntries(player)) {
            threat += ((CompoundTag) tag).getInt(THREAT_KEY);
        }
        return threat;
    }

    private static double targetScore(ServerPlayer player, ThrenodyEntity entity) {
        return threatOf(player) * 8.0D - Math.sqrt(player.distanceToSqr(entity));
    }

    private static ListTag getActiveEntries(ServerPlayer player) {
        CompoundTag memory = getMemory(player);
        ListTag entries = memory.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        long now = player.level().getGameTime();
        long decayTicks = ThrenodyConfig.COMMON.memoryDecayMinutes.get() * 1_200L;
        entries.removeIf(tag -> now - ((CompoundTag) tag).getLong(GAME_TIME_KEY) > decayTicks);
        memory.put(ENTRIES_KEY, entries);
        return entries;
    }

    private static CompoundTag getMemory(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        CompoundTag memory = persisted.getCompound(MEMORY_KEY);
        persisted.put(MEMORY_KEY, memory);
        return memory;
    }
}
