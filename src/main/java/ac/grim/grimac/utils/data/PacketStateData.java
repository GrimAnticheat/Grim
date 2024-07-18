package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;

// This is to keep all the packet data out of the main player class
// Helps clean up the player class and makes devs aware they are sync'd to the netty thread
public class PacketStateData {
    public boolean packetPlayerOnGround = false;
    public boolean lastPacketWasTeleport = false;
    public boolean cancelDuplicatePacket, lastPacketWasOnePointSeventeenDuplicate = false;
    public boolean lastTransactionPacketWasValid = false;
    public int lastSlotSelected;
    public InteractionHand eatingHand = InteractionHand.MAIN_HAND;
    public long lastRiptide = 0;
    public boolean tryingToRiptide = false;
    @Getter
    private boolean slowedByUsingItem;
    @Getter
    private int slowedByUsingItemSlot = Integer.MIN_VALUE;
    public int slowedByUsingItemTransaction = Integer.MIN_VALUE;
    public boolean receivedSteerVehicle = false;
    // This works on 1.8 only
    public boolean didLastLastMovementIncludePosition = false;
    public boolean didLastMovementIncludePosition = false;
    public Vector3d lastClaimedPosition = new Vector3d(0, 0, 0);

    public float lastHealth, lastSaturation;
    public int lastFood;
    public boolean lastServerTransWasValid = false;

    public void setSlowedByUsingItem(boolean slowedByUsingItem) {
        this.slowedByUsingItem = slowedByUsingItem;
        slowedByUsingItemSlot = slowedByUsingItem ? lastSlotSelected : Integer.MIN_VALUE;
    }
}
