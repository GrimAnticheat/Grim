package ac.grim.grimac.utils.nmsutil;

import com.github.retrooper.packetevents.PacketEvents;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Predicate;

@UtilityClass
public class BukkitNMS {
    // resets item usage, then returns whether the player was using an item
    private final @NotNull Predicate<@NotNull Player> resetActiveBukkitItem;

    public void resetBukkitItemUsage(@Nullable Player player) {
        if (player != null && resetActiveBukkitItem.test(player)) {
            // only update if they were using an item to prevent certain issues
            player.updateInventory();
        }
    }

    static {
        try {
            switch (PacketEvents.getAPI().getServerManager().getVersion()) {
                case V_1_8_8 -> {
                    Class<?> EntityHuman = Class.forName("net.minecraft.server.v1_8_R3.EntityHuman");
                    Method getHandle = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer").getMethod("getHandle");
                    Method clearActiveItem = EntityHuman.getMethod("bV");
                    Method isUsingItem = EntityHuman.getMethod("bS");

                    resetActiveBukkitItem = player -> {
                        try {
                            Object handle = getHandle.invoke(player);
                            clearActiveItem.invoke(handle);
                            return (boolean) isUsingItem.invoke(handle);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                }
                case V_1_12_2 -> {
                    Class<?> EntityLiving = Class.forName("net.minecraft.server.v1_12_R1.EntityLiving");
                    Method getHandle = Class.forName("org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer").getMethod("getHandle");
                    Method clearActiveItem = EntityLiving.getMethod("cN");
                    Method getItemInUse = EntityLiving.getMethod("cJ");
                    Method isEmpty = Class.forName("net.minecraft.server.v1_12_R1.ItemStack").getMethod("isEmpty");

                    resetActiveBukkitItem = player -> {
                        try {
                            Object handle = getHandle.invoke(player);
                            clearActiveItem.invoke(handle);
                            Object item = getItemInUse.invoke(handle);
                            return item != null && !((boolean) isEmpty.invoke(item));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                }
                case V_1_16_5 -> {
                    Class<?> EntityLiving = Class.forName("net.minecraft.server.v1_16_R3.EntityLiving");
                    Method getHandle = Class.forName("org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer").getMethod("getHandle");
                    Method clearActiveItem = EntityLiving.getMethod("clearActiveItem");
                    Method getItemInUse = EntityLiving.getMethod("getActiveItem");
                    Method isEmpty = Class.forName("net.minecraft.server.v1_16_R3.ItemStack").getMethod("isEmpty");

                    resetActiveBukkitItem = player -> {
                        try {
                            Object handle = getHandle.invoke(player);
                            clearActiveItem.invoke(handle);
                            Object item = getItemInUse.invoke(handle);
                            return item != null && !((boolean) isEmpty.invoke(item));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                }
                default -> {
                    // cause an exception if these methods don't exist
                    Player.class.getMethod("clearActiveItem");
                    Player.class.getMethod("getItemInUse");

                    resetActiveBukkitItem = player -> {
                        player.clearActiveItem();
                        return player.getItemInUse() != null;
                    };
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException("you are likely using an unsupported server software and or version!", e);
        }
    }
}
