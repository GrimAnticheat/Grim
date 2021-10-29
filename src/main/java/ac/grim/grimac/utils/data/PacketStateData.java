package ac.grim.grimac.utils.data;

import io.github.retrooper.packetevents.utils.player.Hand;

// This is to keep all the packet data out of the main player class
// Helps clean up the player class and makes devs aware they are sync'd to the netty thread
public class PacketStateData {
    public boolean packetPlayerOnGround = false;
    public boolean lastPacketWasTeleport = false;
    public boolean lastPacketWasOnePointSeventeenDuplicate = false;
    public int lastSlotSelected;
    public Hand eatingHand = Hand.MAIN_HAND;
    public AlmostBoolean slowedByUsingItem = AlmostBoolean.FALSE;
    public int slowedByUsingItemTransaction = Integer.MIN_VALUE;
    public boolean receivedSteerVehicle = false;
    // Marks when the player received a ground packet
    public boolean didGroundStatusChangeWithoutPositionPacket = false;
    // This works on 1.8 only
    public boolean didLastLastMovementIncludePosition = false;
    public boolean didLastMovementIncludePosition = false;
}