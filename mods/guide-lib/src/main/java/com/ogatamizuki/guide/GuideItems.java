package com.ogatamizuki.guide;

import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GuideItems {
    public static final Identifier MODEL_CODEX = Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "codex");
    public static final Identifier MODEL_CODEX_TABLET = Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "codex_tablet");

    private GuideItems() {}

    public static ItemStack createCodex(Identifier themeId) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(GuideLibDataComponents.GUIDE_ACCESS.get(), GuideAccess.codex(themeId));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.guide_lib.codex"));
        stack.set(DataComponents.ITEM_MODEL, MODEL_CODEX);
        return stack;
    }

    public static ItemStack createCodexTablet() {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(GuideLibDataComponents.GUIDE_ACCESS.get(), GuideAccess.codex(GuideTheme.TABLET_ID));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.guide_lib.codex_tablet"));
        stack.set(DataComponents.ITEM_MODEL, MODEL_CODEX_TABLET);
        return stack;
    }

    public static ItemStack createManual(Identifier bookId, Identifier themeId) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(GuideLibDataComponents.GUIDE_ACCESS.get(), GuideAccess.manual(bookId, themeId));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.guide_lib.manual"));
        return stack;
    }

    public static ItemStack createManual(Identifier bookId, String translationKey, Identifier themeId) {
        ItemStack stack = createManual(bookId, themeId);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(translationKey));
        return stack;
    }

    public static boolean isGuideItem(ItemStack stack) {
        return !stack.isEmpty() && stack.has(GuideLibDataComponents.GUIDE_ACCESS.get());
    }

    public static GuideAccess getAccess(ItemStack stack) {
        return stack.get(GuideLibDataComponents.GUIDE_ACCESS.get());
    }

    public static ItemStack applyItemModel(ItemStack stack) {
        if (!isGuideItem(stack)) {
            return stack;
        }
        Identifier expected = itemModelFor(getAccess(stack));
        if (expected == null) {
            return stack;
        }
        Identifier current = stack.get(DataComponents.ITEM_MODEL);
        if (expected.equals(current)) {
            return stack;
        }
        ItemStack copy = stack.copy();
        copy.set(DataComponents.ITEM_MODEL, expected);
        return copy;
    }

    private static Identifier itemModelFor(GuideAccess access) {
        if (access == null) {
            return null;
        }
        if (GuideAccess.KIND_CODEX.equals(access.kind()) && GuideTheme.TABLET_ID.equals(access.themeId())) {
            return MODEL_CODEX_TABLET;
        }
        if (GuideAccess.KIND_CODEX.equals(access.kind())) {
            return MODEL_CODEX;
        }
        return null;
    }
}
