package com.ogatamizuki.elytraslot.mixin.client;

import com.ogatamizuki.elytraslot.ElytraSlot;
import com.ogatamizuki.elytraslot.ElytraSlotMod;
import com.ogatamizuki.elytraslot.FireworkSlot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    /**
     * Creative survival-inventory tab remaps high slot indices to the hotbar row.
     * Keep custom slots at their configured creative coordinates.
     */
    @ModifyArgs(
            method = "selectTab",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"
            )
    )
    private void elytraSlot$useCustomSlotCoordinates(Args args) {
        Slot target = args.get(0);
        if (target instanceof ElytraSlot elytraSlot) {
            int[] pos = ElytraSlotMod.resolveCreativeSlotPositions(elytraSlot.getOwner());
            args.set(2, pos[0]);
            args.set(3, pos[1]);
        } else if (target instanceof FireworkSlot fireworkSlot) {
            int[] pos = ElytraSlotMod.resolveCreativeSlotPositions(fireworkSlot.getOwner());
            args.set(2, pos[2]);
            args.set(3, pos[3]);
        }
    }
}
