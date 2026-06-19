package com.ogatamizuki.radialteleport;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_CRAFTING_RECIPE = BUILDER
            .comment("If true, the Teleport Compass can be crafted with the configured recipe.",
                    "If false, obtain it via /radialteleport give or creative mode.")
            .define("enableCraftingRecipe", true);

    public static final ModConfigSpec.BooleanValue ENABLE_WAYPOINTS = BUILDER
            .comment("If true, players can save personal waypoints and teleport to them from the radial menu.")
            .define("enableWaypoints", true);

    public static final ModConfigSpec.IntValue MAX_WAYPOINTS_PER_PLAYER = BUILDER
            .comment("Maximum number of personal waypoints each player may save.")
            .defineInRange("maxWaypointsPerPlayer", 8, 1, 32);

    public static final ModConfigSpec.IntValue TELEPORT_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown between teleports in ticks (20 = 1 second). 0 disables cooldown.")
            .defineInRange("teleportCooldownTicks", 0, 0, 72000);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
