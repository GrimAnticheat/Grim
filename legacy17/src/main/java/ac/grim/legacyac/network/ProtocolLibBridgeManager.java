package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.combat.EntityBoxCache;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.network.InternalPacketEvent;
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
            registerBadPacketsListeners();
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

                // Fire extended movement event for BadPackets checks
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                    InternalPacketEvent.clientMovementEx(player, type.name(), System.nanoTime(),
                        hasPosition, yaw, pitch));

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
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(InternalPacketEvent.clientUseEntity(event.getPlayer(), entityId, true, System.nanoTime()));
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
                    InternalPacketEvent.serverPosition(event.getPlayer(), packet.getDoubles().read(0), packet.getDoubles().read(1), packet.getDoubles().read(2), System.nanoTime()));
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
                LegacyAntiCheatPlugin antiCheatPlugin = (LegacyAntiCheatPlugin) plugin;
                long sentAtNanos = System.nanoTime();
                antiCheatPlugin.checks().onInternalPacketEvent(
                    InternalPacketEvent.serverEntityVelocity(event.getPlayer(), entityId, vx, vy, vz, sentAtNanos));
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
                PacketType type = event.getPacketType();
                PacketContainer packet = event.getPacket();

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

    /**
     * Register listeners for BadPackets check packet types:
     * HELD_ITEM_CHANGE, ENTITY_ACTION, CLIENT_COMMAND (abilities), BLOCK_DIG
     */
    private void registerBadPacketsListeners() {
        // HELD_ITEM_CHANGE — BadPacketsA
        PacketAdapter heldItemAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.HELD_ITEM_SLOT) {
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

        // ENTITY_ACTION — BadPacketsF, G, Q
        PacketAdapter entityActionAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.ENTITY_ACTION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                if (packet.getIntegers().size() < 3) {
                    return;
                }
                int entityId = packet.getIntegers().read(0);
                int actionId = packet.getIntegers().read(1);
                int jumpBoost = packet.getIntegers().read(2);
                // actionId: 1=START_SNEAK, 2=STOP_SNEAK, 3=STOP_SLEEPING, 4=START_SPRINT, 5=STOP_SPRINT
                boolean isSprint = (actionId == 4 || actionId == 5);
                boolean isSneak = (actionId == 1 || actionId == 2);
                ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                    InternalPacketEvent.clientEntityAction(event.getPlayer(), entityId, actionId, jumpBoost,
                            isSprint, isSneak, System.nanoTime()));
            }
        };
        protocolManager.addPacketListener(entityActionAdapter);
        listeners.add(entityActionAdapter);

        // CLIENT_COMMAND / ABILITIES — BadPacketsI
        try {
            PacketAdapter abilitiesAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                    PacketType.Play.Client.ABILITIES) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    PacketContainer packet = event.getPacket();
                    if (packet.getBooleans().size() > 1) {
                        // In 1.7, abilities packet has isFlying as the second boolean
                        boolean claimsFlying = packet.getBooleans().read(1);
                        ((LegacyAntiCheatPlugin) plugin).checks().onInternalPacketEvent(
                            InternalPacketEvent.clientAbilities(event.getPlayer(), claimsFlying, System.nanoTime()));
                    }
                }
            };
            protocolManager.addPacketListener(abilitiesAdapter);
            listeners.add(abilitiesAdapter);
        } catch (Throwable t) {
            plugin.getLogger().info("[GLAC] Abilities packet listener not available: " + t.getMessage());
        }

        // BLOCK_DIG — BadPacketsL
        PacketAdapter blockDigAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.BLOCK_DIG) {
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
}
