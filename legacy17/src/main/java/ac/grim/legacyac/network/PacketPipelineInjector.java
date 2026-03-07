package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.v1_7_R4.Block;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class PacketPipelineInjector {
    private static final String HANDLER_PREFIX = "glac_legacy_tap_";
    private final LegacyAntiCheatPlugin plugin;
    private final Map<UUID, String> injectedHandlers = new ConcurrentHashMap<UUID, String>();
    private final Set<String> reflectionWarnings = java.util.Collections.synchronizedSet(new HashSet<String>());

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

            channel.pipeline().addBefore("packet_handler", handlerName, new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    onPacketReceive(player, msg);
                    super.channelRead(ctx, msg);
                }

                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    onPacketSend(player, msg);
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

    private void onPacketReceive(Player player, Object packet) {
        String packetName = packet.getClass().getSimpleName();
        if (isMovementPacket(packetName)) {
            long now = System.nanoTime();
            plugin.checks().onInternalPacketEvent(InternalPacketEvent.clientMovement(player, packetName, now));
            tryDispatchMovementFrame(player, packet, packetName, now);
            return;
        }

        if (packetName.equals("PacketPlayInTransaction")) {
            Short actionId = readShortField(packet, "b", "a");
            if (actionId != null) {
                plugin.checks().onInternalPacketEvent(InternalPacketEvent.clientTransactionAck(player, actionId.shortValue(), System.nanoTime()));
            }
            return;
        }

        if (packetName.equals("PacketPlayInUseEntity")) {
            Integer entityId = readIntegerField(packet, "a");
            Object action = readFieldValue(packet, "action", "c");
            boolean attack = action == null || "ATTACK".equals(String.valueOf(action));
            if (entityId != null) {
                plugin.checks().onInternalPacketEvent(InternalPacketEvent.clientUseEntity(player, entityId.intValue(), attack, System.nanoTime()));
            }
            return;
        }

        if (packetName.equals("PacketPlayInBlockPlace")) {
            Integer x = readIntegerField(packet, "c", "a");
            Integer y = readIntegerField(packet, "d", "b");
            Integer z = readIntegerField(packet, "e", "c");
            Integer face = readIntegerField(packet, "face", "d");
            Float cursorX = readFloatField(packet, "f");
            Float cursorY = readFloatField(packet, "g");
            Float cursorZ = readFloatField(packet, "h");
            if (x != null && y != null && z != null && face != null && cursorX != null && cursorY != null && cursorZ != null) {
                plugin.getPlayerData(player).recordClientBlockPlacePacket(x.intValue(), y.intValue(), z.intValue(), face.intValue(),
                        cursorX.floatValue(), cursorY.floatValue(), cursorZ.floatValue());
            }
            return;
        }

        if (packetName.equals("PacketPlayInKeepAlive")) {
            Integer keepAlive = readIntegerField(packet, "a");
            if (keepAlive == null) {
                warnReflectionFailureOnce(packetName, "id");
            }
            plugin.checks().onInternalPacketEvent(InternalPacketEvent.clientKeepAlive(player,
                    keepAlive == null ? null : Long.valueOf(keepAlive.longValue()), System.nanoTime()));
        }
    }

    private void onPacketSend(Player player, Object packet) {
        String packetName = packet.getClass().getSimpleName();

        if (packetName.equals("PacketPlayOutBlockChange")) {
            Integer x = readIntegerField(packet, "a");
            Integer y = readIntegerField(packet, "b");
            Integer z = readIntegerField(packet, "c");
            Object blockValue = readFieldValue(packet, "d", "block");
            Integer blockData = readIntegerField(packet, "e");
            Material material = readMaterialFromNmsBlock(blockValue);
            if (x != null && y != null && z != null && material != null) {
                plugin.getPlayerData(player).queueCompensatedBlockChange(player, x.intValue(), y.intValue(), z.intValue(),
                        material, (byte) (blockData == null ? 0 : blockData.intValue()), "netty:block-change");
            }
            return;
        }

        if (packetName.equals("PacketPlayOutMultiBlockChange") || packetName.equals("PacketPlayOutMapChunk")) {
            Integer chunkX = readIntegerField(packet, "a");
            Integer chunkZ = readIntegerField(packet, "b");
            if (chunkX != null && chunkZ != null) {
                plugin.getPlayerData(player).queueCompensatedChunkRefresh(player, chunkX.intValue(), chunkZ.intValue(), "netty:" + packetName);
            }
            return;
        }

        if (packetName.equals("PacketPlayOutMapChunkBulk")) {
            plugin.getPlayerData(player).preloadCompensatedWorld(player, 2);
            return;
        }

        if (packetName.equals("PacketPlayOutPosition")) {
            Double x = readDoubleField(packet, "a");
            Double y = readDoubleField(packet, "b");
            Double z = readDoubleField(packet, "c");
            if (x != null && y != null && z != null) {
                plugin.checks().onInternalPacketEvent(InternalPacketEvent.serverPosition(player, x.doubleValue(), y.doubleValue(), z.doubleValue(), System.nanoTime()));
            }
            return;
        }

        if (packetName.equals("PacketPlayOutEntityVelocity")) {
            Integer entityId = readIntegerField(packet, "a");
            Integer velX = readIntegerField(packet, "b");
            Integer velY = readIntegerField(packet, "c");
            Integer velZ = readIntegerField(packet, "d");
            if (entityId != null && velX != null && velY != null && velZ != null) {
                if (entityId.intValue() == player.getEntityId()) {
                    plugin.checks().onInternalPacketEvent(
                            InternalPacketEvent.serverEntityVelocity(player, entityId.intValue(), velX.intValue(), velY.intValue(), velZ.intValue(), System.nanoTime()));
                }
            } else {
                warnReflectionFailureOnce(packetName, "entityId/velocity");
            }
        }
    }

    private boolean isMovementPacket(String packetName) {
        return packetName.equals("PacketPlayInFlying")
                || packetName.equals("PacketPlayInPosition")
                || packetName.equals("PacketPlayInLook")
                || packetName.equals("PacketPlayInPositionLook");
    }

    private void tryDispatchMovementFrame(Player player, Object packet, String packetName, long nowNanos) {
        PlayerData data = plugin.getPlayerData(player);
        LocationSnapshot base = LocationSnapshot.from(player);
        boolean hasPosition = packetName.equals("PacketPlayInPosition") || packetName.equals("PacketPlayInPositionLook");
        boolean hasLook = packetName.equals("PacketPlayInLook") || packetName.equals("PacketPlayInPositionLook");

        Double x = hasPosition ? readDoubleField(packet, "x", "a") : Double.valueOf(base.x);
        Double y = hasPosition ? readDoubleField(packet, "y", "b") : Double.valueOf(base.y);
        Double z = hasPosition ? readDoubleField(packet, "z", "c") : Double.valueOf(base.z);
        Float yaw = hasLook ? readFloatField(packet, "yaw", "d") : Float.valueOf(base.yaw);
        Float pitch = hasLook ? readFloatField(packet, "pitch", "e") : Float.valueOf(base.pitch);
        Boolean onGround = readBooleanField(packet, "onGround", "g", "f");
        if (x == null || y == null || z == null || yaw == null || pitch == null || onGround == null) {
            warnReflectionFailureOnce(packetName, "movement-frame");
            return;
        }

        plugin.checks().onInternalPacketEvent(
                InternalPacketEvent.clientMovementEx(player, packetName, nowNanos, hasPosition, yaw.floatValue(), pitch.floatValue()));
        if (hasPosition) {
            data.tryConfirmTeleportSync(x.doubleValue(), y.doubleValue(), z.doubleValue());
        }
        data.updateShadowPosition(x.doubleValue(), y.doubleValue(), z.doubleValue(), onGround.booleanValue());
        plugin.movementFrames().dispatch(player, new MovementFrame(nowNanos, x.doubleValue(), y.doubleValue(), z.doubleValue(),
                yaw.floatValue(), pitch.floatValue(), onGround.booleanValue(), hasPosition, hasLook, toMovementSource(packetName)));
    }

    private MovementFrame.Source toMovementSource(String packetName) {
        if (packetName.equals("PacketPlayInPosition")) {
            return MovementFrame.Source.PACKET_POSITION;
        }
        if (packetName.equals("PacketPlayInLook")) {
            return MovementFrame.Source.PACKET_LOOK;
        }
        return MovementFrame.Source.PACKET_POSITION_LOOK;
    }

    private Integer readIntegerField(Object packet, String... preferredNames) {
        for (String preferredName : preferredNames) {
            Object value = readFieldValue(packet, preferredName);
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Short) {
                return Integer.valueOf(((Short) value).intValue());
            }
            if (value instanceof Byte) {
                return Integer.valueOf(((Byte) value).intValue());
            }
            if (value instanceof Long) {
                return Integer.valueOf(((Long) value).intValue());
            }
        }
        try {
            for (Field field : packet.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (type == int.class || type == Integer.class) {
                    return Integer.valueOf(field.getInt(packet));
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Short readShortField(Object packet, String... preferredNames) {
        for (String preferredName : preferredNames) {
            Object value = readFieldValue(packet, preferredName);
            if (value instanceof Short) {
                return (Short) value;
            }
            if (value instanceof Integer) {
                return Short.valueOf(((Integer) value).shortValue());
            }
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

    private Double readDoubleField(Object packet, String... preferredNames) {
        for (String preferredName : preferredNames) {
            Object value = readFieldValue(packet, preferredName);
            if (value instanceof Double) {
                return (Double) value;
            }
            if (value instanceof Float) {
                return Double.valueOf(((Float) value).doubleValue());
            }
        }
        try {
            for (Field field : packet.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (type == double.class || type == Double.class) {
                    return Double.valueOf(field.getDouble(packet));
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Float readFloatField(Object packet, String... preferredNames) {
        for (String preferredName : preferredNames) {
            Object value = readFieldValue(packet, preferredName);
            if (value instanceof Float) {
                return (Float) value;
            }
            if (value instanceof Double) {
                return Float.valueOf(((Double) value).floatValue());
            }
        }
        try {
            for (Field field : packet.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (type == float.class || type == Float.class) {
                    return Float.valueOf(field.getFloat(packet));
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Boolean readBooleanField(Object packet, String... preferredNames) {
        for (String preferredName : preferredNames) {
            Object value = readFieldValue(packet, preferredName);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        try {
            for (Field field : packet.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (type == boolean.class || type == Boolean.class) {
                    return Boolean.valueOf(field.getBoolean(packet));
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Material readMaterialFromNmsBlock(Object blockValue) {
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

    private static final class LocationSnapshot {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        private LocationSnapshot(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        private static LocationSnapshot from(Player player) {
            org.bukkit.Location location = player.getLocation();
            return new LocationSnapshot(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        }
    }

    private Object readFieldValue(Object packet, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = findField(packet.getClass(), fieldName);
                return field.get(packet);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private void warnReflectionFailureOnce(String packetName, String target) {
        if (!plugin.getConfig().getBoolean("netty.strict-reflection", false)) {
            return;
        }
        String key = packetName + ":" + target;
        if (reflectionWarnings.add(key)) {
            plugin.getLogger().warning("[GLAC] Netty reflection failed for " + packetName + " field(s) " + target + ", degraded packet parsing.");
        }
    }

    private Channel resolveChannel(Player player) throws Exception {
        Object craftPlayer = player;
        Object handle = invoke(craftPlayer, "getHandle");

        Field playerConnectionField = findField(handle.getClass(), "playerConnection");
        Object playerConnection = playerConnectionField.get(handle);

        Field networkManagerField = findField(playerConnection.getClass(), "networkManager");
        Object networkManager = networkManagerField.get(playerConnection);

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
