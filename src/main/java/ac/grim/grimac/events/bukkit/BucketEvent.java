package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.BasePlayerChangeBlockData;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.PlayerChangeBlockData;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class BucketEvent implements Listener {

    private static final Material LAVA_BUCKET = XMaterial.LAVA_BUCKET.parseMaterial();

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        int trans = MagicPlayerBlockBreakPlace.getPlayerTransactionForBucket(player, player.bukkitPlayer.getLocation());
        Location pos = event.getBlockClicked().getLocation();
        ChangeBlockData data = new ChangeBlockData(trans, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), 0);
        player.compensatedWorld.worldChangedBlockQueue.add(data);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        BasePlayerChangeBlockData data;
        BlockFace face = event.getBlockFace();
        Location pos = event.getBlockClicked().getLocation().add(face.getModX(), face.getModY(), face.getModZ());
        int trans = MagicPlayerBlockBreakPlace.getPlayerTransactionForBucket(player, player.bukkitPlayer.getLocation());

        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13)) {
            BlockData newData;
            if (event.getBucket() == LAVA_BUCKET) {
                newData = Material.LAVA.createBlockData();
            } else {
                newData = Material.WATER.createBlockData();
            }
            data = new PlayerChangeBlockData(trans, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), newData);
        } else {
            int newData;
            if (event.getBucket() == LAVA_BUCKET) {
                newData = Material.LAVA.getId();
            } else {
                newData = Material.WATER.getId();
            }
            data = new ChangeBlockData(trans, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), newData);
        }

        player.compensatedWorld.worldChangedBlockQueue.add(data);
    }
}
