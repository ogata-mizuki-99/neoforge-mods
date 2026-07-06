package com.ogatamizuki.elytraslot.client;

import com.ogatamizuki.elytraslot.Config;
import com.ogatamizuki.elytraslot.CustomSlotVisibility;
import com.ogatamizuki.elytraslot.client.gui.ConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.resources.Identifier;

public class ElytraSlotModClient {

    public static void init(ModContainer container) {
        CustomSlotVisibility.setCheck(CustomAttachmentSlots::shouldRender);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new ConfigScreen(parent));
        container.getEventBus().addListener(ElytraSlotModClient::onRegisterGuiLayers);
        container.getEventBus().addListener(ElytraSlotModClient::onRegisterKeyMappings);
        container.getEventBus().addListener(ElytraSlotModClient::onConfigLoad);
    }

    private static void onConfigLoad(ModConfigEvent.Loading event) {
        if (!"elytra_slot-client.toml".equals(event.getConfig().getFileName())) {
            return;
        }
        migrateLegacyCreativeSlotPositions();
    }

    /** Old creative defaults overlapped the player preview; bump saved values once. */
    private static void migrateLegacyCreativeSlotPositions() {
        if (Config.CREATIVE_ELYTRA_SLOT_X.get() == 116
                && Config.CREATIVE_ELYTRA_SLOT_Y.get() == 26
                && Config.CREATIVE_FIREWORK_SLOT_X.get() == 116
                && Config.CREATIVE_FIREWORK_SLOT_Y.get() == 8) {
            Config.CREATIVE_ELYTRA_SLOT_X.set(126);
            Config.CREATIVE_ELYTRA_SLOT_Y.set(33);
            Config.CREATIVE_FIREWORK_SLOT_X.set(126);
            Config.CREATIVE_FIREWORK_SLOT_Y.set(6);
            Config.CREATIVE_ELYTRA_SLOT_X.save();
            Config.CREATIVE_ELYTRA_SLOT_Y.save();
            Config.CREATIVE_FIREWORK_SLOT_X.save();
            Config.CREATIVE_FIREWORK_SLOT_Y.save();
        }
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath("elytra_slot", "elytra_hud"),
                ElytraHudRenderer::render
        );
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(KeyMappings.CATEGORY);
        event.register(KeyMappings.QUICK_SWAP_KEY);
    }
}
