package ac.grim.grimac.checks.impl.groundspoof;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPositionRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientRotation;

import java.util.ArrayList;
import java.util.List;

// Catches NoFalls that obey the (1 / 64) rule
@CheckData(name = "NoFall A")
public class NoFallA extends PacketCheck {

    public boolean playerUsingNoGround = false;

    public NoFallA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientFlying.isInstanceOfFlying(event.getPacketType())) {
            // We have the wrong world cached with chunks
            if (player.bukkitPlayer.getWorld() != player.playerWorld) return;
            // The player hasn't spawned yet
            if (player.getSetbackTeleportUtil().insideUnloadedChunk()) return;

            WrapperPlayClientFlying wrapper = null;
            boolean hasPosition = false;

            // Flying packet types
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
                wrapper = new WrapperPlayClientPosition(event);
                hasPosition = true;
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                wrapper = new WrapperPlayClientPositionRotation(event);
                hasPosition = true;
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
                wrapper = new WrapperPlayClientRotation(event);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
                wrapper = new WrapperPlayClientFlying(event);
            }

            assert wrapper != null;

            // Force teleports to have onGround set to false, might patch NoFall on some version.
            if (player.packetStateData.lastPacketWasTeleport) {
                wrapper.setOnGround(false);
                return;
            }

            // The prediction based NoFall check wants us to make the player take fall damage - patches NoGround NoFall
            // NoGround works because if you never touch the ground, you never take fall damage
            // So we make the player touch the ground, and therefore they take fall damage
            if (playerUsingNoGround) {
                playerUsingNoGround = false;
                wrapper.setOnGround(true);
                return;
            }

            // If the player claims to be on the ground
            if (wrapper.isOnGround()) {
                if (!hasPosition) {
                    if (!is003OnGround(wrapper.isOnGround())) wrapper.setOnGround(false);
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

                wrapper.setOnGround(false);
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
