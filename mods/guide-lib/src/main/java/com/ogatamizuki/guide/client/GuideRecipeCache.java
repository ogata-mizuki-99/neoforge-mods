package com.ogatamizuki.guide.client;

import com.ogatamizuki.guide.GuideLibMod;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/**
 * Client-side crafting recipe cache loaded from data packs.
 * Multiplayer clients do not receive the full RecipeManager, so guide crafting pages
 * resolve recipes from local JSON instead.
 */
public final class GuideRecipeCache {
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "guide_recipes");

    private static RecipeManager recipeManager;

    private GuideRecipeCache() {}

    public static void register(AddClientReloadListenersEvent event) {
        recipeManager = new RecipeManager(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        event.addListener(LISTENER_ID, recipeManager);
    }

    public static RecipeHolder<CraftingRecipe> get(Identifier recipeId) {
        if (recipeManager == null) {
            return null;
        }
        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeId);
        return recipeManager.byKey(recipeKey)
                .filter(holder -> holder.value() instanceof CraftingRecipe)
                .map(holder -> {
                    @SuppressWarnings("unchecked")
                    RecipeHolder<CraftingRecipe> typed = (RecipeHolder<CraftingRecipe>) holder;
                    return typed;
                })
                .orElse(null);
    }
}
