package ac.grim.grimac.checks.movement;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.List;

// This check is UNFINISHED!
// TODO: Must make client placed blocks work.
// TODO: If chunk is marked for removal, player could have switched worlds, so exempt
public class NoFall {

    private final GrimPlayer player;

    public NoFall(GrimPlayer player) {
        this.player = player;
    }

    public boolean tickNoFall(PredictionData data) {
        // If the player claims to be on the ground
        if (data.onGround && !data.isJustTeleported) {
            SimpleCollisionBox feetBB;

            feetBB = GetBoundingBox.getBoundingBoxFromPosAndSize(player.packetStateData.packetPlayerX, player.packetStateData.packetPlayerY, player.packetStateData.packetPlayerZ, 0.6, 0.001);
            // Don't expand if the player moved more than 50 blocks this tick (stop netty crash exploit)
            if (new Vector3d(data.playerX, data.playerY, data.playerZ).distanceSquared(new Vector3d(player.packetStateData.packetPlayerX, player.packetStateData.packetPlayerY, player.packetStateData.packetPlayerZ)) < 2500)
                feetBB.expandToCoordinate(data.playerX - player.packetStateData.packetPlayerX, data.playerY - player.packetStateData.packetPlayerY, data.playerZ - player.packetStateData.packetPlayerZ);

            List<SimpleCollisionBox> boxes = Collisions.getCollisionBoxes(player, feetBB);

            for (SimpleCollisionBox box : boxes) {
                if (feetBB.collidesVertically(box)) { // If we collide vertically but aren't in the block
                    return false;
                }
            }

            if (isNearHardEntity(feetBB.expand(4))) return false;

            Bukkit.broadcastMessage(ChatColor.RED + "Player used NoFall! ");
            return true;
        }
        return false;
    }

    // PacketEntities are sync'd to the anticheat thread, not the netty thread
    // This is technically wrong, but it's fine, not taking the complexity/memory usage to do it properly
    private boolean isNearHardEntity(SimpleCollisionBox playerBox) {
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if (entity.type == EntityType.BOAT || entity.type == EntityType.SHULKER) {
                SimpleCollisionBox box = GetBoundingBox.getBoatBoundingBox(entity.position.getX(), entity.position.getY(), entity.position.getZ());
                if (box.isIntersected(playerBox)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkZeroPointZeroThreeGround(boolean onGround) {
        if (onGround) {
            SimpleCollisionBox feetBB = GetBoundingBox.getBoundingBoxFromPosAndSize(player.packetStateData.packetPlayerX, player.packetStateData.packetPlayerY, player.packetStateData.packetPlayerZ, 0.6, 0.001);
            feetBB.expand(0.03); // 0.03 can be in any direction

            List<SimpleCollisionBox> boxes = Collisions.getCollisionBoxes(player, feetBB);

            for (SimpleCollisionBox box : boxes) {
                if (feetBB.isCollided(box)) { // Can't check for intersection, rely on NoClip checks to deal with this.
                    return false;
                }
            }

            if (isNearHardEntity(feetBB.expand(4))) return false;

            Bukkit.broadcastMessage(ChatColor.RED + "Player used NoFall with 0.03!");
            return true;
        }

        return false;
    }
}
