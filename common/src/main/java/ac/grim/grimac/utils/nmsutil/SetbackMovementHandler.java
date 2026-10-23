package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.PlatformPlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class SetbackMovementHandler {

    private static final Access ACCESS;

    static {
        try {
            ClassLoader loader = SetbackMovementHandler.class.getClassLoader();
            ACCESS = new Access(
                    Class.forName("net.minecraft.server.level.ServerPlayer", false, loader),
                    Class.forName("net.minecraft.network.PacketListener", false, loader),
                    Class.forName("net.minecraft.network.protocol.game.ServerGamePacketListener", false, loader),
                    Class.forName("net.minecraft.network.protocol.Packet", false, loader),
                    Class.forName("net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket", false, loader),
                    GrimAPI.INSTANCE.getPlatform() == Platform.FOLIA ? Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer", false, loader) : null
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot resolve the 26.3 setback movement handler", exception);
        }
    }

    public static void queue(PlatformPlayer platformPlayer, int teleportId, double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, boolean vehicle) {
        try {
            Object platformHandle = platformPlayer.getNative();
            Object nativePlayer;
            try {
                nativePlayer = platformHandle.getClass().getMethod("getHandle").invoke(platformHandle);
            } catch (NoSuchMethodException ignored) {
                nativePlayer = platformHandle;
            }

            ACCESS.queue(nativePlayer, platformPlayer, teleportId, posX, posY, posZ, yaw, pitch, onGround, vehicle);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot queue a confirmed setback movement", exception);
        }
    }

    private static final class Access {

        private final Field connection;
        private final Field server;
        private final Method packetProcessor;
        private final Method schedule;
        private final Method schedulePlayerPacket;
        private final Method move;
        private final Method resetPosition;
        private final Constructor<?> confirmation;
        private final Class<?> listenerType;

        private Access(Class<?> playerType, Class<?> baseListenerType, Class<?> listenerType, Class<?> packetType, Class<?> confirmationType, Class<?> platformPlayerType) throws ReflectiveOperationException {
            this.listenerType = listenerType;

            this.connection = field(playerType, "connection");
            if (platformPlayerType != null) {
                this.server = null;
                this.packetProcessor = null;
                this.schedule = null;
                this.schedulePlayerPacket = platformPlayerType.getMethod("schedulePacket", baseListenerType, packetType);
            } else {
                this.server = field(this.connection.getType(), "server");
                this.packetProcessor = this.server.getType().getMethod("packetProcessor");
                this.schedule = this.packetProcessor.getReturnType().getMethod("scheduleIfPossible", baseListenerType, packetType);
                this.schedulePlayerPacket = null;
            }

            this.move = this.connection.getType().getDeclaredMethod("handlePlayerPositionChange", double.class, double.class, double.class, float.class, float.class, boolean.class, boolean.class);
            this.move.setAccessible(true);
            this.resetPosition = this.connection.getType().getMethod("resetPosition");

            this.confirmation = confirmationType.getConstructor(int.class, double.class, double.class, double.class, float.class, float.class);
        }

        private void queue(Object player, PlatformPlayer platformPlayer, int teleportId, double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, boolean vehicle) throws ReflectiveOperationException {
            Object handler = this.connection.get(player);
            Object packet = this.confirmation.newInstance(teleportId, posX, posY, posZ, yaw, pitch); // create a packet to queue

            // create a proxy so we can simulate accept packet without checking teleport id etc.
            Object listener = Proxy.newProxyInstance(this.listenerType.getClassLoader(), new Class<?>[]{this.listenerType}, (proxy, method, args) -> {
                try {
                    if (method.getName().equals("handleAcceptTeleportPacket")) {
                        if (vehicle) {
                            GrimEntity riding = platformPlayer.getVehicle();
                            if (riding != null) riding.eject();

                            // make sure that vanilla won't setback us after vehicle eject
                            this.resetPosition.invoke(handler);
                        }

                        // invoke handlePlayerPositionChange as vanilla teleport would
                        return this.move.invoke(handler, posX, posY, posZ, yaw, pitch, onGround, false);
                    }
                    return method.invoke(handler, args);
                } catch (InvocationTargetException exception) {
                    throw exception.getCause();
                }
            });

            // schedule it
            if (this.schedulePlayerPacket != null) {
                this.schedulePlayerPacket.invoke(platformPlayer.getNative(), listener, packet);
            } else {
                this.schedule.invoke(this.packetProcessor.invoke(this.server.get(handler)), listener, packet);
            }
        }

        private static Field field(Class<?> type, String name) throws NoSuchFieldException {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    // ignored
                }
            }

            throw new NoSuchFieldException(type.getName() + "." + name);
        }

    }

}
