package com.ogatamizuki.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.ogatamizuki.guide.model.GuideManualDefinition;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuideManualLoader extends SimplePreparableReloadListener<Map<Identifier, GuideManualDefinition>> {
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "guide_manuals");

    @Override
    protected Map<Identifier, GuideManualDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return loadManuals(resourceManager);
    }

    @Override
    protected void apply(Map<Identifier, GuideManualDefinition> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        GuideManualRegistry.setManuals(loaded);
        GuideLibMod.LOGGER.info("Loaded {} guide manual(s)", loaded.size());
    }

    public static Map<Identifier, GuideManualDefinition> loadManuals(ResourceManager resourceManager) {
        Map<Identifier, GuideManualDefinition> manuals = new LinkedHashMap<>();

        for (Map.Entry<Identifier, List<Resource>> entry : resourceManager.listResourceStacks(
                "guide/manuals",
                path -> path.getPath().endsWith(".json")
        ).entrySet()) {
            Identifier resourceId = entry.getKey();
            String resourcePath = resourceId.getPath();
            if (!resourcePath.startsWith("guide/manuals/") || !resourcePath.endsWith(".json")) {
                continue;
            }
            String manualLocalId = resourcePath.substring("guide/manuals/".length(), resourcePath.length() - ".json".length());
            Identifier manualId = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), manualLocalId);

            for (Resource resource : entry.getValue()) {
                try (BufferedReader reader = resource.openAsReader()) {
                    JsonElement parsed = com.google.gson.JsonParser.parseReader(reader);
                    if (!parsed.isJsonObject()) {
                        continue;
                    }
                    GuideManualDefinition manual = parseManual(manualId, parsed.getAsJsonObject());
                    if (manual != null) {
                        manuals.put(manual.itemId(), manual);
                    }
                } catch (IOException | JsonParseException e) {
                    GuideLibMod.LOGGER.error("Failed to load guide manual {}", manualId, e);
                }
            }
        }

        return manuals;
    }

    private static GuideManualDefinition parseManual(Identifier manualId, JsonObject root) {
        if (!root.has("item") || !root.has("opens_book")) {
            throw new JsonParseException("Manual " + manualId + " requires item and opens_book");
        }
        Identifier itemId = Identifier.parse(root.get("item").getAsString());
        Identifier opensBook = Identifier.parse(root.get("opens_book").getAsString());
        Identifier themeId = root.has("theme")
                ? Identifier.parse(root.get("theme").getAsString())
                : GuideTheme.BOOK_ID;
        return new GuideManualDefinition(manualId, itemId, opensBook, themeId);
    }
}
