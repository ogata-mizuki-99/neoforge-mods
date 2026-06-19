package com.ogatamizuki.guide;

import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GuideLibMod.MODID)
public class GuideLibMod {
    public static final String MODID = "guide_lib";
    public static final Logger LOGGER = LogManager.getLogger(GuideLibMod.class);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CODEX_OPEN = registerSound("codex_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CODEX_PAGE = registerSound("codex_page");
    public static final DeferredHolder<SoundEvent, SoundEvent> TABLET_OPEN = registerSound("tablet_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> TABLET_BEEP = registerSound("tablet_beep");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register(
            "guide_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guide_lib"))
                    .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .icon(() -> GuideItems.createCodex(GuideTheme.BOOK_ID))
                    .displayItems((parameters, output) -> {
                        output.accept(GuideItems.createCodex(GuideTheme.BOOK_ID));
                        output.accept(GuideItems.createCodexTablet());
                    })
                    .build()
    );

    public GuideLibMod(IEventBus modEventBus) {
        LOGGER.info("Guide Lib Mod Initializing...");

        GuideLibDataComponents.REGISTRAR.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(this::onAddServerReloadListeners);
        NeoForge.EVENT_BUS.register(GuideItemUseHandler.class);
        NeoForge.EVENT_BUS.register(GuideLegacyItemMigrator.class);

        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            GuideLibClient.init(modEventBus);
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, name)));
    }

    private void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(GuideBookLoader.LISTENER_ID, new GuideBookLoader());
        event.addListener(GuideThemeLoader.LISTENER_ID, new GuideThemeLoader());
        event.addListener(GuideManualLoader.LISTENER_ID, new GuideManualLoader());
    }

    public static void openBook(Identifier bookId) {
        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            GuideLibClient.openBook(null, bookId);
        }
    }
}
