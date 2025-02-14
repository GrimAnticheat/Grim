package ac.grim.grimac.utils.nmsutil;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

@UtilityClass
public class BukkitNMS {
    // resets item usage, then returns whether the player was using an item
    private final @NotNull ItemUsageReset resetItemUsage = createItemUsageResetFunction();

    @SneakyThrows
    public void resetItemUsage(@Nullable Player player) {
        if (player != null) {
            resetItemUsage.accept(player);
        }
    }

    @SneakyThrows
    private @NotNull ItemUsageReset createItemUsageResetFunction() {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();

        if (version.isNewerThanOrEquals(ServerVersion.V_1_17)) {
            if (version.isOlderThan(ServerVersion.V_1_19)) {
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
        Method getHandle = Class.forName("org.bukkit.craftbukkit." + nmsPackage + ".entity.CraftPlayer").getMethod("getHandle");
        Method clearActiveItem = Class.forName("net.minecraft.server." + nmsPackage + ".EntityLiving").getMethod(
                switch (nmsPackage) {
                    case "v1_9_R1" -> "cz";
                    case "v1_9_R2" -> "cA";
                    case "v1_10_R1" -> "cE";
                    case "v1_11_R1" -> "cF";
                    case "v1_12_R1" -> "cN";
                    case "v1_13_R1", "v1_13_R2" -> "da";
                    case "v1_14_R1" -> "dp";
                    case "v1_15_R1" -> "dH";
                    case "v1_16_R1", "v1_16_R2", "v1_16_R3" -> "clearActiveItem";
                    default -> throw new IllegalStateException("You are using an unsupported server version! (" + version.getReleaseName() + ")");
                }
        );

        return player -> clearActiveItem.invoke(getHandle.invoke(player));
    }

    private interface ItemUsageReset {
        void accept(@NotNull Player player) throws Throwable;
    }
}
