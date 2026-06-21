package com.ogatamizuki.nickname;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Mod(NicknameMod.MODID)
public class NicknameMod {
    public static final String MODID = "nickname";
    public static final Logger LOGGER = LogManager.getLogger(NicknameMod.class);

    public NicknameMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Nickname Mod Initializing...");

        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // サーバー → クライアントへの同期パケット
        registrar.playToClient(
                NicknameSyncPayload.TYPE,
                NicknameSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        NicknameStorage.setNickname(payload.playerUuid(), payload.nickname());

                        // 表示名をキャッシュから再構成
                        net.minecraft.world.entity.player.Player targetPlayer = context.player().level()
                                .getPlayerByUUID(payload.playerUuid());
                        if (targetPlayer != null) {
                            targetPlayer.refreshDisplayName();
                        }
                    });
                });

        registrar.playToClient(
                NicknameClearAllPayload.TYPE,
                NicknameClearAllPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        NicknameStorage.clear();
                        for (net.minecraft.world.entity.player.Player targetPlayer : context.player().level().players()) {
                            targetPlayer.refreshDisplayName();
                        }
                    });
                });
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        NicknameStorage.load(event.getServer());
        LOGGER.info("Loaded nicknames data: {} entries", NicknameStorage.getNicknames().size());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        NicknameStorage.save(event.getServer());
        LOGGER.info("Saved nicknames data.");
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        // ログイン直後に Tab 上の他プレイヤー表示名（radial-teleport 等）を安定させる
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            String nick = NicknameStorage.getNickname(online.getUUID());
            if (nick != null && !nick.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new NicknameSyncPayload(online.getUUID(), nick));
            }
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        // 他のプレイヤーが描画範囲に入った（トラッキング開始した）際、そのプレイヤーのニックネームを同期
        if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer tracker) {
            String nick = NicknameStorage.getNickname(targetPlayer.getUUID());
            if (nick != null && !nick.isEmpty()) {
                PacketDistributor.sendToPlayer(tracker, new NicknameSyncPayload(targetPlayer.getUUID(), nick));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerNameFormat(PlayerEvent.NameFormat event) {
        String nick = NicknameStorage.getNickname(event.getEntity().getUUID());
        if (nick != null && !nick.isEmpty()) {
            event.setDisplayname(Component.literal(nick));
        }
    }

    @SubscribeEvent
    public void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        String nick = NicknameStorage.getNickname(event.getEntity().getUUID());
        if (nick != null && !nick.isEmpty()) {
            event.setDisplayName(Component.literal(nick));
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("nickname")
                        .requires(source -> true) // OP権限不要、全プレイヤー実行可能
                        .then(Commands.literal("clear")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        changeNickname(player, "");
                                        return 1;
                                    }
                                    return 0;
                                }))
                        .then(Commands.literal("clearall")
                                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) // OP権限が必要
                                .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    NicknameStorage.clear();
                                    NicknameStorage.saveAsync(server);

                                    // 全プレイヤーの表示名およびTABリスト名をリフレッシュ
                                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                        player.refreshDisplayName();
                                        player.refreshTabListName();
                                    }

                                    // 全クライアントに一括クリアを同期
                                    PacketDistributor.sendToAllPlayers(new NicknameClearAllPayload());

                                    context.getSource().sendSuccess(
                                            () -> Component.translatable("nickname.message.cleared_all").withStyle(ChatFormatting.GREEN),
                                            true);
                                    return 1;
                                }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                                String newName = StringArgumentType.getString(context, "name");
                                                changeNickname(player, newName);
                                                return 1;
                                            }
                                            return 0;
                                        })))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                     if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                         String newName = StringArgumentType.getString(context, "name");
                                         changeNickname(player, newName);
                                         return 1;
                                     }
                                     return 0;
                                 }))
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                // 引数なしの場合はニックネーム解除
                                changeNickname(player, "");
                                return 1;
                            }
                            return 0;
                        }));
    }

    private void changeNickname(ServerPlayer player, String newName) {
        UUID uuid = player.getUUID();
        NicknameValidation.Result validation = NicknameValidation.validate(newName);
        if (!validation.accepted()) {
            String errorKey = validation.errorKey().orElse("nickname.message.invalid");
            if (validation.errorArg() > 0) {
                player.sendSystemMessage(
                        Component.translatable(errorKey, validation.errorArg()).withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(Component.translatable(errorKey).withStyle(ChatFormatting.RED));
            }
            return;
        }

        String nameToSet = validation.sanitized();

        NicknameStorage.setNickname(uuid, nameToSet);
        if (nameToSet.isEmpty()) {
            player.sendSystemMessage(Component.translatable("nickname.message.reset").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable("nickname.message.set", nameToSet).withStyle(ChatFormatting.GREEN));
        }

        // 表示名およびTABリスト名をリフレッシュ
        player.refreshDisplayName();
        player.refreshTabListName();

        // サーバー側の保存データを更新
        var server = player.level().getServer();
        if (server != null) {
            NicknameStorage.saveAsync(server);
        }

        // 全クライアントに新しいニックネームを同期
        PacketDistributor.sendToAllPlayers(new NicknameSyncPayload(uuid, nameToSet));
    }


}
