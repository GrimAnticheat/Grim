package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.Ray;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AntiUseItemDesync extends PacketCheck {

    private static final Material BUCKET = XMaterial.BUCKET.parseMaterial();
    private static final Material SCAFFOLDING = XMaterial.SCAFFOLDING.parseMaterial();
    private static final Material LILY_PAD = XMaterial.LILY_PAD.parseMaterial();

    public boolean resyncBucket = false;
    public boolean resyncEmptyBucket = false;
    public boolean resyncScaffolding = false;
    public boolean resyncLilyPad = false;

    public AntiUseItemDesync(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.BLOCK_PLACE || packetID == PacketType.Play.Client.USE_ITEM) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            // All these items can cause ghost blocks, thank you mojang!
            boolean isBucket = false;
            boolean isEmptyBucket = false;
            boolean isScaffolding = false;
            boolean isLilyPad = false;

            ItemStack main = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);
            if (main != null && Materials.isPlaceableLiquidBucket(main.getType()))
                isBucket = true;
            if (main != null && main.getType() == BUCKET)
                isEmptyBucket = true;
            if (main != null && main.getType() == SCAFFOLDING)
                isScaffolding = true;
            if (main != null && main.getType() == LILY_PAD)
                isLilyPad = true;

            if (XMaterial.supports(9)) {
                ItemStack off = player.bukkitPlayer.getInventory().getItemInOffHand();
                if (Materials.isPlaceableLiquidBucket(off.getType()))
                    isBucket = true;
                if (off.getType() == BUCKET)
                    isEmptyBucket = true;
                if (off.getType() == SCAFFOLDING)
                    isScaffolding = true;
                if (off.getType() == LILY_PAD)
                    isLilyPad = true;
            }

            // Mojang is incompetent and while this is mostly patched in 1.17, it desync's at high ping.
            resyncBucket = resyncBucket || isBucket;
            resyncEmptyBucket = resyncEmptyBucket || isEmptyBucket;
            resyncScaffolding = resyncScaffolding || isScaffolding;
            resyncLilyPad = resyncLilyPad || isLilyPad;
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
                    List<SimpleCollisionBox> worldBoxes = new ArrayList<>();
                    Collisions.getCollisionBoxes(player, new SimpleCollisionBox(pos.getX(), pos.getY() + eyeHeight, pos.getZ(), endPos.getX(), endPos.getY(), endPos.getZ()).sort(), worldBoxes, false);

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

                    player.getResyncWorldUtil().resyncPositions(player, box.expand(1), true);
                }
            }

            // Resend water/lava blocks in the player's view angle
            if (resyncEmptyBucket) {
                resyncEmptyBucket = false;

                double minEye = Collections.min(player.getPossibleEyeHeights());
                double maxEye = Collections.max(player.getPossibleEyeHeights());

                Vector startPos = new Vector(pos.getX(), pos.getY() + minEye, pos.getZ());
                Ray trace = new Ray(player, pos.getX(), pos.getY() + minEye, pos.getZ(), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot);
                Vector endPos = trace.getPointAtDistance(6);

                SimpleCollisionBox box = new SimpleCollisionBox(startPos, endPos).sort().expandMax(0, maxEye - minEye, 0);

                player.getResyncWorldUtil().resyncPositions(player, GrimMath.floor(box.minX), GrimMath.floor(box.minY), GrimMath.floor(box.minZ),
                        GrimMath.floor(box.maxX), GrimMath.floor(box.maxY), GrimMath.floor(box.maxZ),

                        // Only resend source blocks, other blocks couldn't have been desync'd by this bug
                        pair -> {
                            BaseBlockState state = pair.getFirst();

                            if (!Materials.checkFlag(state.getMaterial(), Materials.WATER) && !Materials.checkFlag(state.getMaterial(), Materials.LAVA))
                                return false;
                            if (state instanceof MagicBlockState) {
                                // Source block
                                return (((MagicBlockState) state).getBlockData() & 0x7) == 0;
                            } else {
                                BlockData flatData = ((FlatBlockState) state).getBlockData();
                                return flatData instanceof Levelled && ((Levelled) flatData).getLevel() == 0;
                            }
                        }, true);
            }

            if (resyncLilyPad) {
                resyncLilyPad = false;

                double minEye = Collections.min(player.getPossibleEyeHeights());
                double maxEye = Collections.max(player.getPossibleEyeHeights());

                Vector startPos = new Vector(pos.getX(), pos.getY() + minEye, pos.getZ());
                Ray trace = new Ray(player, pos.getX(), pos.getY() + minEye, pos.getZ(), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot);
                Vector endPos = trace.getPointAtDistance(6);

                SimpleCollisionBox box = new SimpleCollisionBox(startPos, endPos).sort().expandMax(0, maxEye - minEye, 0);

                player.getResyncWorldUtil().resyncPositions(player, GrimMath.floor(box.minX), GrimMath.floor(box.minY), GrimMath.floor(box.minZ),
                        GrimMath.floor(box.maxX), GrimMath.floor(box.maxY), GrimMath.floor(box.maxZ),

                        // Only resend the blocks above source blocks to solve this bug
                        pair -> {
                            Vector3i position = pair.getSecond();
                            BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(position.getX(), position.getY() - 1, position.getZ());

                            if (!Materials.checkFlag(state.getMaterial(), Materials.WATER) && !Materials.checkFlag(state.getMaterial(), Materials.LAVA))
                                return false;
                            if (state instanceof MagicBlockState) {
                                // Source block
                                return (((MagicBlockState) state).getBlockData() & 0x7) == 0;
                            } else {
                                BlockData flatData = ((FlatBlockState) state).getBlockData();
                                return flatData instanceof Levelled && ((Levelled) flatData).getLevel() == 0;
                            }
                        }, true);
            }

            // You can too easily place stuff on ghost blocks with this, resend all blocks
            if (resyncScaffolding) {
                resyncScaffolding = false;

                double minEye = Collections.min(player.getPossibleEyeHeights());
                double maxEye = Collections.max(player.getPossibleEyeHeights());

                Vector startPos = new Vector(pos.getX(), pos.getY() + minEye, pos.getZ());
                Ray trace = new Ray(player, pos.getX(), pos.getY() + minEye, pos.getZ(), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot);
                Vector endPos = trace.getPointAtDistance(6);

                // Add 1 because you can place blocks in a way to extend your reach
                SimpleCollisionBox box = new SimpleCollisionBox(startPos, endPos).sort().expandMax(0, maxEye - minEye, 0).expand(1);

                player.getResyncWorldUtil().resyncPositions(player, GrimMath.floor(box.minX), GrimMath.floor(box.minY), GrimMath.floor(box.minZ),
                        GrimMath.floor(box.maxX), GrimMath.floor(box.maxY), GrimMath.floor(box.maxZ), state -> true, true);
            }
        }
    }
}
