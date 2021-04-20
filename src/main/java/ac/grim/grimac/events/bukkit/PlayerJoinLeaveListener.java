package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GrimPlayer grimPlayer = new GrimPlayer(player);
        grimPlayer.lastX = player.getLocation().getX();
        grimPlayer.lastY = player.getLocation().getY();
        grimPlayer.lastZ = player.getLocation().getZ();
        grimPlayer.lastXRot = player.getLocation().getYaw();
        grimPlayer.lastYRot = player.getLocation().getPitch();
        grimPlayer.lastSneaking = player.isSneaking();
        grimPlayer.x = player.getLocation().getX();
        grimPlayer.y = player.getLocation().getY();
        grimPlayer.z = player.getLocation().getZ();
        grimPlayer.xRot = player.getLocation().getYaw();
        grimPlayer.yRot = player.getLocation().getPitch();

        GrimAC.playerGrimHashMap.put(event.getPlayer(), new GrimPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerPlaceBlockEvent(BlockPlaceEvent event) {
        Location blockPlaceLocation = event.getBlock().getLocation();
        VoxelShape blockPlaced = c(((CraftWorld) blockPlaceLocation.getWorld()).getHandle().getType(new BlockPosition(blockPlaceLocation.getBlockX(), blockPlaceLocation.getBlockY(), blockPlaceLocation.getBlockZ())),
                new BlockPosition(blockPlaceLocation.getBlockX(), blockPlaceLocation.getBlockY(), blockPlaceLocation.getBlockZ()));

        Bukkit.broadcastMessage(blockPlaced.toString());
    }

    public VoxelShape c(IBlockData iblockdata, BlockPosition blockposition) {
        Block block = iblockdata.getBlock();

        // Shulker boxes reads entity data from the world, which we can't do async
        // What if we use shulkers to determine a player's ping :)
        // TODO: Do something about shulkers because false positives!
        if (block instanceof BlockShulkerBox) {
            return VoxelShapes.b();
        }

        return BlockProperties.getCanCollideWith(block) ? iblockdata.getShape(null, blockposition) : VoxelShapes.a();
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        GrimAC.playerGrimHashMap.remove(event.getPlayer());
    }
}
