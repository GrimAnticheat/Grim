package ac.grim.grimac.platform.bukkit.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.bukkit.player.BukkitPlatformPlayer;
import ac.grim.grimac.platform.bukkit.utils.reflection.PaperUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class BukkitItemResetHandler implements ItemResetHandler {
    // resets item usage, then returns whether the player was using an item
    private final @NotNull ItemUsageReset resetItemUsage = createItemUsageResetFunction();

    @SneakyThrows
    public void resetItemUsage(@Nullable PlatformPlayer player) {
        if (player != null) {
            resetItemUsage.accept(((BukkitPlatformPlayer) player).getNative());
        }
    }

    @SneakyThrows
    private @NotNull ItemUsageReset createItemUsageResetFunction() {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        if (version.isNewerThan(ServerVersion.V_1_17) && PaperUtils.PAPER) {
            if (version.isOlderThan(ServerVersion.V_1_19) || GrimAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
                return LivingEntity::clearActiveItem;
            }
            Method setLivingEntityFlag = Class.forName(version.isOlderThan(ServerVersion.V_1_20_5) ? "net.minecraft.world.entity.EntityLiving" : "net.minecraft.world.entity.LivingEntity")
                    .getDeclaredMethod(version.isOlderThan(ServerVersion.V_1_20_5) ? "c" : "setLivingEntityFlag", int.class, boolean.class);
            Method getHandle = (version.isOlderThan(ServerVersion.V_1_20_5)
                    ? Class.forName("org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackageName().split("\\.")[3] + ".entity.CraftPlayer")
                    : Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer")
            ).getMethod("getHandle");

            setLivingEntityFlag.setAccessible(true);

            return player -> {
                // don't trigger gameevents
                setLivingEntityFlag.invoke(getHandle.invoke(player), 1, false);
                player.clearActiveItem();
            };
        }

        if (version == ServerVersion.V_1_8_8) {
            Class<?> EntityHuman = Class.forName("net.minecraft.server.v1_8_R3.EntityHuman");
            Method getHandle = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer").getMethod("getHandle");
            Method clearActiveItem = EntityHuman.getMethod("bV");
            Method isUsingItem = EntityHuman.getMethod("bS");

            return player -> {
                Object handle = getHandle.invoke(player);
                clearActiveItem.invoke(handle);

                // in 1.8 we need to resync item usage manually,
                // only do so if the player was using an item
                if ((boolean) isUsingItem.invoke(handle)) {
                    player.updateInventory();
                }
            };
        }

        String nmsPackage = Bukkit.getServer().getClass().getPackageName().split("\\.")[3];
        String livingEntityPackage = version.isNewerThan(ServerVersion.V_1_16_5) ? "net.minecraft.world.entity.EntityLiving" : "net.minecraft.server." + nmsPackage + ".EntityLiving";
        Method getHandle = Class.forName("org.bukkit.craftbukkit." + nmsPackage + ".entity.CraftPlayer").getMethod("getHandle");
        Method clearActiveItem = Class.forName(livingEntityPackage).getMethod(
            switch (nmsPackage) {
                case "v1_9_R1" -> "cz";
                case "v1_9_R2" -> "cA";
                case "v1_10_R1" -> "cE";
                case "v1_11_R1" -> "cF";
                case "v1_12_R1" -> "cN";
                case "v1_13_R1", "v1_13_R2" -> "da";
                case "v1_14_R1" -> "dp";
                case "v1_15_R1" -> "dH";
                case "v1_16_R1", "v1_16_R2", "v1_16_R3", "v1_17_R1" -> "clearActiveItem";
                case "v1_18_R1" -> "eR";
                case "v1_18_R2" -> "eS";
                default -> throw new IllegalStateException("You are using an unsupported server version! (" + version.getReleaseName() + ")");
            }
        );

        return player -> clearActiveItem.invoke(getHandle.invoke(player));
    }

    private interface ItemUsageReset {
        void accept(@NotNull Player player) throws Throwable;
    }
}
