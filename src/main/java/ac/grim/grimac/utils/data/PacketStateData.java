package ac.grim.grimac.utils.data;

import io.github.retrooper.packetevents.utils.player.Hand;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.GameMode;

import java.util.concurrent.atomic.AtomicInteger;

// This is to keep all the packet data out of the main player class
// Helps clean up the player class and makes devs aware they are sync'd to the netty thread
public class PacketStateData {
    public boolean isPacketSneaking = false;
    public boolean isPacketSprinting = false;
    public float packetVehicleHorizontal = 0;
    public float packetVehicleForward = 0;
    public AtomicInteger packetLastTransactionReceived = new AtomicInteger(0);
    public Vector3d lastPacketPosition;
    public Vector3d packetPosition;
    public float lastPacketPlayerXRot;
    public float lastPacketPlayerYRot;
    public float packetPlayerXRot;
    public float packetPlayerYRot;
    public boolean packetPlayerOnGround = false;
    public boolean lastPacketWasTeleport = false;
    public int lastSlotSelected;
    public Hand eatingHand = Hand.MAIN_HAND;
    public AlmostBoolean slowedByUsingItem = AlmostBoolean.FALSE;
    public GameMode gameMode;
    public boolean receivedSteerVehicle = false;
    public int horseJump = 0;
    public boolean tryingToRiptide = false;
    // Marks when the player received a ground packet
    public boolean didGroundStatusChangeWithoutPositionPacket = false;
    // This works on 1.8 only
    public boolean didLastLastMovementIncludePosition = false;
    public boolean didLastMovementIncludePosition = false;
    // Just to filter out the first incorrect ground status
    public int movementPacketsReceived = 0;
    public int minPlayerAttackSlow = 0;
    public int maxPlayerAttackSlow = 0;
}