package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.combat.EntityBoxCache;
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
                PlayerData data = plugin.getPlayerData(player);
                PacketType type = event.getPacketType();
                PacketContainer packet = event.getPacket();

                data.setLastRawMovementPacketAt(System.nanoTime());
                data.incrementRawMovementPacketCounter();

                boolean hasPosition = type == PacketType.Play.Client.POSITION || type == PacketType.Play.Client.POSITION_LOOK;
                if (!hasPosition) {
                    return;
                }

                if (packet.getDoubles().size() < 3) {
                    return;
                }

                double x = packet.getDoubles().read(0);
                double y = packet.getDoubles().read(1);
                double z = packet.getDoubles().read(2);
                if (type == PacketType.Play.Client.POSITION_LOOK) {
                    data.tryConfirmTeleportSync(x, y, z);
                }
                boolean onGround = packet.getBooleans().size() > 0 && packet.getBooleans().read(0);
                data.updateShadowPosition(x, y, z, onGround);

                long maxAckAge = plugin.getConfig().getLong("transaction.max-ack-age-ms", 4000L);
                boolean confirmed = data.hasRecentTransactionAck(maxAckAge) || data.hasRecentKeepAliveAck(maxAckAge);
                data.setMovementUnconfirmed(data.isTeleportSyncPending() || !confirmed);
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
                plugin.checks().onUseEntityAttackPacket(event.getPlayer(), entityId);
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
                PlayerData data = plugin.getPlayerData(event.getPlayer());
                data.beginTeleportSync(packet.getDoubles().read(0), packet.getDoubles().read(1), packet.getDoubles().read(2));
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
                PlayerData data = plugin.getPlayerData(event.getPlayer());
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
