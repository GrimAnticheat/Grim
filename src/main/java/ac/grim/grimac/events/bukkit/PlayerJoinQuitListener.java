package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    public static boolean isViaLegacyUpdated = true;

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerJoinEvent(PlayerJoinEvent event) {
        Player bukkitPlayer = event.getPlayer();

        if (PacketEvents.get().getPlayerUtils().isGeyserPlayer(bukkitPlayer)) return;

        GrimPlayer player = new GrimPlayer(bukkitPlayer);

        // We can't send transaction packets to this player, disable the anticheat for them
        if (!isViaLegacyUpdated && player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_16_4)) {
            LogUtil.warn(ChatColor.RED + "Please update ViaBackwards to 4.0.2 or newer");
            LogUtil.warn(ChatColor.RED + "An important packet is broken for 1.16 and below clients on this ViaBackwards version");
            LogUtil.warn(ChatColor.RED + "Disabling all checks for 1.16 and below players as otherwise they WILL be falsely banned");
            LogUtil.warn(ChatColor.RED + "Supported version: " + ChatColor.WHITE + "https://github.com/ViaVersion/ViaBackwards/actions/runs/1039987269");
            return;
        }

        player.playerWorld = bukkitPlayer.getLocation().getWorld();
        player.x = bukkitPlayer.getLocation().getX();
        player.y = bukkitPlayer.getLocation().getY();
        player.z = bukkitPlayer.getLocation().getZ();
        player.xRot = bukkitPlayer.getLocation().getYaw();
        player.yRot = bukkitPlayer.getLocation().getPitch();
        player.isDead = bukkitPlayer.isDead();

        player.lastX = bukkitPlayer.getLocation().getX();
        player.lastY = bukkitPlayer.getLocation().getY();
        player.lastZ = bukkitPlayer.getLocation().getZ();
        player.lastXRot = bukkitPlayer.getLocation().getYaw();
        player.lastYRot = bukkitPlayer.getLocation().getPitch();

        player.onGround = bukkitPlayer.isOnGround();
        player.lastOnGround = bukkitPlayer.isOnGround();
        player.packetStateData.packetPlayerOnGround = bukkitPlayer.isOnGround();

        player.packetStateData.packetPosition = new Vector3d(bukkitPlayer.getLocation().getX(), bukkitPlayer.getLocation().getY(), bukkitPlayer.getLocation().getZ());
        player.packetStateData.packetPlayerXRot = bukkitPlayer.getLocation().getYaw();
        player.packetStateData.packetPlayerYRot = bukkitPlayer.getLocation().getPitch();

        player.packetStateData.lastPacketPosition = new Vector3d(bukkitPlayer.getLocation().getX(), bukkitPlayer.getLocation().getY(), bukkitPlayer.getLocation().getZ());
        player.packetStateData.lastPacketPlayerXRot = bukkitPlayer.getLocation().getYaw();
        player.packetStateData.lastPacketPlayerYRot = bukkitPlayer.getLocation().getPitch();

        player.packetStateData.gameMode = bukkitPlayer.getGameMode();

        player.uncertaintyHandler.pistonPushing.add(0d);
        player.uncertaintyHandler.collidingEntities.add(0);

        player.getSetbackTeleportUtil().setSafeTeleportPositionFromTeleport(new Vector3d(player.x, player.y, player.z));

        player.boundingBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.6, 1.8);
        GrimAPI.INSTANCE.getPlayerDataManager().addPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerQuitEvent(PlayerQuitEvent event) {
        GrimAPI.INSTANCE.getPlayerDataManager().remove(event.getPlayer());
    }
}
