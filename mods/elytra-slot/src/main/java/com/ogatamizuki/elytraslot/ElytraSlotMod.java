package com.ogatamizuki.elytraslot;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ElytraSlotMod.MODID)
public class ElytraSlotMod {
    public static final String MODID = "elytra_slot";
    public static final Logger LOGGER = LogManager.getLogger(ElytraSlotMod.class);

    // Attachment type registration
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> ELYTRA_SLOT =
            ATTACHMENT_TYPES.register("elytra_item", () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.OPTIONAL_CODEC.fieldOf("item"))
                    .build());

    public ElytraSlotMod(IEventBus modEventBus) {
        LOGGER.info("Elytra Slot Mod Initializing...");

        ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Register Sync Payload
        registrar.playToClient(
                ElytraSlotSyncPayload.TYPE,
                ElytraSlotSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.world.entity.player.Player targetPlayer = context.player().level()
                                .getPlayerByUUID(payload.playerUuid());
                        if (targetPlayer != null) {
                            targetPlayer.setData(ELYTRA_SLOT, payload.elytraItem());
                        }
                    });
                });
    }

    // Handle player death drops
    @SubscribeEvent
    public void onLivingDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) {
                ItemStack stack = player.getData(ELYTRA_SLOT);
                if (!stack.isEmpty()) {
                    player.drop(stack.copy(), true, false);
                    player.setData(ELYTRA_SLOT, ItemStack.EMPTY);
                    syncSlot(player, ItemStack.EMPTY);
                }
            }
        }
    }

    // Keep elytra slot content on dimensions change or keepInventory=true on death
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer oldPlayer && event.getEntity() instanceof ServerPlayer newPlayer) {
            if (!event.isWasDeath() || oldPlayer.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) {
                ItemStack stack = oldPlayer.getData(ELYTRA_SLOT);
                newPlayer.setData(ELYTRA_SLOT, stack.copy());
            }
        }
    }




    // Server-side synchronization when a player logs in
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = player.getData(ELYTRA_SLOT);
            if (!stack.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new ElytraSlotSyncPayload(player.getUUID(), stack));
            }
        }
    }


    // Sync when tracking starts (so other clients render the elytra)
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer tracker) {
            ItemStack stack = targetPlayer.getData(ELYTRA_SLOT);
            if (!stack.isEmpty()) {
                PacketDistributor.sendToPlayer(tracker, new ElytraSlotSyncPayload(targetPlayer.getUUID(), stack));
            }
        }
    }

    // Helper to sync to tracking players and self
    public static void syncSlot(ServerPlayer player, ItemStack stack) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ElytraSlotSyncPayload(player.getUUID(), stack));
    }
}
