package com.ogatamizuki.radialteleport.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

final class RadialTeleportDisplayNames {
    private static final String NICKNAME_MOD_ID = "nickname";
    private static final String NICKNAME_STORAGE_CLASS = "com.ogatamizuki.nickname.NicknameStorage";

    private static Method cachedGetNickname;
    private static boolean nicknameLookupInitialized;

    private RadialTeleportDisplayNames() {
    }

    static String resolvePlayerName(Minecraft mc, PlayerInfo info) {
        UUID playerId = info.getProfile().id();
        String profileName = info.getProfile().name();

        Component tabName = info.getTabListDisplayName();
        if (tabName != null) {
            String text = tabName.getString();
            if (!text.isEmpty() && !text.equals(profileName)) {
                return text;
            }
        }

        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(playerId);
            if (player != null) {
                String text = player.getDisplayName().getString();
                if (!text.isEmpty() && !text.equals(profileName)) {
                    return text;
                }
            }
        }

        String nickname = resolveNicknameStorage(playerId);
        if (nickname != null) {
            return nickname;
        }

        if (tabName != null) {
            String text = tabName.getString();
            if (!text.isEmpty()) {
                return text;
            }
        }

        return profileName;
    }

    private static String resolveNicknameStorage(UUID playerId) {
        if (!ModList.get().isLoaded(NICKNAME_MOD_ID)) {
            return null;
        }

        Method getNickname = nicknameLookupMethod();
        if (getNickname == null) {
            return null;
        }

        try {
            Object value = getNickname.invoke(null, playerId);
            if (value instanceof String nickname && !nickname.isEmpty()) {
                return nickname;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private static Method nicknameLookupMethod() {
        if (nicknameLookupInitialized) {
            return cachedGetNickname;
        }

        nicknameLookupInitialized = true;
        try {
            Class<?> storageClass = Class.forName(NICKNAME_STORAGE_CLASS);
            cachedGetNickname = storageClass.getMethod("getNickname", UUID.class);
        } catch (ReflectiveOperationException ignored) {
            cachedGetNickname = null;
        }
        return cachedGetNickname;
    }
}
