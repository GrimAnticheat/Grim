package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.combat.EntityBoxCache;
import ac.grim.legacyac.network.frame.MovementFrame;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import java.util.ArrayList;
import java.util.List;
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

                data.setLastRawMovementPacketAt(System.nanoTime());
                data.incrementRawMovementPacketCounter();

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

                if (hasPosition) {
                    data.tryConfirmTeleportSync(x, y, z);
                }

                boolean onGround = packet.getBooleans().size() > 0 && packet.getBooleans().read(0);
                data.updateShadowPosition(x, y, z, onGround);

                long maxAckAge = plugin.getConfig().getLong("transaction.max-ack-age-ms", 4000L);
                boolean confirmed = data.hasRecentTransactionAck(maxAckAge) || data.hasRecentKeepAliveAck(maxAckAge);
                data.setMovementUnconfirmed(data.isTeleportSyncPending() || !confirmed);

                MovementFrame.Source source = toSource(type);
                MovementFrame frame = new MovementFrame(System.nanoTime(), x, y, z, yaw, pitch, onGround, hasPosition, hasLook, source);
                ((LegacyAntiCheatPlugin) plugin).movementFrames().dispatch(player, frame);
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }

    private MovementFrame.Source toSource(PacketType type) {
        if (type == PacketType.Play.Client.FLYING) {
            return MovementFrame.Source.PACKET_FLYING;
        }
        if (type == PacketType.Play.Client.POSITION) {
            return MovementFrame.Source.PACKET_POSITION;
        }
        if (type == PacketType.Play.Client.LOOK) {
            return MovementFrame.Source.PACKET_LOOK;
        }
        return MovementFrame.Source.PACKET_POSITION_LOOK;
    }

    private void registerUseEntityListener() {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                if (packet.getEntityUseActions().size() > 0) {
                    EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);
                    if (action != EnumWrappers.EntityUseAction.ATTACK) {
                        return;
                    }
                }
                if (packet.getIntegers().size() <= 0) {
                    return;
                }
                int entityId = packet.getIntegers().read(0);
                for (Entity entity : event.getPlayer().getWorld().getEntities()) {
                    if (entity.getEntityId() == entityId) {
                        entityBoxCache.getSize(entity);
                        break;
                    }
                }
                ((LegacyAntiCheatPlugin) plugin).checks().onUseEntityAttackPacket(event.getPlayer(), entityId);
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
                PlayerData data = ((LegacyAntiCheatPlugin) plugin).getPlayerData(event.getPlayer());
                data.beginTeleportSync(packet.getDoubles().read(0), packet.getDoubles().read(1), packet.getDoubles().read(2));
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
                if (packet.getIntegers().size() < 4) return;
                
                int entityId = packet.getIntegers().read(0);
                if (entityId != event.getPlayer().getEntityId()) {
                    return;
                }
                
                int vx = packet.getIntegers().read(1);
                int vy = packet.getIntegers().read(2);
                int vz = packet.getIntegers().read(3);
                final double dx = vx / 8000.0D;
                final double dy = vy / 8000.0D;
                final double dz = vz / 8000.0D;
                
                final org.bukkit.entity.Player player = event.getPlayer();
                
                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;
                        org.bukkit.util.Vector vector = new org.bukkit.util.Vector(dx, dy, dz);
                        org.bukkit.event.player.PlayerVelocityEvent bukkitEvent = new org.bukkit.event.player.PlayerVelocityEvent(player, vector);
                        ((LegacyAntiCheatPlugin) plugin).checks().onVelocity(bukkitEvent);
                    }
                });
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
                PlayerData data = ((LegacyAntiCheatPlugin) plugin).getPlayerData(event.getPlayer());
                PacketType type = event.getPacketType();
                PacketContainer packet = event.getPacket();

                if (type == PacketType.Play.Client.TRANSACTION) {
                    if (packet.getShorts().size() > 0) {
                        short actionId = packet.getShorts().read(0);
                        data.acknowledgeTransaction(actionId, System.nanoTime());
                    }
                    return;
                }

                if (type == PacketType.Play.Client.KEEP_ALIVE) {
                    data.acknowledgeKeepAlive(System.currentTimeMillis());
                }
            }
        };
        protocolManager.addPacketListener(adapter);
        listeners.add(adapter);
    }
}
