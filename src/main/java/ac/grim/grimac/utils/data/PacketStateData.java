package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.protocol.player.InteractionHand;

// This is to keep all the packet data out of the main player class
// Helps clean up the player class and makes devs aware they are sync'd to the netty thread
public class PacketStateData {
    public boolean packetPlayerOnGround = false;
    public boolean lastPacketWasTeleport = false;
    public boolean lastPacketWasOnePointSeventeenDuplicate = false;
    public int lastSlotSelected;
    public InteractionHand eatingHand = InteractionHand.MAIN_HAND;
    public boolean slowedByUsingItem = false;
    public int slowedByUsingItemTransaction = Integer.MIN_VALUE;
    public boolean receivedSteerVehicle = false;
    // This works on 1.8 only
    public boolean didLastLastMovementIncludePosition = false;
    public boolean didLastMovementIncludePosition = false;
}