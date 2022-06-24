package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public class ViaBackwardsManager implements Initable {
    public static boolean isViaLegacyUpdated = true;
    public static boolean didViaBreakBlockPredictions = true;

    @Override
    public void start() {
        LogUtil.info("Checking ViaBackwards Compatibility...");

        // We have a more accurate version of this patch
        System.setProperty("com.viaversion.ignorePaperBlockPlacePatch", "true");

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
            // Enable ping -> transaction packet
            System.setProperty("com.viaversion.handlePingsAsInvAcknowledgements", "true");

            // Check if we support this property
            try {
                Plugin viaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion");
                // 1.19 servers don't have via messing with block predictions
                if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_19) && viaVersion != null) {
                    String[] split = viaVersion.getDescription().getVersion().replace("-SNAPSHOT", "").split("\\.");

                    if (split.length == 3) {
                        // 4.3.2 fixes an issue with 1.19 block predictions
                        if (Integer.parseInt(split[0]) < 4 || (Integer.parseInt(split[1]) == 3 && Integer.parseInt(split[2]) < 2)) {
                            didViaBreakBlockPredictions = true;
                        }
                    }
                }

                Plugin viaBackwards = Bukkit.getPluginManager().getPlugin("ViaBackwards");
                if (viaBackwards != null) {
                    String[] split = viaBackwards.getDescription().getVersion().replace("-SNAPSHOT", "").split("\\.");

                    if (split.length == 3) {
                        // If the version is before 4.0.2
                        if (Integer.parseInt(split[0]) < 4 || (Integer.parseInt(split[1]) == 0 && Integer.parseInt(split[2]) < 2)) {
                            Logger logger = GrimAPI.INSTANCE.getPlugin().getLogger();

                            logger.warning(ChatColor.RED + "Please update ViaBackwards to 4.0.2 or newer");
                            logger.warning(ChatColor.RED + "An important packet is broken for 1.16 and below clients on this ViaBackwards version");
                            logger.warning(ChatColor.RED + "Disabling all checks for 1.16 and below players as otherwise they WILL be falsely banned");
                            logger.warning(ChatColor.RED + "Supported  version: " + ChatColor.WHITE + "https://www.spigotmc.org/resources/viabackwards.27448/");

                            isViaLegacyUpdated = false;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
