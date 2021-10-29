package ac.grim.grimac.checks.impl.groundspoof;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

// Catches NoFalls that obey the (1 / 64) rule
@CheckData(name = "NoFall A")
public class NoFallA extends PacketCheck {

    private final GrimPlayer player;
    public boolean playerUsingNoGround = false;

    public NoFallA(GrimPlayer player) {
        super(player);
        this.player = player;
    }

    @Override
    public void onPacketReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (PacketType.Play.Client.Util.isInstanceOfFlying(packetID)) {
            WrappedPacketInFlying flying = new WrappedPacketInFlying(event.getNMSPacket());

            // We have the wrong world cached with chunks
            if (player.bukkitPlayer.getWorld() != player.playerWorld) return;
            // The player hasn't spawned yet
            if (player.getSetbackTeleportUtil().insideUnloadedChunk()) return;

            // Force teleports to have onGround set to false, might patch NoFall on some version.
            if (player.packetStateData.lastPacketWasTeleport) {
                flying.setOnGround(false);
                return;
            }

            // The prediction based NoFall check wants us to make the player take fall damage - patches NoGround NoFall
            // NoGround works because if you never touch the ground, you never take fall damage
            // So we make the player touch the ground, and therefore they take fall damage
            if (playerUsingNoGround) {
                playerUsingNoGround = false;
                flying.setOnGround(true);
                return;
            }

            // If the player claims to be on the ground
            if (flying.isOnGround()) {
                boolean hasPosition = packetID == PacketType.Play.Client.POSITION || packetID == PacketType.Play.Client.POSITION_LOOK;

                if (!hasPosition) {
                    if (!is003OnGround(flying.isOnGround())) flying.setOnGround(false);
                    return;
                }

                SimpleCollisionBox feetBB;

                Vector3d position = new Vector3d(player.x, player.y, player.z);
                Vector3d lastPos = new Vector3d(player.lastX, player.lastY, player.lastZ);

                feetBB = GetBoundingBox.getBoundingBoxFromPosAndSize(position.getX(), position.getY(), position.getZ(), 0.6, 0.001);

                // Don't expand if the player moved more than 50 blocks this tick (stop netty crash exploit)
                if (position.distanceSquared(lastPos) < 2500)
                    feetBB.expandToAbsoluteCoordinates(lastPos.getX(), lastPos.getY(), lastPos.getZ());

                // Shulkers have weird BB's that the player might be standing on
                if (Collisions.hasMaterial(player, feetBB, blockData -> Materials.checkFlag(blockData.getMaterial(), Materials.SHULKER)))
                    return;

                // This is to support stepping movement (Not blatant, we need to wait on prediction engine to flag this)
                // This check mainly serves to correct blatant onGround cheats
                feetBB.expandMin(0, -4, 0);

                if (checkForBoxes(feetBB)) return;

                flying.setOnGround(false);
            }
        }
    }

    public boolean is003OnGround(boolean onGround) {
        if (onGround) {
            SimpleCollisionBox feetBB = GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.6, 0.001);
            feetBB.expand(0.03); // 0.03 can be in any direction

            return checkForBoxes(feetBB);
        }
        return true;
    }

    private boolean checkForBoxes(SimpleCollisionBox playerBB) {
        List<SimpleCollisionBox> boxes = new ArrayList<>();
        Collisions.getCollisionBoxes(player, playerBB, boxes, false);

        for (SimpleCollisionBox box : boxes) {
            if (playerBB.collidesVertically(box)) { // If we collide vertically but aren't in the block
                return true;
            }
        }

        return player.compensatedWorld.isNearHardEntity(playerBB.copy().expand(4));
    }
}
