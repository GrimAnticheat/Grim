package ac.grim.grimac.utils.data;

import io.github.retrooper.packetevents.utils.player.Hand;

// This is to keep all the packet data out of the main player class
// Helps clean up the player class and makes devs aware they are sync'd to the netty thread
public class PacketStateData {
    public boolean isPacketSneaking = false;
    public boolean isPacketSprinting = false;
    public float packetVehicleHorizontal = 0;
    public float packetVehicleForward = 0;
    public int packetLastTransactionReceived = 0;
    public double packetPlayerX;
    public double packetPlayerY;
    public double packetPlayerZ;
    public float packetPlayerXRot;
    public float packetPlayerYRot;
    public int lastSlotSelected;
    public Hand eatingHand = Hand.MAIN_HAND;
    public boolean isEating = false;
    public Integer vehicle = null;
    public boolean receivedVehicleMove = false;
    public int horseJump = 0;
    public boolean tryingToRiptide = false;
}