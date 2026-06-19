package com.ogatamizuki.deconstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DeconstructorBlockEntity extends BlockEntity implements MenuProvider {
    @SuppressWarnings("removal")
    private final net.neoforged.neoforge.items.ItemStackHandler inventory = new net.neoforged.neoforge.items.ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final int maxExtractCount;

    // NeoForge 互換のコンストラクタ
    public DeconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(DeconstructorMod.DECONSTRUCTOR_BLOCK_ENTITY_TYPE.get(), pos, state);
        this.maxExtractCount = state.is(DeconstructorMod.PRECISION_DECONSTRUCTOR.get()) ? 3 : 1;
    }

    public DeconstructorBlockEntity(BlockPos pos, BlockState state, int maxExtractCount) {
        super(DeconstructorMod.DECONSTRUCTOR_BLOCK_ENTITY_TYPE.get(), pos, state);
        this.maxExtractCount = maxExtractCount;
    }

    public ItemStack getInputStack() {
        return inventory.getStackInSlot(0);
    }

    public void setInputStack(ItemStack stack) {
        setInputStackIfChanged(stack);
    }

    public void setInputStackIfChanged(ItemStack stack) {
        ItemStack current = inventory.getStackInSlot(0);
        if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() == stack.getCount()) {
            return;
        }
        this.inventory.setStackInSlot(0, stack);
        setChanged();
    }

    public int getMaxExtractCount() {
        return maxExtractCount;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(maxExtractCount == 3 ? "container.deconstructor.precision_deconstructor" : "container.deconstructor.deconstructor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DeconstructorMenu(containerId, playerInventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.getInputStack());
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        this.inventory.serialize(tag.child("input_inventory"));
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        this.inventory.deserialize(tag.childOrEmpty("input_inventory"));
    }
}
