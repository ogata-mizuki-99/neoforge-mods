package com.ogatamizuki.radialteleport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerWaypointStorage extends SavedData {
    private static final Codec<PlayerWaypointStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    PlayerWaypoint.CODEC.listOf()
            ).fieldOf("players").forGetter(storage -> storage.waypointsByPlayer)
    ).apply(instance, PlayerWaypointStorage::new));

    private static final SavedDataType<PlayerWaypointStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(RadialTeleportMod.MODID, "waypoints"),
            PlayerWaypointStorage::new,
            CODEC
    );

    private Map<UUID, List<PlayerWaypoint>> waypointsByPlayer = new LinkedHashMap<>();

    private PlayerWaypointStorage() {
    }

    private PlayerWaypointStorage(Map<UUID, List<PlayerWaypoint>> waypointsByPlayer) {
        this.waypointsByPlayer = new LinkedHashMap<>(waypointsByPlayer);
    }

    public static PlayerWaypointStorage get(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<PlayerWaypoint> getWaypoints(UUID playerId) {
        return List.copyOf(waypointsByPlayer.getOrDefault(playerId, List.of()));
    }

    public boolean addWaypoint(UUID playerId, PlayerWaypoint waypoint, int maxWaypoints) {
        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        if (current.size() >= maxWaypoints) {
            return false;
        }
        if (current.stream().anyMatch(existing -> existing.name().equalsIgnoreCase(waypoint.name()))) {
            return false;
        }
        current.add(waypoint);
        waypointsByPlayer.put(playerId, List.copyOf(current));
        setDirty();
        return true;
    }

    public Optional<PlayerWaypoint> removeWaypoint(UUID playerId, String name) {
        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).name().equalsIgnoreCase(name)) {
                PlayerWaypoint removed = current.remove(i);
                if (current.isEmpty()) {
                    waypointsByPlayer.remove(playerId);
                } else {
                    waypointsByPlayer.put(playerId, List.copyOf(current));
                }
                setDirty();
                return Optional.of(removed);
            }
        }
        return Optional.empty();
    }

    public Optional<PlayerWaypoint> removeWaypointById(UUID playerId, UUID waypointId) {
        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).waypointId().equals(waypointId)) {
                PlayerWaypoint removed = current.remove(i);
                if (current.isEmpty()) {
                    waypointsByPlayer.remove(playerId);
                } else {
                    waypointsByPlayer.put(playerId, List.copyOf(current));
                }
                setDirty();
                return Optional.of(removed);
            }
        }
        return Optional.empty();
    }

    public boolean renameWaypoint(UUID playerId, UUID waypointId, String newName) {
        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        if (current.stream().anyMatch(waypoint -> waypoint.name().equalsIgnoreCase(newName)
                && !waypoint.waypointId().equals(waypointId))) {
            return false;
        }

        for (int i = 0; i < current.size(); i++) {
            PlayerWaypoint existing = current.get(i);
            if (existing.waypointId().equals(waypointId)) {
                current.set(i, new PlayerWaypoint(
                        existing.waypointId(),
                        newName,
                        existing.dimension(),
                        existing.x(),
                        existing.y(),
                        existing.z(),
                        existing.yaw(),
                        existing.pitch()
                ));
                waypointsByPlayer.put(playerId, List.copyOf(current));
                setDirty();
                return true;
            }
        }
        return false;
    }

    public boolean moveWaypoint(UUID playerId, UUID waypointId, int direction) {
        if (direction == 0) {
            return false;
        }

        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        int index = -1;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).waypointId().equals(waypointId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return false;
        }

        int targetIndex = index + direction;
        if (targetIndex < 0 || targetIndex >= current.size()) {
            return false;
        }

        PlayerWaypoint moving = current.remove(index);
        current.add(targetIndex, moving);
        waypointsByPlayer.put(playerId, List.copyOf(current));
        setDirty();
        return true;
    }

    public Optional<PlayerWaypoint> findByDestinationId(UUID playerId, String destinationId) {
        if (!destinationId.startsWith(TeleportService.WAYPOINT_DESTINATION_PREFIX)) {
            return Optional.empty();
        }
        String suffix = destinationId.substring(TeleportService.WAYPOINT_DESTINATION_PREFIX.length());
        try {
            UUID waypointId = UUID.fromString(suffix);
            return getWaypoints(playerId).stream()
                    .filter(waypoint -> waypoint.waypointId().equals(waypointId))
                    .findFirst();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Map<UUID, List<PlayerWaypoint>> view() {
        return Collections.unmodifiableMap(waypointsByPlayer);
    }
}
