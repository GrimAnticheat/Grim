package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.ResyncWorldUtil;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsImplementations.RayTrace;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class AntiBucketDesync extends PacketCheck {

    private static final Material BUCKET = XMaterial.BUCKET.parseMaterial();
    private static final Material WATER_BUCKET = XMaterial.WATER_BUCKET.parseMaterial();
    private static final Material LAVA_BUCKET = XMaterial.LAVA_BUCKET.parseMaterial();
    public static boolean resync = false;

    public AntiBucketDesync(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.BLOCK_PLACE || packetID == PacketType.Play.Client.USE_ITEM) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            // 1.17 players don't have this desync, mojang finally managed to patch it (partially)
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
                resync = true;
            }
        }

        if (PacketType.Play.Client.Util.isInstanceOfFlying(packetID)) {
            if (resync) {
                resync = false;
                Vector3d pos = player.packetStateData.lastPacketPosition;
                RayTrace trace = new RayTrace(player, pos.getX(), pos.getY(), pos.getZ(), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot);
                Vector result = trace.getPostion(6);
                // Brute force eye level
                SimpleCollisionBox box = new SimpleCollisionBox(pos.getX(), pos.getY(), pos.getZ(), result.getX(), result.getY(), result.getZ()).sort().expandMax(0, 2, 0);
                ResyncWorldUtil.resyncPositions(player, box);
            }
        }
    }
}
