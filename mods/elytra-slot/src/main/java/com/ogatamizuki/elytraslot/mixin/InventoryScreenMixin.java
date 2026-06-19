package com.ogatamizuki.elytraslot.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {

    private InventoryScreenMixin() {
        super(null, null, null, Component.empty());
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void onExtractBackground(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // Draw the slot box border at leftPos+76, topPos+25 (18x18; elytra slot at 77,26; above offhand ~77,62)
        int x = this.leftPos + 76;
        int y = this.topPos + 25;

        // Draw sunken slot border
        gui.fill(x, y, x + 18, y + 18, 0xFF8B8B8B); // Background
        gui.fill(x, y, x + 18, y + 1, 0xFF373737); // Top border
        gui.fill(x, y, x + 1, y + 18, 0xFF373737); // Left border
        gui.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF); // Bottom border
        gui.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF); // Right border
    }
}
