package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;

public final class TransactionSyncManager {
    private final LegacyAntiCheatPlugin plugin;
    private int taskId = -1;

    public TransactionSyncManager(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("transaction.enabled", true)) {
            return;
        }
        final int interval = Math.max(1, plugin.getConfig().getInt("transaction.interval-ticks", 20));
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    sendTransaction(player);
                }
            }
        }, interval, interval);
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void sendTransaction(Player player) {
        try {
            PlayerData data = plugin.getPlayerData(player);
            short actionId = data.nextTransactionActionId();

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = findField(handle.getClass(), "playerConnection").get(handle);

            String version = getServerVersionToken();
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTransaction");
            Constructor<?> constructor = packetClass.getConstructor(int.class, short.class, boolean.class);
            Object packet = constructor.newInstance(0, actionId, false);

            Method sendPacket = findMethod(connection.getClass(), "sendPacket", packetClass.getSuperclass());
            if (sendPacket == null) {
                sendPacket = findMethod(connection.getClass(), "sendPacket", packetClass);
            }
            if (sendPacket == null) {
                sendPacket = findMethodByName(connection.getClass(), "sendPacket");
            }
            if (sendPacket == null) {
                return;
            }

            sendPacket.invoke(connection, packet);
            data.markTransactionSent(actionId, System.nanoTime());
        } catch (Throwable throwable) {
            plugin.getLogger().fine("[GLAC] transaction send failed: " + throwable.getMessage());
        }
    }

    private String getServerVersionToken() {
        String[] parts = plugin.getServer().getClass().getPackage().getName().split("\\.");
        return parts[parts.length - 1];
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

    private Method findMethod(Class<?> type, String name, Class<?> paramType) {
        Class<?> search = type;
        while (search != null) {
            for (Method method : search.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].isAssignableFrom(paramType)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            search = search.getSuperclass();
        }
        return null;
    }

    private Method findMethodByName(Class<?> type, String name) {
        Class<?> search = type;
        while (search != null) {
            for (Method method : search.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterTypes().length == 1) {
                    method.setAccessible(true);
                    return method;
                }
            }
            search = search.getSuperclass();
        }
        return null;
    }
}
