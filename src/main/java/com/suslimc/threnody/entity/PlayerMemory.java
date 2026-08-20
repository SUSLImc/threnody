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

public final class PlayerMemory {
    private static final String MEMORY_KEY = "ThrenodyMemory";
    private static final String ENTRIES_KEY = "Encounters";
    private static final String TRANSGRESSIONS_KEY = "Transgressions";
    private static final String GAME_TIME_KEY = "GameTime";
    private static final String THREAT_KEY = "Threat";
    private static final String REASON_KEY = "Reason";

    private PlayerMemory() {
    }

    public static void recordEncounter(ServerPlayer player, BlockPos location, int threat) {
        CompoundTag memory = getMemory(player);
        ListTag entries = memory.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
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

    private static double targetScore(ServerPlayer player, ThrenodyEntity entity) {
        ListTag entries = getActiveEntries(player);
        int rememberedThreat = 0;
        for (Tag tag : entries) {
            rememberedThreat += ((CompoundTag) tag).getInt(THREAT_KEY);
        }
        int transgressions = getTransgressionCount(player);
        return rememberedThreat * 8.0D + transgressions * 40.0D - Math.sqrt(player.distanceToSqr(entity));
    }

    /**
     * Records a broken survival covenant, such as entering creative mode or running a cheat command.
     *
     * @return the number of transgressions the player has accumulated within the forgiveness window
     */
    public static int recordTransgression(ServerPlayer player, String reason) {
        CompoundTag memory = getMemory(player);
        ListTag transgressions = getActiveTransgressions(player);

        CompoundTag entry = new CompoundTag();
        entry.putLong(GAME_TIME_KEY, player.level().getGameTime());
        entry.putString(REASON_KEY, reason);
        transgressions.add(entry);

        int capacity = ThrenodyConfig.COMMON.memoryCapacity.get();
        while (transgressions.size() > capacity) {
            transgressions.remove(0);
        }

        memory.put(TRANSGRESSIONS_KEY, transgressions);
        return transgressions.size();
    }

    public static int getTransgressionCount(ServerPlayer player) {
        return getActiveTransgressions(player).size();
    }

    public static void forgive(ServerPlayer player) {
        getMemory(player).put(TRANSGRESSIONS_KEY, new ListTag());
    }

    private static ListTag getActiveTransgressions(ServerPlayer player) {
        CompoundTag memory = getMemory(player);
        ListTag transgressions = memory.getList(TRANSGRESSIONS_KEY, Tag.TAG_COMPOUND);
        long now = player.level().getGameTime();
        long decayTicks = ThrenodyConfig.COMMON.judgmentForgivenessMinutes.get() * 1_200L;
        transgressions.removeIf(tag -> now - ((CompoundTag) tag).getLong(GAME_TIME_KEY) > decayTicks);
        memory.put(TRANSGRESSIONS_KEY, transgressions);
        return transgressions;
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
