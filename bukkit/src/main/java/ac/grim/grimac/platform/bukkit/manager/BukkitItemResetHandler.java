package ac.grim.grimac.platform.bukkit.manager;

import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.bukkit.player.BukkitPlatformPlayer;
import ac.grim.grimac.platform.bukkit.utils.reflection.PaperUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class BukkitItemResetHandler implements ItemResetHandler {
    private final @NotNull ItemUsageReset resetItemUsage = createItemUsageResetFunction();
    private final @NotNull ItemUsageHandGetter itemUsageHandGetter = createItemUsageHandGetterFunction();

    @Override
    @SneakyThrows
    public void resetItemUsage(@Nullable PlatformPlayer player) {
        if (player != null) {
            resetItemUsage.accept(((BukkitPlatformPlayer) player).getNative());
        }
    }

    @Override
    @SneakyThrows
    public @Nullable InteractionHand getItemUsageHand(@Nullable PlatformPlayer platformPlayer) {
        return platformPlayer == null ? null
                : itemUsageHandGetter.apply(((BukkitPlatformPlayer) platformPlayer).getNative());
    }

    @SneakyThrows
    private @NotNull ItemUsageReset createItemUsageResetFunction() {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        if (version.isNewerThan(ServerVersion.V_1_17) && PaperUtils.PAPER) {
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
                // no gameevent, no exception
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
        if (version.isOlderThan(ServerVersion.V_1_19)) {
            String livingEntityPackage = version.isNewerThan(ServerVersion.V_1_16_5) ? "net.minecraft.world.entity.EntityLiving" : "net.minecraft.server." + nmsPackage + ".EntityLiving";
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
        } else {
            Class<?> EntityLiving = Class.forName("net.minecraft.world.entity.EntityLiving");
            Method setLivingEntityFlag = EntityLiving.getDeclaredMethod("c", int.class, boolean.class);
            setLivingEntityFlag.setAccessible(true);

            Method clearActiveItem = EntityLiving.getMethod(switch (nmsPackage) {
                case "v1_19_R1" -> "eZ";
                case "v1_19_R2" -> "ff";
                case "v1_19_R3" -> "fk";
                case "v1_20_R1" -> "fo";
                case "v1_20_R2" -> "fs";
                case "v1_20_R3" -> "ft";
                case "v1_20_R4" -> "fB";
                case "v1_21_R1" -> "fx";
                case "v1_21_R2", "v1_21_R3", "v1_21_R4" -> "fF";
                default -> throw new IllegalStateException("You are using an unsupported server version! (" + version.getReleaseName() + ")");
            });

            return player -> {
                final Object handle = getHandle.invoke(player);
                // no gameevent, no exception
                setLivingEntityFlag.invoke(handle, 1, false);
                clearActiveItem.invoke(handle);
            };
        }
    }

    @SneakyThrows
    private @NotNull ItemUsageHandGetter createItemUsageHandGetterFunction() {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        if (version.isNewerThanOrEquals(ServerVersion.V_1_16_5) && PaperUtils.PAPER) {
            return player -> player.isHandRaised()
                    ? player.getHandRaised() == EquipmentSlot.OFF_HAND
                        ? InteractionHand.OFF_HAND
                        : InteractionHand.MAIN_HAND
                    : null;
        }

        if (version == ServerVersion.V_1_8_8) {
            Method getHandle = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer").getMethod("getHandle");
            Method isUsingItem = Class.forName("net.minecraft.server.v1_8_R3.EntityHuman").getMethod("bS");
            return player -> (boolean) isUsingItem.invoke(getHandle.invoke(player)) ? InteractionHand.MAIN_HAND : null;
        }

        String nmsPackage = Bukkit.getServer().getClass().getPackageName().split("\\.")[3];
        Method getHandle = Class.forName("org.bukkit.craftbukkit." + nmsPackage + ".entity.CraftPlayer").getMethod("getHandle");
        Class<?> LivingEntity = Class.forName(version.isNewerThan(ServerVersion.V_1_16_5)
                ? "net.minecraft.world.entity.EntityLiving"
                : "net.minecraft.server." + nmsPackage + ".EntityLiving"
        );

        Method isUsingItem = LivingEntity.getMethod(switch (nmsPackage) {
            case "v1_9_R1" -> "cs";
            case "v1_9_R2" -> "ct";
            case "v1_10_R1" -> "cx";
            case "v1_11_R1", "v1_12_R1", "v1_13_R1", "v1_13_R2", "v1_14_R1",
                 "v1_15_R1", "v1_16_R1", "v1_16_R2", "v1_16_R3", "v1_17_R1" -> "isHandRaised";
            case "v1_18_R1" -> "eL";
            case "v1_18_R2" -> "eM";
            case "v1_19_R1" -> "eT";
            case "v1_19_R2" -> "eZ";
            case "v1_19_R3" -> "fe";
            case "v1_20_R1" -> "fi";
            case "v1_20_R2" -> "fm";
            case "v1_20_R3" -> "fn";
            case "v1_20_R4" -> "fv";
            case "v1_21_R1" -> "fr";
            case "v1_21_R2", "v1_21_R3", "v1_21_R4" -> "fz";
            default -> throw new IllegalStateException("You are using an unsupported server version! (" + version.getReleaseName() + ")");
        });
        Method getUsingItemHand = LivingEntity.getMethod(switch (nmsPackage) {
            case "v1_9_R1" -> "ct";
            case "v1_9_R2" -> "cu";
            case "v1_10_R1" -> "cy";
            case "v1_11_R1" -> "cz";
            case "v1_12_R1" -> "cH";
            case "v1_13_R1", "v1_13_R2", "v1_14_R1" -> "cU";
            case "v1_15_R1", "v1_16_R1", "v1_16_R2", "v1_16_R3", "v1_17_R1" -> "getRaisedHand";
            case "v1_18_R1" -> "eM";
            case "v1_18_R2" -> "eN";
            case "v1_19_R1" -> "eU";
            case "v1_19_R2" -> "fa";
            case "v1_19_R3" -> "ff";
            case "v1_20_R1" -> "fj";
            case "v1_20_R2" -> "fn";
            case "v1_20_R3" -> "fo";
            case "v1_20_R4" -> "fw";
            case "v1_21_R1" -> "fs";
            case "v1_21_R2", "v1_21_R3", "v1_21_R4" -> "fA";
            default -> throw new IllegalStateException("You are using an unsupported server version! (" + version.getReleaseName() + ")");
        });

        return player -> {
            final Object handle = getHandle.invoke(player);
            return (boolean) isUsingItem.invoke(handle)
                    ? ((Enum<?>) getUsingItemHand.invoke(handle)).ordinal() == 0
                        ? InteractionHand.MAIN_HAND
                        : InteractionHand.OFF_HAND
                    : null;
        };
    }

    private interface ItemUsageReset {
        void accept(@NotNull Player player) throws Throwable;
    }

    private interface ItemUsageHandGetter {
        InteractionHand apply(@NotNull Player player) throws Throwable;
    }
}
