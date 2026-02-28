package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class PacketPipelineInjector {
    private static final String HANDLER_PREFIX = "glac_legacy_tap_";
    private final LegacyAntiCheatPlugin plugin;
    private final Map<UUID, String> injectedHandlers = new ConcurrentHashMap<UUID, String>();

    public PacketPipelineInjector(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean inject(Player player) {
        try {
            Channel channel = resolveChannel(player);
            if (channel == null || !channel.isOpen()) {
                return false;
            }

            final String handlerName = HANDLER_PREFIX + player.getUniqueId().toString().replace('-', '_');
            if (channel.pipeline().get(handlerName) != null) {
                injectedHandlers.put(player.getUniqueId(), handlerName);
                return true;
            }

            final PlayerData data = plugin.getPlayerData(player);
            channel.pipeline().addBefore("packet_handler", handlerName, new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    onPacketReceive(data, msg);
                    super.channelRead(ctx, msg);
                }

                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    onPacketSend(data, msg);
                    super.write(ctx, msg, promise);
                }
            });

            injectedHandlers.put(player.getUniqueId(), handlerName);
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[GLAC] Failed injecting pipeline for " + player.getName() + ": " + throwable.getMessage());
            return false;
        }
    }

    public void uninject(Player player) {
        try {
            Channel channel = resolveChannel(player);
            if (channel == null) {
                return;
            }
            String handlerName = injectedHandlers.remove(player.getUniqueId());
            if (handlerName != null && channel.pipeline().get(handlerName) != null) {
                channel.pipeline().remove(handlerName);
            }
        } catch (Throwable ignored) {
        }
    }

    public void uninjectAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            uninject(player);
        }
        injectedHandlers.clear();
    }

    private void onPacketReceive(PlayerData data, Object packet) {
        String packetName = packet.getClass().getSimpleName();
        if (packetName.startsWith("PacketPlayInFlying") || packetName.equals("PacketPlayInPosition") || packetName.equals("PacketPlayInLook")) {
            data.setLastRawMovementPacketAt(System.nanoTime());
            data.incrementRawMovementPacketCounter();
            return;
        }

        if (packetName.equals("PacketPlayInTransaction")) {
            Short actionId = readTransactionActionId(packet);
            if (actionId != null) {
                data.acknowledgeTransaction(actionId.shortValue(), System.nanoTime());
            }
        }
    }

    private void onPacketSend(PlayerData data, Object packet) {
        String packetName = packet.getClass().getSimpleName();
        if (packetName.equals("PacketPlayOutPosition")) {
            data.setLastServerPositionSyncAt(System.nanoTime());
        }
    }


    private Short readTransactionActionId(Object packet) {
        try {
            Field byName = findField(packet.getClass(), "b");
            Object value = byName.get(packet);
            if (value instanceof Short) {
                return (Short) value;
            }
            if (value instanceof Integer) {
                return Short.valueOf(((Integer) value).shortValue());
            }
        } catch (Throwable ignored) {
        }

        try {
            for (Field field : packet.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (type == short.class || type == Short.class) {
                    return Short.valueOf(field.getShort(packet));
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private Channel resolveChannel(Player player) throws Exception {
        Object craftPlayer = player;
        Object handle = invoke(craftPlayer, "getHandle");

        Field playerConnectionField = findField(handle.getClass(), "playerConnection");
        Object playerConnection = playerConnectionField.get(handle);

        Field networkManagerField = findField(playerConnection.getClass(), "networkManager");
        Object networkManager = networkManagerField.get(playerConnection);

        // 1.7.10 channel field in NMS NetworkManager is often named 'm'
        Field channelField;
        try {
            channelField = findField(networkManager.getClass(), "m");
        } catch (NoSuchFieldException ignored) {
            channelField = findFieldByType(networkManager.getClass(), Channel.class);
        }
        Object channelObj = channelField.get(networkManager);
        return channelObj instanceof Channel ? (Channel) channelObj : null;
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

    private Field findFieldByType(Class<?> type, Class<?> targetType) throws Exception {
        Class<?> search = type;
        while (search != null) {
            for (Field field : search.getDeclaredFields()) {
                if (targetType.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            }
            search = search.getSuperclass();
        }
        throw new NoSuchFieldException("No field of type " + targetType.getName());
    }

    private Object invoke(Object target, String method) throws Exception {
        return target.getClass().getMethod(method).invoke(target);
    }
}
