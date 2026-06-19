package com.ogatamizuki.elytraslot;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;

public class ElytraSlot extends Slot {
    private final Player player;
    private static final Container DUMMY = new SimpleContainer(1);

    public ElytraSlot(Player player, int x, int y) {
        super(DUMMY, 0, x, y);
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(Items.ELYTRA);
    }

    @Override
    public ItemStack getItem() {
        return this.player.getData(ElytraSlotMod.ELYTRA_SLOT);
    }

    @Override
    public void set(ItemStack stack) {
        this.player.setData(ElytraSlotMod.ELYTRA_SLOT, stack);
        this.setChanged();
        if (!this.player.level().isClientSide() && this.player instanceof ServerPlayer serverPlayer) {
            ElytraSlotMod.syncSlot(serverPlayer, stack);
        }
    }

    @Override
    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    @Override
    public net.minecraft.resources.Identifier getNoItemIcon() {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath("elytra_slot", "container/slot/empty_elytra_slot");
    }


    @Override
    public ItemStack remove(int amount) {

        ItemStack current = this.getItem();
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = current.split(amount);
        this.set(current);
        return result;
    }
}
