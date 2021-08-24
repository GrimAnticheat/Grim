package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.ResyncWorldUtil;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.Ray;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class AntiBucketDesync extends PacketCheck {

    private static final Material BUCKET = XMaterial.BUCKET.parseMaterial();
    private static final Material WATER_BUCKET = XMaterial.WATER_BUCKET.parseMaterial();
    private static final Material LAVA_BUCKET = XMaterial.LAVA_BUCKET.parseMaterial();
    public boolean resyncBucket = false;
    public boolean resyncEmptyBucket = false;

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
            boolean isEmptyBucket = false;

            ItemStack main = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);
            if (main != null && (main.getType() == WATER_BUCKET || main.getType() == LAVA_BUCKET))
                isBucket = true;
            if (main != null && main.getType() == BUCKET)
                isEmptyBucket = true;

            if (XMaterial.supports(9)) {
                ItemStack off = player.bukkitPlayer.getInventory().getItemInOffHand();
                if (off.getType() == WATER_BUCKET || off.getType() == LAVA_BUCKET)
                    isBucket = true;
                if (off.getType() == BUCKET)
                    isEmptyBucket = true;
            }

            if (isBucket) {
                resyncBucket = true;
            }
            if (isEmptyBucket) {
                resyncEmptyBucket = true;
            }
        }

        if (PacketType.Play.Client.Util.isInstanceOfFlying(packetID)) {
            Vector3d pos = player.packetStateData.lastPacketPosition;

            // Resend the area around the first block the player's look collides with
            if (resyncBucket) {
                resyncBucket = false;

                for (double eyeHeight : player.getPossibleEyeHeights()) {
                    Vector startingPos = new Vector(pos.getX(), pos.getY() + eyeHeight, pos.getZ());
                    Ray trace = new Ray(player, pos.getX(), pos.getY() + eyeHeight, pos.getZ(), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot);
                    Vector endPos = trace.getPointAtDistance(6);

                    List<SimpleCollisionBox> worldBoxes = Collisions.getCollisionBoxes(player, new SimpleCollisionBox(pos.getX(), pos.getY() + eyeHeight, pos.getZ(), endPos.getX(), endPos.getY(), endPos.getZ()).sort());

                    double bestDistance = Double.MAX_VALUE;
                    Vector bestBlock = null;

                    for (SimpleCollisionBox box : worldBoxes) {
                        Vector intersection = box.intersectsRay(trace, 0, 6);
                        if (intersection == null) continue; // Didn't collide

                        double distance = intersection.distanceSquared(startingPos);

                        if (distance < bestDistance) {
                            bestBlock = intersection;
                            bestDistance = distance;
                        }
                    }

                    if (bestBlock == null) return; // No collisions, nothing to worry about

                    SimpleCollisionBox box = new SimpleCollisionBox(bestBlock, bestBlock);
                    ResyncWorldUtil.resyncPositions(player, box.expand(1));
                }
            }

            // Resend water/lava blocks in the player's view angle
            if (resyncEmptyBucket) {
                resyncEmptyBucket = false;

                for (double eyeHeight : player.getPossibleEyeHeights()) {
                    Vector startPos = new Vector(pos.getX(), pos.getY() + eyeHeight, pos.getZ());
                    Ray trace = new Ray(player, pos.getX(), pos.getY() + eyeHeight, pos.getZ(), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot);
                    Vector endPos = trace.getPointAtDistance(6);

                    SimpleCollisionBox box = new SimpleCollisionBox(startPos, endPos).sort();

                    ResyncWorldUtil.resyncPositions(player, GrimMathHelper.floor(box.minX), GrimMathHelper.floor(box.minY), GrimMathHelper.floor(box.minZ),
                            GrimMathHelper.floor(box.maxX), GrimMathHelper.floor(box.maxY), GrimMathHelper.floor(box.maxZ),

                            // Only resend source blocks, other blocks couldn't have been desync'd by this bug
                            state -> {
                                if (!Materials.checkFlag(state.getMaterial(), Materials.WATER) && !Materials.checkFlag(state.getMaterial(), Materials.LAVA))
                                    return false;
                                if (state instanceof MagicBlockState) {
                                    // Source block
                                    return (((MagicBlockState) state).getBlockData() & 0x7) == 0;
                                } else {
                                    BlockData flatData = ((FlatBlockState) state).getBlockData();
                                    return flatData instanceof Levelled && ((Levelled) flatData).getLevel() == ((Levelled) flatData).getMaximumLevel();
                                }
                            });
                }
            }
        }
    }
}
