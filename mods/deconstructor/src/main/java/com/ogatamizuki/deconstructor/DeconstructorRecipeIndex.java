package com.ogatamizuki.deconstructor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * クラフトレシピの成果物 Item からレシピ・素材リストへの逆引きインデックス。
 * RecipeManager が変わったときだけ全レシピを1回走査して構築する。
 */
public final class DeconstructorRecipeIndex {
    public record Entry(RecipeHolder<CraftingRecipe> recipe, List<Ingredient> ingredients, int recipeOutputCount) {}

    private static final CraftingInput DUMMY_INPUT = CraftingInput.of(3, 3, List.of(
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    ));

    private static RecipeManager indexedManager;
    private static Map<Item, Entry> index = Map.of();

    private DeconstructorRecipeIndex() {}

    public static Entry lookup(RecipeManager recipeManager, Item item) {
        if (Config.isExcluded(item)) {
            return null;
        }
        ensureBuilt(recipeManager);
        return index.get(item);
    }

    public static void invalidate() {
        indexedManager = null;
        index = Map.of();
    }

    private static boolean containsOutputAsIngredient(Item resultItem, List<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            @SuppressWarnings("deprecation")
            var holders = ingredient.items();
            if (holders.anyMatch(holder -> holder.value() == resultItem)) {
                return true;
            }
        }
        return false;
    }

    private static void ensureBuilt(RecipeManager recipeManager) {
        if (recipeManager == indexedManager) {
            return;
        }
        synchronized (DeconstructorRecipeIndex.class) {
            if (recipeManager == indexedManager) {
                return;
            }
            Map<Item, Entry> newIndex = new HashMap<>();
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                if (!(holder.value() instanceof CraftingRecipe craftingRecipe) || craftingRecipe.isSpecial()) {
                    continue;
                }
                ItemStack result = resolveResult(craftingRecipe);
                if (result.isEmpty()) {
                    continue;
                }
                Item resultItem = result.getItem();
                if (Config.isExcluded(resultItem)) {
                    continue;
                }
                if (newIndex.containsKey(resultItem)) {
                    continue;
                }
                List<Ingredient> ingredients = extractIngredients(craftingRecipe);
                if (ingredients.isEmpty()) {
                    continue;
                }
                if (containsOutputAsIngredient(resultItem, ingredients)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                RecipeHolder<CraftingRecipe> craftingHolder = (RecipeHolder<CraftingRecipe>) holder;
                newIndex.put(resultItem, new Entry(craftingHolder, ingredients, result.getCount()));
            }
            index = Collections.unmodifiableMap(newIndex);
            indexedManager = recipeManager;
        }
    }

    private static ItemStack resolveResult(CraftingRecipe recipe) {
        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            var result = shapelessRecipe.result();
            return result.create();
        }
        try {
            return recipe.assemble(DUMMY_INPUT);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static List<Ingredient> extractIngredients(CraftingRecipe recipe) {
        PlacementInfo placement = recipe.placementInfo();
        if (placement == null || placement.isImpossibleToPlace()) {
            return List.of();
        }
        return placement.ingredients();
    }

    public static List<ItemStack> previewStacksFor(Ingredient ingredient) {
        @SuppressWarnings("deprecation")
        var holders = ingredient.items();
        List<ItemStack> items = new ArrayList<>();
        holders.forEach(holder -> items.add(new ItemStack(holder.value())));
        if (items.isEmpty()) {
            return List.of();
        }
        ItemStack stack = items.getFirst().copy();
        stack.setCount(1);
        return List.of(stack);
    }
}
