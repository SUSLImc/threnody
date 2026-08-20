package com.suslimc.threnody.judgment;

import com.suslimc.threnody.ThrenodyMod;
import com.suslimc.threnody.config.ThrenodyConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * Watches for players breaking the survival covenant through creative mode or cheat commands.
 */
@Mod.EventBusSubscriber(modid = ThrenodyMod.MODID)
public final class JudgmentHandler {
    private static final int SWEEP_INTERVAL_TICKS = 100;

    private JudgmentHandler() {
    }

    @SubscribeEvent
    public static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!Judgment.isEnabled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        GameType requested = event.getNewGameMode();
        if (requested != GameType.CREATIVE && requested != GameType.SPECTATOR) {
            return;
        }
        if (Judgment.isExempt(player)) {
            return;
        }

        boolean blocked = ThrenodyConfig.COMMON.judgmentRevertGameMode.get();
        Judgment.punish(player, "gamemode." + requested.getName(), false);

        if (blocked) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!Judgment.isEnabled() || !ThrenodyConfig.COMMON.judgmentWatchCommands.get()) {
            return;
        }
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String root = rootCommand(event.getParseResults().getReader().getString());
        if (root.isEmpty() || !Judgment.watchedCommands().contains(root)) {
            return;
        }

        Judgment.punish(player, "command." + root, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !Judgment.isEnabled()
                || !ThrenodyConfig.COMMON.judgmentRevertGameMode.get()
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % SWEEP_INTERVAL_TICKS != 0) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            Judgment.punish(player, "gamemode.lingering", true);
        }
    }

    private static String rootCommand(String rawInput) {
        String command = rawInput.startsWith("/") ? rawInput.substring(1) : rawInput;
        int separator = command.indexOf(' ');
        if (separator >= 0) {
            command = command.substring(0, separator);
        }
        int namespace = command.indexOf(':');
        if (namespace >= 0) {
            command = command.substring(namespace + 1);
        }
        return command.trim().toLowerCase(Locale.ROOT);
    }
}
