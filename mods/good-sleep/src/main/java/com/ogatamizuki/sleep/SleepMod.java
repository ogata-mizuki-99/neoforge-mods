package com.ogatamizuki.sleep;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.ClockAdjustment;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("good_sleep")
public class SleepMod {
    public static final String MODID = "good_sleep";
    public static final Logger LOGGER = LogManager.getLogger(SleepMod.class);

    private static final int VANILLA_SLEEPING_PERCENTAGE = 100;

    public SleepMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Good Sleep Mod Initializing...");

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::onConfigReload);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        applySleepingPercentage(event.getServer());
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                applySleepingPercentage(server);
            }
        }
    }

    @SubscribeEvent
    public void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!Config.ALLOW_DAY_SLEEP.get() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.isDarkOutside()) {
            return;
        }

        event.setAdjustment(new ClockAdjustment.Marker(ClockTimeMarkers.NIGHT));
    }

    @SubscribeEvent
    public void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (!Config.ALLOW_DAY_SLEEP.get() || event.getProblem() == null) {
            return;
        }

        Player.BedSleepingProblem vanilla = event.getVanillaProblem();
        if (vanilla == Player.BedSleepingProblem.NOT_SAFE
                || vanilla == Player.BedSleepingProblem.TOO_FAR_AWAY
                || vanilla == Player.BedSleepingProblem.OBSTRUCTED) {
            return;
        }

        if (isDaytimeSleepBlocked(event.getEntity(), event.getPos())) {
            event.setProblem(null);
        }
    }

    @SubscribeEvent
    public void onCanContinueSleeping(CanContinueSleepingEvent event) {
        if (!Config.ALLOW_DAY_SLEEP.get() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos pos = player.getSleepingPos().orElse(player.blockPosition());
        if (isDaytimeSleepBlocked(player, pos)) {
            event.setContinueSleeping(true);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !Config.HEAL_WHILE_SLEEPING.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.isSleeping()) {
            return;
        }

        int interval = Config.HEAL_INTERVAL_TICKS.get();
        if (interval == 0) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            return;
        }

        if (!player.isSleepingLongEnough() || player.tickCount % interval != 0) {
            return;
        }

        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity().level().isClientSide() || !Config.HEAL_WHILE_SLEEPING.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // STOP_SLEEPING（プレイヤーによるキャンセル）は updateLevel=true。
        // 時間スキップ完了時の wakeUpAllPlayers は updateLevel=false。
        if (event.updateLevel()) {
            return;
        }

        player.setHealth(player.getMaxHealth());
    }

    private static boolean isDaytimeSleepBlocked(Player player, BlockPos pos) {
        Level level = player.level();
        BedRule rule = level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos);
        return !rule.canSleep(level);
    }

    private static void applySleepingPercentage(MinecraftServer server) {
        int percentage = Config.ONE_PLAYER_SKIP.get() ? 0 : VANILLA_SLEEPING_PERCENTAGE;
        server.getGameRules().set(GameRules.PLAYERS_SLEEPING_PERCENTAGE, percentage, server);
        LOGGER.info("playersSleepingPercentage set to {}", percentage);
    }
}
