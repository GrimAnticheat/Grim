package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.combat.EntityBoxCache;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.v1_7_R4.Block;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class ProtocolLibBridgeManager {
    private final LegacyAntiCheatPlugin plugin;
    private final List<PacketListener> listeners = new ArrayList<PacketListener>();
    private final EntityBoxCache entityBoxCache = new EntityBoxCache();
    private final ProtocolLibPacketReader packetReader;
    private ProtocolManager protocolManager;
    private volatile boolean movementCaptureHealthy = true;
    private volatile boolean placeCaptureHealthy = true;
    private volatile boolean combatCaptureHealthy = true;
    private volatile boolean worldCaptureHealthy = true;

    public ProtocolLibBridgeManager(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.packetReader = new ProtocolLibPacketReader(plugin);
    }

    public boolean start() {
        if (!plugin.getConfig().getBoolean("protocollib.enabled", true)) {
            return false;
        }
        if (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            plugin.getLogger().warning("[GLAC] ProtocolLib not found, falling back to Netty injector path.");
            return false;
        }

        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
            registerMovementListener();
            registerAckListener();
            registerUseEntityListener();
            registerServerPositionListener();
            registerVelocityListener();
            registerBadPacketsListeners();
            registerBlockPlaceCaptureListener();
            registerWorldStateListeners();
            plugin.getLogger().info("[GLAC] ProtocolLib packet self-check armed: movement=true, place=true, combat=true, world=true");
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[GLAC] Failed to start ProtocolLib bridge: " + throwable.getMessage());
            return false;
        }
    }

    public double[] resolveEntityBox(Entity entity) {
        return entityBoxCache.getSize(entity);
    }

    public void stop() {
        if (protocolManager != null) {
            for (PacketListener listener : listeners) {
                protocolManager.removePacketListener(listener);
            }
        }
        listeners.clear();
    }

    public boolean isMovementPipelineHealthy() {
        return movementCaptureHealthy;
    }

    public boolean isPlaceCaptureHealthy() {
        return placeCaptureHealthy;
    }

    public boolean isCombatCaptureHealthy() {
        return combatCaptureHealthy;
    }

    public boolean isWorldCaptureHealthy() {
        return worldCaptureHealthy;
    }

    private void registerMovementListener() {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.FLYING,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                PlayerData data = ((LegacyAntiCheatPlugin) plugin).getPlayerData(player);
                PacketType type = event.getPacketType();
                Object handle = event.getPacket().getHandle();

                boolean hasPosition = type == PacketType.Play.Client.POSITION || type == PacketType.Play.Client.POSITION_LOOK;
                boolean hasLook = type == PacketType.Play.Client.LOOK || type == PacketType.Play.Client.POSITION_LOOK;

                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();
                if (hasPosition) {
                    Double packetX = packetReader.readDoubleValue(handle, 0, "x", "a");
                    Double packetY = packetReader.readDoubleValue(handle, 1, "y", "b");
                    Double packetZ = packetReader.readDoubleValue(handle, 2, "z", "c");
                    if (packetX == null || packetY == null || packetZ == null) {
                        packetReader.warnReflectionFailureOnce(type.name(), "movement-position");
                        disableMovementCapture("movement-position");
                        return;
                    }
                    x = packetX.doubleValue();
                    y = packetY.doubleValue();
                    z = packetZ.doubleValue();
                }

                float yaw = player.getLocation().getYaw();
                float pitch = player.getLocation().getPitch();
                if (hasLook) {
                    Float packetYaw = packetReader.readFloatValue(handle, 0, "yaw", "d");
                    Float packetPitch = packetReader.readFloatValue(handle, 1, "pitch", "e");
                    if (packetYaw == null || packetPitch == null) {
                        packetReader.warnReflectionFailureOnce(type.name(), "movement-look");
                        disableMovementCapture("movement-look");
                        return;
                    }
                    yaw = packetYaw.floatValue();
                    pitch = packetPitch.floatValue();
                }

                Boolean onGround = packetReader.readBooleanValue(handle, 0, "onGround", "f", "g");
                if (onGround == null) {
                    packetReader.warnReflectionFailureOnce(type.name(), "movement-ground");
                    disableMovementCapture("movement-ground");
                    return;
                }

                long now = System.nanoTime();
                String packetName = handle == null ? type.name() : handle.getClass().getSimpleName();
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.clientMovementEx(player, packetName, now, x, y, z,
                                onGround.booleanValue(), hasPosition, yaw, pitch));

                if (hasPosition) {
                    data.tryConfirmTeleportSync(x, y, z);
                }

                data.updateShadowPosition(x, y, z, onGround.booleanValue());
                MovementFrame frame = new MovementFrame(now, x, y, z, yaw, pitch, onGround.booleanValue(),
                        hasPosition, hasLook, toMovementSource(type));
                ((LegacyAntiCheatPlugin) plugin).movementFrames().dispatch(player, frame);
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }
    private void registerUseEntityListener() {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Object handle = event.getPacket().getHandle();
                Integer entityId = packetReader.readIntegerValue(handle, 0, "a", "entityId");
                Object action = packetReader.readFieldValue(handle, "action", "c", "b");
                boolean attack = packetReader.isUseEntityAttack(action);
                if (entityId == null) {
                    packetReader.warnReflectionFailureOnce("USE_ENTITY", "entityId");
                    disableCombatCapture("use-entity");
                    return;
                }
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.clientUseEntity(event.getPlayer(), entityId.intValue(), attack, System.nanoTime()));
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }
    private void registerServerPositionListener() {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.POSITION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Object handle = event.getPacket().getHandle();
                Double x = packetReader.readDoubleValue(handle, 0, "a", "x");
                Double y = packetReader.readDoubleValue(handle, 1, "b", "y");
                Double z = packetReader.readDoubleValue(handle, 2, "c", "z");
                if (x == null || y == null || z == null) {
                    packetReader.warnReflectionFailureOnce("SERVER_POSITION", "xyz");
                    disableMovementCapture("server-position");
                    return;
                }
                short anchorTxId = reserveDeferredTransaction(event.getPlayer());
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.serverPosition(event.getPlayer(), x.doubleValue(), y.doubleValue(),
                                z.doubleValue(), anchorTxId, System.nanoTime()));
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }
    private void registerVelocityListener() {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_VELOCITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Object handle = event.getPacket().getHandle();
                Integer entityId = packetReader.readIntegerValue(handle, 0, "a", "entityId");
                Integer vx = packetReader.readIntegerValue(handle, 1, "b", "x");
                Integer vy = packetReader.readIntegerValue(handle, 2, "c", "y");
                Integer vz = packetReader.readIntegerValue(handle, 3, "d", "z");
                if (entityId == null || vx == null || vy == null || vz == null) {
                    packetReader.warnReflectionFailureOnce("ENTITY_VELOCITY", "entityId/velocity");
                    disableCombatCapture("entity-velocity");
                    return;
                }
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.serverEntityVelocity(event.getPlayer(), entityId.intValue(), vx.intValue(), vy.intValue(),
                                vz.intValue(), System.nanoTime()));
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }
    private void registerAckListener() {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.TRANSACTION,
                PacketType.Play.Client.KEEP_ALIVE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Object handle = event.getPacket().getHandle();
                PacketType type = event.getPacketType();
                if (type == PacketType.Play.Client.TRANSACTION) {
                    Short actionId = packetReader.readShortValue(handle, 0, "b", "action", "uid");
                    if (actionId == null) {
                        packetReader.warnReflectionFailureOnce("TRANSACTION", "actionId");
                        return;
                    }
                    ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                            InternalPacketEvent.clientTransactionAck(event.getPlayer(), actionId.shortValue(), System.nanoTime()));
                    return;
                }

                if (type == PacketType.Play.Client.KEEP_ALIVE) {
                    Integer keepAliveId = packetReader.readIntegerValue(handle, 0, "a", "keepAliveId", "id");
                    ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                            InternalPacketEvent.clientKeepAlive(event.getPlayer(),
                                    keepAliveId == null ? null : Long.valueOf(keepAliveId.longValue()), System.nanoTime()));
                }
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }
    private void registerBadPacketsListeners() {
        PacketAdapter heldItemAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.HELD_ITEM_SLOT) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Object handle = event.getPacket().getHandle();
                Integer slot = packetReader.readIntegerValue(handle, 0, "itemInHandIndex", "slot", "a");
                if (slot == null) {
                    packetReader.warnReflectionFailureOnce("HELD_ITEM_SLOT", "slot");
                    return;
                }
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.clientHeldItemChange(event.getPlayer(), slot.intValue(), System.nanoTime()));
            }
        };
        protocolManager.addPacketListener(heldItemAdapter);
        listeners.add(heldItemAdapter);

        PacketAdapter entityActionAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ENTITY_ACTION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Object handle = event.getPacket().getHandle();
                Integer entityId = packetReader.readIntegerValue(handle, 0, "a", "entityId");
                Object actionObj = packetReader.readFieldValue(handle, "action", "animation", "b");
                if (actionObj == null) {
                    actionObj = packetReader.readFirstEnumField(handle, "PLAYERACTION", "ENTITYACTION");
                }
                Integer actionId = packetReader.resolveEntityActionId(actionObj);
                Integer jumpBoost = packetReader.readIntegerValue(handle, 1, "c", "jumpBoost");
                if (jumpBoost == null) {
                    jumpBoost = Integer.valueOf(0);
                }
                if (entityId == null || actionId == null) {
                    packetReader.warnReflectionFailureOnce("ENTITY_ACTION", "entityId/actionId");
                    return;
                }
                boolean isSprint = actionId.intValue() == 4 || actionId.intValue() == 5;
                boolean isSneak = actionId.intValue() == 1 || actionId.intValue() == 2;
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.clientEntityAction(event.getPlayer(), entityId.intValue(), actionId.intValue(),
                                jumpBoost.intValue(), isSprint, isSneak, System.nanoTime()));
            }
        };
        protocolManager.addPacketListener(entityActionAdapter);
        listeners.add(entityActionAdapter);

        try {
            PacketAdapter abilitiesAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ABILITIES) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Object handle = event.getPacket().getHandle();
                    Boolean claimsFlying = packetReader.readBooleanValue(handle, 1, "b", "isFlying", "flying");
                    if (claimsFlying == null) {
                        packetReader.warnReflectionFailureOnce("ABILITIES", "flying");
                        return;
                    }
                    ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                            InternalPacketEvent.clientAbilities(event.getPlayer(), claimsFlying.booleanValue(), System.nanoTime()));
                }
            };
            protocolManager.addPacketListener(abilitiesAdapter);
            listeners.add(abilitiesAdapter);
        } catch (Throwable throwable) {
            plugin.getLogger().info("[GLAC] Abilities packet listener not available: " + throwable.getMessage());
        }

        PacketAdapter blockDigAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Object handle = event.getPacket().getHandle();
                Integer x = packetReader.readIntegerValue(handle, 0, "a", "x");
                Integer y = packetReader.readIntegerValue(handle, 1, "b", "y");
                Integer z = packetReader.readIntegerValue(handle, 2, "c", "z");
                Integer face = packetReader.readIntegerValue(handle, 3, "d", "face");
                Integer action = packetReader.resolveDigAction(handle);
                if (action == null || x == null || y == null || z == null || face == null) {
                    packetReader.warnReflectionFailureOnce("BLOCK_DIG", "coords/action");
                    disablePlaceCapture("block-dig");
                    return;
                }
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.clientBlockDig(event.getPlayer(), x.intValue(), y.intValue(), z.intValue(),
                                face.intValue(), action.intValue(), System.nanoTime()));
            }
        };
        protocolManager.addPacketListener(blockDigAdapter);
        listeners.add(blockDigAdapter);
    }
    private void registerBlockPlaceCaptureListener() {
        try {
            PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.BLOCK_PLACE) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Object handle = event.getPacket().getHandle();
                    Integer x = packetReader.readIntegerValue(handle, 0, "c", "a");
                    Integer y = packetReader.readIntegerValue(handle, 1, "d", "b");
                    Integer z = packetReader.readIntegerValue(handle, 2, "e", "c");
                    Integer face = packetReader.readIntegerValue(handle, 3, "face", "d");
                    Float cursorX = packetReader.readFloatValue(handle, 0, "f");
                    Float cursorY = packetReader.readFloatValue(handle, 1, "g");
                    Float cursorZ = packetReader.readFloatValue(handle, 2, "h");
                    if (x == null || y == null || z == null || face == null
                            || cursorX == null || cursorY == null || cursorZ == null) {
                        packetReader.warnReflectionFailureOnce("BLOCK_PLACE", "placeCursor");
                        disablePlaceCapture("block-place");
                        return;
                    }
                    ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                            InternalPacketEvent.clientBlockPlace(event.getPlayer(), x.intValue(), y.intValue(),
                                    z.intValue(), face.intValue(), cursorX.floatValue(), cursorY.floatValue(),
                                    cursorZ.floatValue(), System.nanoTime()));
                }
            };
            protocolManager.addPacketListener(adapter);
            listeners.add(adapter);
        } catch (Throwable throwable) {
            plugin.getLogger().info("[GLAC] Block place capture listener not available: " + throwable.getMessage());
        }
    }
    private void registerWorldStateListeners() {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Server.BLOCK_CHANGE,
                PacketType.Play.Server.MULTI_BLOCK_CHANGE,
                PacketType.Play.Server.MAP_CHUNK,
                PacketType.Play.Server.MAP_CHUNK_BULK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlayerData data = ((LegacyAntiCheatPlugin) plugin).getPlayerData(player);
                PacketType type = event.getPacketType();
                Object handle = event.getPacket().getHandle();
                short anchorTxId = reserveDeferredTransaction(player);

                if (type == PacketType.Play.Server.BLOCK_CHANGE) {
                    BlockChangeSnapshot snapshot = readBlockChange(handle);
                    if (snapshot != null) {
                        data.queueCompensatedBlockChange(player, snapshot.x, snapshot.y, snapshot.z,
                                snapshot.material, snapshot.data, anchorTxId,
                                "packet:block-change:" + snapshot.material.name());
                    } else {
                        disableWorldCapture("block-change");
                        data.queueCompensatedChunkRefresh(player, player.getLocation().getBlockX() >> 4,
                                player.getLocation().getBlockZ() >> 4, anchorTxId, "packet:block-change-fallback");
                    }
                    return;
                }

                if (type == PacketType.Play.Server.MULTI_BLOCK_CHANGE || type == PacketType.Play.Server.MAP_CHUNK) {
                    int chunkX = readIntField(handle, 0, "a", "chunkX");
                    int chunkZ = readIntField(handle, 1, "b", "chunkZ");
                    data.queueCompensatedChunkRefresh(player, chunkX, chunkZ, anchorTxId,
                            "packet:" + type.name().toLowerCase());
                    return;
                }

                if (type == PacketType.Play.Server.MAP_CHUNK_BULK) {
                    int[] xs = readIntArrayField(handle, 0, "c", "xChunks");
                    int[] zs = readIntArrayField(handle, 1, "d", "zChunks");
                    if (xs != null && zs != null) {
                        for (int index = 0; index < xs.length && index < zs.length; index++) {
                            data.queueCompensatedChunkRefresh(player, xs[index], zs[index], anchorTxId,
                                    "packet:map_chunk_bulk");
                        }
                    } else {
                        disableWorldCapture("map-chunk-bulk");
                        data.preloadCompensatedWorld(player, 2);
                    }
                }
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }

    private MovementFrame.Source toMovementSource(PacketType type) {
        if (type == PacketType.Play.Client.FLYING) {
            return MovementFrame.Source.PACKET_FLYING;
        }
        if (type == PacketType.Play.Client.POSITION) {
            return MovementFrame.Source.PACKET_POSITION;
        }
        if (type == PacketType.Play.Client.LOOK) {
            return MovementFrame.Source.PACKET_LOOK;
        }
        if (type == PacketType.Play.Client.POSITION_LOOK) {
            return MovementFrame.Source.PACKET_POSITION_LOOK;
        }
        return MovementFrame.Source.PACKET_FLYING;
    }
    private BlockChangeSnapshot readBlockChange(Object handle) {
        if (handle == null) {
            return null;
        }
        int x = readIntField(handle, 0, "a", "x");
        int y = readIntField(handle, 1, "b", "y");
        int z = readIntField(handle, 2, "c", "z");
        Object blockValue = readFieldValue(handle, "d", "block");
        int data = readIntField(handle, 4, "e", "data");
        Material material = materialFromNmsBlock(blockValue);
        if (material == null) {
            material = Material.AIR;
        }
        return new BlockChangeSnapshot(x, y, z, material, (byte) data);
    }

    private Material materialFromNmsBlock(Object blockValue) {
        if (blockValue == null) {
            return null;
        }
        try {
            if (blockValue instanceof Block) {
                int id = Block.getId((Block) blockValue);
                return Material.getMaterial(id);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private int readIntField(Object handle, int fallbackIndex, String... preferredNames) {
        Object direct = readFieldValue(handle, preferredNames);
        if (direct instanceof Integer) {
            return ((Integer) direct).intValue();
        }
        int seen = 0;
        for (Field field : handle.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                if (field.getType() == int.class || field.getType() == Integer.class) {
                    int value = field.getInt(handle);
                    if (seen == fallbackIndex) {
                        return value;
                    }
                    seen++;
                }
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }

    private int[] readIntArrayField(Object handle, int fallbackIndex, String... preferredNames) {
        Object direct = readFieldValue(handle, preferredNames);
        if (direct instanceof int[]) {
            return (int[]) direct;
        }
        int seen = 0;
        for (Field field : handle.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                if (field.getType().isArray() && field.getType().getComponentType() == int.class) {
                    int[] value = (int[]) field.get(handle);
                    if (seen == fallbackIndex) {
                        return value;
                    }
                    seen++;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private Object readFieldValue(Object handle, String... preferredNames) {
        for (String name : preferredNames) {
            try {
                Field field = findField(handle.getClass(), name);
                return field.get(handle);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private Field findField(Class<?> type, String name) throws Exception {
        Class<?> search = type;
        while (search != null) {
            try {
                Field field = search.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                search = search.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private short reserveDeferredTransaction(final Player player) {
        if (plugin.transactionSync() == null || player == null) {
            return 0;
        }
        final PlayerData data = plugin.getPlayerData(player);
        final short actionId = data.nextTransactionActionId();
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                plugin.transactionSync().sendReservedTransaction(player, actionId);
            }
        });
        return actionId;
    }

    private void disableMovementCapture(String reason) {
        if (!movementCaptureHealthy) {
            return;
        }
        movementCaptureHealthy = false;
        plugin.getLogger().warning("[GLAC] ProtocolLib movement capture degraded: " + reason
                + ". Falling back to Bukkit movement pipeline.");
    }

    private void disablePlaceCapture(String reason) {
        if (!placeCaptureHealthy) {
            return;
        }
        placeCaptureHealthy = false;
        plugin.getLogger().warning("[GLAC] ProtocolLib block-place capture degraded: " + reason
                + ". Falling back to Bukkit place checks.");
    }

    private void disableCombatCapture(String reason) {
        if (!combatCaptureHealthy) {
            return;
        }
        combatCaptureHealthy = false;
        plugin.getLogger().warning("[GLAC] ProtocolLib combat capture degraded: " + reason
                + ". Falling back to event-driven combat checks.");
    }

    private void disableWorldCapture(String reason) {
        if (!worldCaptureHealthy) {
            return;
        }
        worldCaptureHealthy = false;
        plugin.getLogger().warning("[GLAC] ProtocolLib world-state capture degraded: " + reason
                + ". Falling back to direct Bukkit world state.");
    }

    private static final class BlockChangeSnapshot {
        private final int x;
        private final int y;
        private final int z;
        private final Material material;
        private final byte data;

        private BlockChangeSnapshot(int x, int y, int z, Material material, byte data) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
            this.data = data;
        }
    }
}
