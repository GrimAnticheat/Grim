package ac.grim.grimac.checks.impl.groundspoof;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.ArrayList;
import java.util.List;

// Catches NoFalls for LOOK and GROUND packets
// This check runs AFTER the predictions
@CheckData(name = "NoFall", configName = "nofall", setback = 10)
public class NoFallA extends Check implements PacketCheck {

    public boolean flipPlayerGroundStatus = false;

    public NoFallA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            // The player hasn't spawned yet
            if (player.getSetbackTeleportUtil().insideUnloadedChunk()) return;
            // The player has already been flagged, and
            if (player.getSetbackTeleportUtil().blockOffsets) return;

            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
            boolean hasPosition = false;

            // If the player claims to be on the ground
            // Run this code IFF the player doesn't send the position, as that won't get processed by predictions
            if (wrapper.isOnGround() && !hasPosition) {
                if (!isNearGround(wrapper.isOnGround())) { // If player isn't near ground
                    // 1.8 boats have a mind on their own... only flag if they're not near a boat or are on 1.9+
                    if (!GhostBlockDetector.isGhostBlock(player) && flagWithSetback()) alert("");
                    if (shouldModifyPackets()) wrapper.setOnGround(false);
                }
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
            // The prediction based NoFall check (that runs before us without the packet)
            // has asked us to flip the player's onGround status
            // This happens to make both checks use the same logic... and
            // since we don't have access to modify the packet with prediction based checks
            // I could add that feature but ehh... this works and is better anyway.
            //
            // Also flip teleports because I don't trust vanilla's handling of teleports and ground
            if (flipPlayerGroundStatus) {
                flipPlayerGroundStatus = false;
                if (shouldModifyPackets()) wrapper.setOnGround(!wrapper.isOnGround());
            }
            if (player.packetStateData.lastPacketWasTeleport) {
                if (shouldModifyPackets()) wrapper.setOnGround(false);
            }
        }
    }

    public boolean isNearGround(boolean onGround) {
        if (onGround) {
            SimpleCollisionBox feetBB = GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.6f, 0.001f);
            feetBB.expand(player.getMovementThreshold()); // Movement threshold can be in any direction

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
