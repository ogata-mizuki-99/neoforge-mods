package com.ogatamizuki.radialteleport;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

@Mod(RadialTeleportMod.MODID)
public class RadialTeleportMod {
    public static final String MODID = "radial_teleport";
    public static final Logger LOGGER = LogManager.getLogger(RadialTeleportMod.class);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);

    public static final DeferredItem<TeleportCompassItem> TELEPORT_COMPASS = ITEMS.registerItem(
            "teleport_compass",
            props -> new TeleportCompassItem(props.stacksTo(1))
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register(
            "teleport_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.radial_teleport"))
                    .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .icon(() -> TELEPORT_COMPASS.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(TELEPORT_COMPASS.get()))
                    .build()
    );

    public RadialTeleportMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Radial Teleport Mod Initializing...");

        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        CONDITION_CODECS.register("crafting_recipe_enabled", () -> CraftingRecipeEnabledCondition.CODEC);
        CONDITION_CODECS.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::onConfigReload);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(this);
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != Config.SPEC) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        server.execute(() -> server.reloadResources(server.getPackRepository().getSelectedIds())
                .thenRun(() -> LOGGER.info("Reloaded datapacks after radial_teleport config change")));
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // サーバー → クライアント: チャンネル定義（Dedicated Server 側必須）
        registrar.playToClient(TeleportDestinationsPayload.TYPE, TeleportDestinationsPayload.STREAM_CODEC);
        registrar.playToClient(TeleportResultPayload.TYPE, TeleportResultPayload.STREAM_CODEC);
        registrar.playToClient(WaypointListPayload.TYPE, WaypointListPayload.STREAM_CODEC);

        registrar.playToServer(
                RequestDestinationsPayload.TYPE,
                RequestDestinationsPayload.STREAM_CODEC,
                this::handleRequestDestinations
        );

        registrar.playToServer(
                TeleportRequestPayload.TYPE,
                TeleportRequestPayload.STREAM_CODEC,
                this::handleTeleportRequest
        );

        registrar.playToServer(
                WaypointActionPayload.TYPE,
                WaypointActionPayload.STREAM_CODEC,
                this::handleWaypointAction
        );
    }

    private void handleRequestDestinations(RequestDestinationsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!isHoldingCompass(player)) {
                return;
            }

            var server = player.level().getServer();
            if (server == null) {
                return;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    TeleportService.buildDestinations(server, player)
            );
        });
    }

    private void handleWaypointAction(WaypointActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            switch (payload.action()) {
                case WaypointActionPayload.ACTION_SAVE -> {
                    if (!isHoldingCompass(player)) {
                        return;
                    }
                    WaypointService.saveAtPlayer(player, payload.argument());
                }
                case WaypointActionPayload.ACTION_DELETE -> {
                    if (!isHoldingCompass(player)) {
                        return;
                    }
                    if (WaypointService.deleteByDestinationId(player, payload.argument())) {
                        sendWaypointList(player);
                    }
                }
                case WaypointActionPayload.ACTION_OPEN_EDIT -> {
                    if (!player.isUsingItem() || !player.getUseItem().is(TELEPORT_COMPASS.get())) {
                        return;
                    }
                    sendWaypointList(player);
                }
                case WaypointActionPayload.ACTION_RENAME -> {
                    if (!isHoldingCompass(player)) {
                        return;
                    }
                    String[] renameParts = splitWaypointArgument(payload.argument());
                    if (renameParts.length == 2
                            && WaypointService.renameByDestinationId(player, renameParts[0], renameParts[1])) {
                        sendWaypointList(player);
                    }
                }
                case WaypointActionPayload.ACTION_MOVE -> {
                    if (!isHoldingCompass(player)) {
                        return;
                    }
                    String[] moveParts = splitWaypointArgument(payload.argument());
                    if (moveParts.length == 2) {
                        boolean up = "up".equals(moveParts[1]);
                        if (WaypointService.moveByDestinationId(player, moveParts[0], up)) {
                            sendWaypointList(player);
                        }
                    }
                }
                default -> {
                }
            }
        });
    }

    private static void sendWaypointList(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, WaypointService.buildListPayload(player));
    }

    private static String[] splitWaypointArgument(String argument) {
        int separator = argument.indexOf(WaypointActionPayload.ARG_SEPARATOR);
        if (separator < 0) {
            return new String[0];
        }
        return new String[]{
                argument.substring(0, separator),
                argument.substring(separator + 1)
        };
    }

    private static boolean isHoldingCompass(ServerPlayer player) {
        return player.getMainHandItem().is(TELEPORT_COMPASS.get())
                || player.getOffhandItem().is(TELEPORT_COMPASS.get());
    }

    private void handleTeleportRequest(TeleportRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.isUsingItem() || !player.getUseItem().is(TELEPORT_COMPASS.get())) {
                PacketDistributor.sendToPlayer(
                        player,
                        TeleportResultPayload.message(false, "radial_teleport.message.not_using_item")
                );
                return;
            }

            TeleportResultPayload result = TeleportService.teleport(player, payload.destinationId());
            PacketDistributor.sendToPlayer(player, result);

            if (result.success()) {
                player.stopUsingItem();
            }
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("radialteleport")
                        .then(Commands.literal("give")
                                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> giveCompass(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")
                                        ))
                                )
                        )
        );
    }

    private static int giveCompass(CommandSourceStack source, Collection<ServerPlayer> targets)
            throws CommandSyntaxException {
        ItemStack stack = TELEPORT_COMPASS.get().getDefaultInstance();

        for (ServerPlayer target : targets) {
            ItemStack copy = stack.copy();
            if (!target.getInventory().add(copy)) {
                target.drop(copy, false);
            } else {
                target.inventoryMenu.broadcastChanges();
            }
        }

        source.sendSuccess(() -> Component.translatable("radial_teleport.message.given"), true);
        return targets.size();
    }
}
