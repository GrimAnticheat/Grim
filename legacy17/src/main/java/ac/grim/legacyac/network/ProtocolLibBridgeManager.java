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
    private ProtocolManager protocolManager;

    public ProtocolLibBridgeManager(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
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
                PacketContainer packet = event.getPacket();

                final boolean hasPosition = type == PacketType.Play.Client.POSITION || type == PacketType.Play.Client.POSITION_LOOK;
                final boolean hasLook = type == PacketType.Play.Client.LOOK || type == PacketType.Play.Client.POSITION_LOOK;

                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();
                if (hasPosition) {
                    if (packet.getDoubles().size() < 3) {
                        return;
                    }
                    x = packet.getDoubles().read(0);
                    y = packet.getDoubles().read(1);
                    z = packet.getDoubles().read(2);
                }

                float yaw = player.getLocation().getYaw();
                float pitch = player.getLocation().getPitch();
                if (hasLook && packet.getFloat().size() >= 2) {
                    yaw = packet.getFloat().read(0);
                    pitch = packet.getFloat().read(1);
                }

                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.clientMovementEx(player, type.name(), System.nanoTime(), hasPosition, yaw, pitch));

                if (hasPosition) {
                    data.tryConfirmTeleportSync(x, y, z);
                }

                boolean onGround = packet.getBooleans().size() > 0 && packet.getBooleans().read(0);
                data.updateShadowPosition(x, y, z, onGround);

                MovementFrame.Source source = toMovementSource(type);
                MovementFrame frame = new MovementFrame(System.nanoTime(), x, y, z, yaw, pitch, onGround, hasPosition, hasLook, source);
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
                PacketContainer packet = event.getPacket();
                int entityId = packet.getIntegers().size() > 0 ? packet.getIntegers().read(0) : -1;
                boolean attack = true;
                try {
                    Object handle = packet.getHandle();
                    Object action = readFieldValue(handle, "action", "c");
                    if (action != null) {
                        attack = "ATTACK".equals(String.valueOf(action));
                    }
                } catch (Throwable ignored) {
                }
                if (entityId >= 0) {
                    ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                            InternalPacketEvent.clientUseEntity(event.getPlayer(), entityId, attack, System.nanoTime()));
                }
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }

    private void registerServerPositionListener() {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.POSITION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                if (packet.getDoubles().size() < 3) {
                    return;
                }
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.serverPosition(event.getPlayer(),
                                packet.getDoubles().read(0),
                                packet.getDoubles().read(1),
                                packet.getDoubles().read(2),
                                System.nanoTime()));
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }

    private void registerVelocityListener() {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_VELOCITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                if (packet.getIntegers().size() < 4) {
                    return;
                }
                int entityId = packet.getIntegers().read(0);
                int vx = packet.getIntegers().read(1);
                int vy = packet.getIntegers().read(2);
                int vz = packet.getIntegers().read(3);
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.serverEntityVelocity(event.getPlayer(), entityId, vx, vy, vz, System.nanoTime()));
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
                PacketContainer packet = event.getPacket();
                PacketType type = event.getPacketType();
                if (type == PacketType.Play.Client.TRANSACTION) {
                    if (packet.getShorts().size() > 0) {
                        short actionId = packet.getShorts().read(0);
                        ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                                InternalPacketEvent.clientTransactionAck(event.getPlayer(), actionId, System.nanoTime()));
                    }
                    return;
                }

                if (type == PacketType.Play.Client.KEEP_ALIVE) {
                    Long keepAliveId = null;
                    if (packet.getIntegers().size() > 0) {
                        keepAliveId = Long.valueOf(packet.getIntegers().read(0).longValue());
                    }
                    ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                            InternalPacketEvent.clientKeepAlive(event.getPlayer(), keepAliveId, System.nanoTime()));
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
                PacketContainer packet = event.getPacket();
                if (packet.getIntegers().size() > 0) {
                    int slot = packet.getIntegers().read(0);
                    ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                            InternalPacketEvent.clientHeldItemChange(event.getPlayer(), slot, System.nanoTime()));
                }
            }
        };
        protocolManager.addPacketListener(heldItemAdapter);
        listeners.add(heldItemAdapter);

        PacketAdapter entityActionAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ENTITY_ACTION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                if (packet.getIntegers().size() < 3) {
                    return;
                }
                int entityId = packet.getIntegers().read(0);
                int actionId = packet.getIntegers().read(1);
                int jumpBoost = packet.getIntegers().read(2);
                boolean isSprint = (actionId == 4 || actionId == 5);
                boolean isSneak = (actionId == 1 || actionId == 2);
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                        InternalPacketEvent.clientEntityAction(event.getPlayer(), entityId, actionId, jumpBoost,
                                isSprint, isSneak, System.nanoTime()));
            }
        };
        protocolManager.addPacketListener(entityActionAdapter);
        listeners.add(entityActionAdapter);

        try {
            PacketAdapter abilitiesAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ABILITIES) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    PacketContainer packet = event.getPacket();
                    if (packet.getBooleans().size() > 1) {
                        boolean claimsFlying = packet.getBooleans().read(1);
                        ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                                InternalPacketEvent.clientAbilities(event.getPlayer(), claimsFlying, System.nanoTime()));
                    }
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
                PacketContainer packet = event.getPacket();
                if (packet.getIntegers().size() > 0) {
                    int action = packet.getIntegers().read(0);
                    ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                            InternalPacketEvent.clientBlockDig(event.getPlayer(), action, System.nanoTime()));
                }
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
                    PacketContainer packet = event.getPacket();
                    if (packet.getIntegers().size() < 4 || packet.getFloat().size() < 3) {
                        return;
                    }
                    PlayerData data = ((LegacyAntiCheatPlugin) plugin).getPlayerData(event.getPlayer());
                    data.recordClientBlockPlacePacket(
                            packet.getIntegers().read(0),
                            packet.getIntegers().read(1),
                            packet.getIntegers().read(2),
                            packet.getIntegers().read(3),
                            packet.getFloat().read(0),
                            packet.getFloat().read(1),
                            packet.getFloat().read(2));
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

                if (type == PacketType.Play.Server.BLOCK_CHANGE) {
                    BlockChangeSnapshot snapshot = readBlockChange(handle);
                    if (snapshot != null) {
                        data.queueCompensatedBlockChange(player, snapshot.x, snapshot.y, snapshot.z,
                                snapshot.material, snapshot.data, "packet:block-change:" + snapshot.material.name());
                    } else {
                        data.queueCompensatedChunkRefresh(player, player.getLocation().getBlockX() >> 4,
                                player.getLocation().getBlockZ() >> 4, "packet:block-change-fallback");
                    }
                    return;
                }

                if (type == PacketType.Play.Server.MULTI_BLOCK_CHANGE || type == PacketType.Play.Server.MAP_CHUNK) {
                    int chunkX = readIntField(handle, 0, "a", "chunkX");
                    int chunkZ = readIntField(handle, 1, "b", "chunkZ");
                    data.queueCompensatedChunkRefresh(player, chunkX, chunkZ, "packet:" + type.name().toLowerCase());
                    return;
                }

                if (type == PacketType.Play.Server.MAP_CHUNK_BULK) {
                    int[] xs = readIntArrayField(handle, 0, "c", "xChunks");
                    int[] zs = readIntArrayField(handle, 1, "d", "zChunks");
                    if (xs != null && zs != null) {
                        for (int index = 0; index < xs.length && index < zs.length; index++) {
                            data.queueCompensatedChunkRefresh(player, xs[index], zs[index], "packet:map_chunk_bulk");
                        }
                    } else {
                        data.preloadCompensatedWorld(player, 2);
                    }
                }
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }

    private MovementFrame.Source toMovementSource(PacketType type) {
        if (type == PacketType.Play.Client.POSITION) {
            return MovementFrame.Source.PACKET_POSITION;
        }
        if (type == PacketType.Play.Client.LOOK) {
            return MovementFrame.Source.PACKET_LOOK;
        }
        if (type == PacketType.Play.Client.POSITION_LOOK) {
            return MovementFrame.Source.PACKET_POSITION_LOOK;
        }
        return MovementFrame.Source.PACKET_POSITION_LOOK;
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
