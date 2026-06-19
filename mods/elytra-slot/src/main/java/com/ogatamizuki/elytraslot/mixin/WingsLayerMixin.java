package com.ogatamizuki.elytraslot.mixin;

import com.ogatamizuki.elytraslot.HumanoidRenderStateExt;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WingsLayer.class)
public class WingsLayerMixin {

    @Redirect(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;chestEquipment:Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack redirectChestEquipment(HumanoidRenderState state) {
        if (state instanceof HumanoidRenderStateExt ext) {
            ItemStack elytra = ext.getElytraSlotItem();
            if (elytra != null && !elytra.isEmpty()) {
                return elytra;
            }
        }
        return state.chestEquipment;
    }
}
