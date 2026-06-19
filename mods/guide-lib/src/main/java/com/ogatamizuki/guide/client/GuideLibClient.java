package com.ogatamizuki.guide.client;

import com.ogatamizuki.guide.GuideBookLoader;
import com.ogatamizuki.guide.GuideBookRegistry;
import com.ogatamizuki.guide.GuideLibMod;
import com.ogatamizuki.guide.GuideManualLoader;
import com.ogatamizuki.guide.GuideManualRegistry;
import com.ogatamizuki.guide.GuideThemeLoader;
import com.ogatamizuki.guide.GuideThemeRegistry;
import com.ogatamizuki.guide.client.screen.CodexScreen;
import com.ogatamizuki.guide.client.screen.GuideBookScreen;
import com.ogatamizuki.guide.client.GuideClientSounds;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

public final class GuideLibClient {
    private GuideLibClient() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(GuideLibClient::onAddClientReloadListeners);
    }

    private static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(GuideBookLoader.LISTENER_ID, new GuideBookLoader());
        event.addListener(GuideThemeLoader.LISTENER_ID, new GuideThemeLoader());
        event.addListener(GuideManualLoader.LISTENER_ID, new GuideManualLoader());
        GuideRecipeCache.register(event);
    }

    public static void openCodex(Screen parent, Identifier bookId) {
        openCodex(parent, bookId, GuideTheme.BOOK_ID);
    }

    public static void openCodex(Screen parent, Identifier bookId, Identifier themeId) {
        ensureDataLoaded();
        GuideTheme theme = GuideThemeRegistry.resolve(themeId);
        playOpenSound(theme);
        Minecraft mc = Minecraft.getInstance();
        if (bookId != null) {
            mc.setScreen(new GuideBookScreen(parent, bookId, theme));
            return;
        }
        mc.setScreen(new CodexScreen(parent, theme));
    }

    public static void openBook(Screen parent, Identifier bookId) {
        openBook(parent, bookId, GuideTheme.BOOK_ID, false);
    }

    public static void openBook(Screen parent, Identifier bookId, Identifier themeId) {
        openBook(parent, bookId, themeId, false);
    }

    public static void openBook(Screen parent, Identifier bookId, Identifier themeId, boolean closeOnBack) {
        ensureDataLoaded();
        GuideTheme theme = GuideThemeRegistry.resolve(themeId);
        playOpenSound(theme);
        Minecraft.getInstance().setScreen(new GuideBookScreen(parent, bookId, theme, closeOnBack));
    }

    public static void ensureDataLoaded() {
        Minecraft mc = Minecraft.getInstance();
        var resourceManager = mc.getResourceManager();
        if (GuideBookRegistry.isEmpty()) {
            GuideBookRegistry.setBooks(GuideBookLoader.loadBooks(resourceManager));
        }
        if (GuideThemeRegistry.isEmpty()) {
            GuideThemeRegistry.setThemes(GuideThemeLoader.loadThemes(resourceManager));
        }
        if (GuideManualRegistry.isEmpty()) {
            GuideManualRegistry.setManuals(GuideManualLoader.loadManuals(resourceManager));
        }
    }

    public static void playOpenSound(GuideTheme theme) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && theme != null) {
            GuideClientSounds.play(mc.player, theme.openSound(), 0.8F, 1.0F);
        }
    }

    public static void playPageTurnSound(GuideTheme theme) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && theme != null) {
            GuideClientSounds.play(mc.player, theme.pageTurnSound(), 0.8F, 1.0F);
        }
    }

    public static Component translateOrLiteral(String keyOrText) {
        if (keyOrText == null || keyOrText.isEmpty()) {
            return Component.empty();
        }
        if (keyOrText.startsWith("literal:")) {
            return Component.literal(keyOrText.substring("literal:".length()));
        }
        return Component.translatable(keyOrText);
    }
}
