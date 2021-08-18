package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.List;

// This check is UNFINISHED!
// TODO: Must make client placed blocks work.
// TODO: If chunk is marked for removal, player could have switched worlds, so exempt
@CheckData(name = "NoFall")
public class NoFall extends PacketCheck {

    private final GrimPlayer player;

    public NoFall(GrimPlayer player) {
        super(player);
        this.player = player;
    }

    @Override
    public void onPacketReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (PacketType.Play.Client.Util.isInstanceOfFlying(packetID)) {
            WrappedPacketInFlying flying = new WrappedPacketInFlying(event.getNMSPacket());

            // Force teleports to have onGround set to false, might patch NoFall on some version.
            if (player.packetStateData.lastPacketWasTeleport) {
                flying.setOnGround(false);
                return;
            }

            // If the player claims to be on the ground
            if (flying.isOnGround()) {
                boolean hasPosition = packetID == PacketType.Play.Client.POSITION || packetID == PacketType.Play.Client.POSITION_LOOK;

                if (!hasPosition) {
                    checkZeroPointZeroThreeGround(flying.isOnGround());
                    return;
                }

                SimpleCollisionBox feetBB;

                Vector3d position = player.packetStateData.packetPosition;
                Vector3d lastPos = player.packetStateData.lastPacketPosition;

                feetBB = GetBoundingBox.getBoundingBoxFromPosAndSize(position.getX(), position.getY(), position.getZ(), 0.6, 0.001);
                // Don't expand if the player moved more than 50 blocks this tick (stop netty crash exploit)
                if (position.distanceSquared(lastPos) < 2500)
                    feetBB.expandToAbsoluteCoordinates(lastPos.getX(), lastPos.getY(), lastPos.getZ());

                List<SimpleCollisionBox> boxes = Collisions.getCollisionBoxes(player, feetBB);

                for (SimpleCollisionBox box : boxes) {
                    if (feetBB.collidesVertically(box)) { // If we collide vertically but aren't in the block
                        return;
                    }
                }

                if (isNearHardEntity(feetBB.expand(4))) return;

                // TODO: We actually need to pass this into a post prediction check to double check boats/shulkers
                // also, stepping on legacy versions needs to be checked correctly
                Bukkit.broadcastMessage(ChatColor.RED + "Player used NoFall! ");
            }
        }
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

    public void checkZeroPointZeroThreeGround(boolean onGround) {
        if (onGround) {
            Vector3d pos = player.packetStateData.packetPosition;
            SimpleCollisionBox feetBB = GetBoundingBox.getBoundingBoxFromPosAndSize(pos.getX(), pos.getY(), pos.getZ(), 0.6, 0.001);
            feetBB.expand(0.03); // 0.03 can be in any direction

            List<SimpleCollisionBox> boxes = Collisions.getCollisionBoxes(player, feetBB);

            for (SimpleCollisionBox box : boxes) {
                if (feetBB.isCollided(box)) { // Can't check for intersection, rely on NoClip checks to deal with this.
                    return;
                }
            }

            if (isNearHardEntity(feetBB.expand(4))) return;

            Bukkit.broadcastMessage(ChatColor.RED + "Player used NoFall with 0.03!");
        }
    }
}
