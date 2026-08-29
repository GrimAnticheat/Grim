package ac.grim.grimac.platform.bukkit.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.BlockTranslator;
import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.platform.bukkit.entity.BukkitGrimEntity;
import ac.grim.grimac.platform.bukkit.utils.anticheat.MultiLibUtil;
import ac.grim.grimac.platform.bukkit.utils.convert.BukkitConversionUtils;
import ac.grim.grimac.platform.bukkit.utils.reflection.PaperUtils;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.utils.reflection.ReflectionUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BukkitPlatformPlayer extends BukkitGrimEntity implements PlatformPlayer {

    private static final BukkitAudiences audiences = BukkitAudiences.create(GrimACBukkitLoaderPlugin.LOADER);
    private static final Consumer<@NotNull Player> resyncSharedFlags;

    @Getter
    private final Player bukkitPlayer;
    @Getter
    private final PlatformInventory inventory;

    private final @Nullable User user;

    public BukkitPlatformPlayer(@NotNull Player bukkitPlayer) {
        super(bukkitPlayer);
        this.bukkitPlayer = bukkitPlayer;
        this.inventory = new BukkitPlatformInventory(bukkitPlayer);
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value()) {
            this.user = PacketEvents.getAPI().getPlayerManager().getUser(bukkitPlayer);
        } else {
            this.user = null;
        }
    }

    @Override
    public void kickPlayer(String textReason) {
        bukkitPlayer.kickPlayer(textReason);
    }

    @Override
    public boolean hasPermission(String s) {
        return bukkitPlayer.hasPermission(s);
    }

    @Override
    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return this.bukkitPlayer.hasPermission(new Permission(s, defaultIfUnset ? PermissionDefault.TRUE : PermissionDefault.FALSE));
    }

    @Override
    public void resyncSharedFlags() {
        resyncSharedFlags.accept(bukkitPlayer);
    }

    @Override
    public void sendMessage(String message) {
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            bukkitPlayer.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(Component message) {
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            audiences.player(bukkitPlayer).sendMessage(message);
        }
    }

    @Override
    public boolean isOnline() {
        return bukkitPlayer.isOnline();
    }

    @Override
    public String getName() {
        return bukkitPlayer.getName();
    }

    @Override
    public void updateInventory() {
        bukkitPlayer.updateInventory();
    }

    @Override
    public void closeInventory() {
        bukkitPlayer.closeInventory();
    }

    @Override
    public Vector3d getPosition() {
        if (CAN_USE_DIRECT_GETTERS) {
            return new Vector3d(this.bukkitPlayer.getX(), this.bukkitPlayer.getY(), this.bukkitPlayer.getZ());
        } else {
            org.bukkit.Location location = this.bukkitPlayer.getLocation();
            return new Vector3d(location.getX(), location.getY(), location.getZ());
        }
    }

    @Override
    public @Nullable GrimEntity getVehicle() {
        return bukkitPlayer.getVehicle() == null ? null : new BukkitGrimEntity(bukkitPlayer.getVehicle());
    }

    @Override
    public GameMode getGameMode() {
        return SpigotConversionUtil.fromBukkitGameMode(bukkitPlayer.getGameMode());
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        bukkitPlayer.setGameMode(SpigotConversionUtil.toBukkitGameMode(gameMode));
    }

    public World getBukkitWorld() {
        return bukkitPlayer.getWorld();
    }

    @Override
    public UUID getUniqueId() {
        return bukkitPlayer.getUniqueId();
    }

    @Override
    public boolean eject() {
        return bukkitPlayer.eject();
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        org.bukkit.Location bLoc = BukkitConversionUtils.toBukkitLocation(location);
        return PaperUtils.teleportAsync(this.bukkitPlayer, bLoc);
    }

    @Override
    public boolean isExternalPlayer() {
        return MultiLibUtil.isExternalPlayer(this.bukkitPlayer);
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] byteArray) {
        this.bukkitPlayer.sendPluginMessage(GrimACBukkitLoaderPlugin.LOADER, channelName, byteArray);
    }

    @Override
    public Sender getSender() {
        return GrimACBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().map(this.bukkitPlayer);
    }

    @Override
    public BlockTranslator getBlockTranslator() {
        return BlockTranslator.IDENTITY;
    }

    @Override
    @NotNull
    public Player getNative() {
        return this.bukkitPlayer;
    }

    static {
        final ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();

        try {
            final String nmsPackage;

            Class<?> CraftEntity = ReflectionUtils.getClass("org.bukkit.craftbukkit.entity.CraftEntity");
            if (CraftEntity != null) {
                nmsPackage = null;
            } else {
                nmsPackage = Bukkit.getServer().getClass().getPackageName().split("\\.")[3];
                CraftEntity = Class.forName("org.bukkit.craftbukkit." + nmsPackage + ".entity.CraftEntity");
            }

            final Method getHandle = CraftEntity.getMethod("getHandle");
            final Class<?> Entity = getHandle.getReturnType();

            final Method getDataWatcher = Entity.getMethod(nmsPackage != null ? switch (nmsPackage) {
                case "v1_8_R3", "v1_9_R1", "v1_9_R2", "v1_10_R1",
                     "v1_11_R1", "v1_12_R1", "v1_13_R1", "v1_13_R2",
                     "v1_14_R1", "v1_15_R1", "v1_16_R1", "v1_16_R2",
                     "v1_16_R3", "v1_17_R1" -> "getDataWatcher";
                case "v1_18_R1", "v1_18_R2", "v1_19_R1" -> "ai";
                case "v1_19_R2", "v1_20_R2" -> "al";
                case "v1_19_R3", "v1_20_R1" -> "aj";
                case "v1_20_R3" -> "an";
                case "v1_20_R4" -> "ap";
                case "v1_21_R1", "v1_21_R4" -> "ar";
                case "v1_21_R2", "v1_21_R3", "v1_21_R5" -> "au";
                case "v1_21_R6" -> "aC";
                case "v1_21_R7" -> "aD";
                default -> throw new IllegalStateException("You are using an unsupported server version: " + nmsPackage + "/" + version.getReleaseName());
            } : "getEntityData");

            final Class<?> DataWatcher = getDataWatcher.getReturnType();

            final Method markDirty;
            final Object sharedFlagsId;

            if (version.isOlderThan(ServerVersion.V_1_9)) {
                markDirty = DataWatcher.getMethod("update", int.class);
                sharedFlagsId = 0;
            } else {
                Field field = Entity.getDeclaredField(nmsPackage != null ? switch (nmsPackage) {
                    case "v1_9_R1" -> "ax";
                    case "v1_9_R2" -> "ay";
                    case "v1_10_R1", "v1_18_R1" -> "aa";
                    case "v1_11_R1", "v1_12_R1", "v1_17_R1",
                         "v1_18_R2", "v1_19_R1", "v1_19_R2" -> "Z";
                    case "v1_13_R1", "v1_13_R2" -> "ac";
                    case "v1_14_R1" -> "W";
                    case "v1_15_R1", "v1_16_R1" -> "T";
                    case "v1_16_R2", "v1_16_R3" -> "S";
                    case "v1_19_R3", "v1_20_R1" -> "an";
                    case "v1_20_R2", "v1_20_R3" -> "ao";
                    case "v1_20_R4", "v1_21_R1" -> "ap";
                    case "v1_21_R2", "v1_21_R3", "v1_21_R4" -> "am";
                    case "v1_21_R5" -> "az";
                    case "v1_21_R6", "v1_21_R7" -> "aA";
                    default -> throw new IllegalStateException("You are using an unsupported server version: " + nmsPackage + "/" + version.getReleaseName());
                } : "DATA_SHARED_FLAGS_ID");
                field.setAccessible(true);

                markDirty = DataWatcher.getMethod("markDirty", field.getType());
                sharedFlagsId = field.get(null);
            }

            resyncSharedFlags = player -> {
                try {
                    Object handle = getHandle.invoke(player);
                    Object dataWatcher = getDataWatcher.invoke(handle);
                    markDirty.invoke(dataWatcher, sharedFlagsId);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (Throwable t) {
            throw t instanceof RuntimeException e ? e : new RuntimeException(t);
        }
    }
}
