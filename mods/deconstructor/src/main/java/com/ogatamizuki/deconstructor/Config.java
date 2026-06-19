package com.ogatamizuki.deconstructor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_ITEMS = BUILDER
            .comment("Comma- or space-separated item IDs that cannot be deconstructed (e.g. minecraft:netherite_sword).")
            .define("excludedItems", "");

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static boolean isExcluded(Item item) {
        String raw = EXCLUDED_ITEMS.get();
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        for (String entry : raw.split("[,\\s]+")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty() && itemId.equals(trimmed)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidItemId(String id) {
        return id != null && !id.isBlank() && Identifier.tryParse(id.trim()) != null;
    }
}
