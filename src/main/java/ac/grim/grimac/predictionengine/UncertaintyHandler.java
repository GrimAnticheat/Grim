package ac.grim.grimac.predictionengine;

import ac.grim.grimac.utils.lists.EvictingList;
import org.bukkit.block.BlockFace;

import java.util.HashSet;

public class UncertaintyHandler {
    public double pistonX;
    public double pistonY;
    public double pistonZ;
    public boolean collidingWithBoat;
    public boolean collidingWithShulker;
    public boolean striderOnGround;
    public boolean isStepMovement;
    public HashSet<BlockFace> slimePistonBounces;

    public double xNegativeUncertainty = 0;
    public double xPositiveUncertainty = 0;
    public double zNegativeUncertainty = 0;
    public double zPositiveUncertainty = 0;

    public boolean wasLastGravityUncertain = false;
    public boolean wasLastOnGroundUncertain = false;
    public double gravityUncertainty = 0;
    public boolean lastLastMovementWasZeroPointZeroThree = false;
    public boolean lastMovementWasZeroPointZeroThree = false;

    public boolean didGroundStatusChangeWithoutPositionPacket = false;

    public EvictingList<Integer> strictCollidingEntities = new EvictingList<>(3);
    public EvictingList<Integer> collidingEntities = new EvictingList<>(3);

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
}
