package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PostPacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;

public class AntiBucketDesync extends PacketListenerAbstract {

    public static final HashSet<Player> resyncNeeded = new HashSet<>();

    private static final Material BUCKET = XMaterial.BUCKET.parseMaterial();
    private static final Material WATER_BUCKET = XMaterial.WATER_BUCKET.parseMaterial();
    private static final Material LAVA_BUCKET = XMaterial.LAVA_BUCKET.parseMaterial();

    public AntiBucketDesync() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.BLOCK_PLACE || packetID == PacketType.Play.Client.USE_ITEM) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            // 1.17 players don't have this desync, mojang finally managed to patch it
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_17)) return;

            boolean isBucket = false;

            ItemStack main = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);
            if (main != null && (main.getType() == BUCKET || main.getType() == WATER_BUCKET || main.getType() == LAVA_BUCKET))
                isBucket = true;

            if (XMaterial.supports(9)) {
                ItemStack off = player.bukkitPlayer.getInventory().getItemInOffHand();
                if (off.getType() == BUCKET || off.getType() == WATER_BUCKET || off.getType() == LAVA_BUCKET)
                    isBucket = true;
            }

            if (isBucket) {
                resyncNeeded.add(event.getPlayer());
            }
        }
    }

    @Override
    public void onPostPacketPlayReceive(PostPacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (PacketType.Play.Client.Util.isInstanceOfFlying(packetID)) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;
        }
    }
}
