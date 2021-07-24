package ac.grim.grimac.predictionengine;

import ac.grim.grimac.utils.lists.EvictingList;
import org.bukkit.block.BlockFace;

import java.util.HashSet;

public class UncertaintyHandler {
    // Handles uncertainty when a piston could have pushed a player in a direction
    // Only the required amount of uncertainty is given
    public double pistonX;
    public double pistonY;
    public double pistonZ;
    // Is the player within 0.26 of a boat?
    public boolean collidingWithBoat;
    // Is the player within 0.26 blocks of a shulker?
    public boolean collidingWithShulker;
    // Did the player step onto a block?
    // This is needed because we don't know if a player jumped onto the step block or not
    // Jumping would set onGround to false while not would set it to true
    // Meaning no matter what, just trust the player's onGround status
    public boolean isStepMovement;
    // What directions could slime block pistons be pushing the player from
    public HashSet<BlockFace> slimePistonBounces;

    // Handles general uncertainty such as entity pushing and the 1.14+ X Z collision bug where X momentum is maintained
    public double xNegativeUncertainty = 0;
    public double xPositiveUncertainty = 0;
    public double zNegativeUncertainty = 0;
    public double zPositiveUncertainty = 0;

    public double yNegativeUncertainty = 0;
    public double yPositiveUncertainty = 0;

    // Handles 0.03 vertical false where actual velocity is greater than predicted because of previous lenience
    public boolean wasLastGravityUncertain = false;
    // Marks how much to allow the actual velocity to deviate from predicted when
    // the previous lenience because of 0.03 would occur
    public double gravityUncertainty = 0;
    // The player landed while jumping but without new position information because of 0.03
    public boolean wasLastOnGroundUncertain = false;
    // Marks previous didGroundStatusChangeWithoutPositionPacket from last tick
    public boolean lastPacketWasGroundPacket = false;
    // Marks previous lastPacketWasGroundPacket from last tick
    public boolean lastLastPacketWasGroundPacket = false;
    // Slime sucks in terms of bouncing and stuff.  Trust client onGround when on slime
    public boolean isSteppingOnSlime = false;

    // Marks whether the player could have landed but without position packet because 0.03
    public boolean lastTickWasNearGroundZeroPointZeroThree = false;

    // Give horizontal lenience if the previous movement was 0.03 because their velocity is unknown
    public boolean lastMovementWasZeroPointZeroThree = true;
    // Give horizontal lenience if two movements ago was 0.03 because especially on ice it matters
    public boolean lastLastMovementWasZeroPointZeroThree = false;

    // How many entities are very likely to be colliding with the player's bounding box?
    public EvictingList<Integer> strictCollidingEntities = new EvictingList<>(3);
    // How many entities are within 0.5 blocks of the player's bounding box?
    public EvictingList<Integer> collidingEntities = new EvictingList<>(3);
    public EvictingList<Double> pistonPushing = new EvictingList<>(20);

    public EvictingList<Boolean> tempElytraFlightHack = new EvictingList<>(3);

    public int lastTeleportTicks = 0;
    public boolean hasSentValidMovementAfterTeleport = false;

    public UncertaintyHandler() {
        reset();
    }

    public void reset() {
        pistonX = 0;
        pistonY = 0;
        pistonZ = 0;
        gravityUncertainty = 0;
        collidingWithBoat = false;
        collidingWithShulker = false;
        isStepMovement = false;
        slimePistonBounces = new HashSet<>();
    }

    @Override
    public String toString() {
        return "UncertaintyHandler{" +
                "pistonX=" + pistonX +
                ", pistonY=" + pistonY +
                ", pistonZ=" + pistonZ +
                ", collidingWithBoat=" + collidingWithBoat +
                ", collidingWithShulker=" + collidingWithShulker +
                ", isStepMovement=" + isStepMovement +
                ", xNegativeUncertainty=" + xNegativeUncertainty +
                ", xPositiveUncertainty=" + xPositiveUncertainty +
                ", zNegativeUncertainty=" + zNegativeUncertainty +
                ", zPositiveUncertainty=" + zPositiveUncertainty +
                ", wasLastGravityUncertain=" + wasLastGravityUncertain +
                ", gravityUncertainty=" + gravityUncertainty +
                ", wasLastOnGroundUncertain=" + wasLastOnGroundUncertain +
                ", lastPacketWasGroundPacket=" + lastPacketWasGroundPacket +
                ", lastLastPacketWasGroundPacket=" + lastLastPacketWasGroundPacket +
                ", lastTickWasNearGroundZeroPointZeroThree=" + lastTickWasNearGroundZeroPointZeroThree +
                ", lastMovementWasZeroPointZeroThree=" + lastMovementWasZeroPointZeroThree +
                ", lastLastMovementWasZeroPointZeroThree=" + lastLastMovementWasZeroPointZeroThree +
                '}';
    }
}
