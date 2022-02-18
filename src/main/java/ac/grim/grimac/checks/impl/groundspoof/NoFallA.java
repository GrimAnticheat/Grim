package ac.grim.grimac.checks.impl.groundspoof;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;

import java.util.ArrayList;
import java.util.List;

// Catches NoFalls for LOOK and GROUND packets
// This check runs AFTER the predictions
@CheckData(name = "NoFall", configName = "nofall", setback = 10)
public class NoFallA extends PacketCheck {

    public boolean flipPlayerGroundStatus = false;

    public NoFallA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // We have the wrong world cached with chunks
            if (player.bukkitPlayer != null && player.bukkitPlayer.getWorld() != player.playerWorld) return;
            // The player hasn't spawned yet
            if (player.getSetbackTeleportUtil().insideUnloadedChunk()) return;
            // The player has already been flagged, and
            if (player.getSetbackTeleportUtil().blockOffsets) return;

            PacketWrapper wrapper = null;
            boolean hasPosition = false;

            // Flying packet types
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
                wrapper = new WrapperPlayClientPlayerPosition(event);
                hasPosition = true;
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
                hasPosition = true;
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
                wrapper = new WrapperPlayClientPlayerRotation(event);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
                wrapper = new WrapperPlayClientPlayerFlying(event);
            }

            assert wrapper != null;

            // The prediction based NoFall check (that runs before us without the packet)
            // has asked us to flip the player's onGround status
            // This happens to make both checks use the same logic... and
            // since we don't have access to modify the packet with prediction based checks
            // I could add that feature but ehh... this works and is better anyway.
            if (flipPlayerGroundStatus) {
                flipPlayerGroundStatus = false;
                setOnGround(wrapper, !onGround(wrapper));
                return;
            }

            // If the player claims to be on the ground
            // Run this code IFF the player doesn't send the position, as that won't get processed by predictions
            if (onGround(wrapper) && !hasPosition) {
                if (!is003OnGround(onGround(wrapper))) { // If player isn't near ground
                    increaseViolations();
                    setOnGround(wrapper, false);
                } else {
                    reward();
                }
            }
        }
    }

    private void setOnGround(PacketWrapper wrapper, boolean onGround) {
        if (wrapper instanceof WrapperPlayClientPlayerPosition) {
            ((WrapperPlayClientPlayerPosition) wrapper).setOnGround(onGround);
        } else if (wrapper instanceof WrapperPlayClientPlayerPositionAndRotation) {
            ((WrapperPlayClientPlayerPositionAndRotation) wrapper).setOnGround(onGround);
        } else if (wrapper instanceof WrapperPlayClientPlayerRotation) {
            ((WrapperPlayClientPlayerRotation) wrapper).setOnGround(onGround);
        } else if (wrapper instanceof WrapperPlayClientPlayerFlying) {
            ((WrapperPlayClientPlayerFlying) wrapper).setOnGround(onGround);
        }
    }

    private boolean onGround(PacketWrapper wrapper) {
        if (wrapper instanceof WrapperPlayClientPlayerPosition) {
            return ((WrapperPlayClientPlayerPosition) wrapper).isOnGround();
        } else if (wrapper instanceof WrapperPlayClientPlayerPositionAndRotation) {
            return ((WrapperPlayClientPlayerPositionAndRotation) wrapper).isOnGround();
        } else if (wrapper instanceof WrapperPlayClientPlayerRotation) {
            return ((WrapperPlayClientPlayerRotation) wrapper).isOnGround();
        } else if (wrapper instanceof WrapperPlayClientPlayerFlying) {
            return ((WrapperPlayClientPlayerFlying) wrapper).isOnGround();
        }
        return false;
    }

    public boolean is003OnGround(boolean onGround) {
        if (onGround) {
            SimpleCollisionBox feetBB = GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.6f, 0.001f);
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
