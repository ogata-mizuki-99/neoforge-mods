package com.ogatamizuki.elytraslot.mixin;

import com.ogatamizuki.elytraslot.ElytraSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu {

    private InventoryMenuMixin() {
        super(null, 0, 0, 0);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(Inventory playerInventory, boolean active, Player owner, CallbackInfo ci) {
        // Elytra slot above offhand: clickable at (77, 26); background border drawn at (76, 25)
        this.addSlot(new ElytraSlot(owner, 77, 26));
    }
}
