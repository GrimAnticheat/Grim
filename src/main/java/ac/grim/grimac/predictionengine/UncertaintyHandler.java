package ac.grim.grimac.predictionengine;

import org.bukkit.block.BlockFace;

import java.util.HashSet;

public class UncertaintyHandler {
    public double pistonX;
    public double pistonY;
    public double pistonZ;
    public boolean collidingWithBoat;
    public boolean collidingWithShulker;
    public boolean striderOnGround;
    public HashSet<BlockFace> slimePistonBounces;

    public double xPushEntityPositive = 0;
    public double zPushEntityPositive = 0;
    public double xPushEntityNegative = 0;
    public double zPushEntityNegative = 0;
    public double collidingEntities = 0;

    public UncertaintyHandler() {
        reset();
    }

    public void reset() {
        pistonX = 0;
        pistonY = 0;
        pistonZ = 0;
        collidingWithBoat = false;
        collidingWithShulker = false;
        striderOnGround = false;
        slimePistonBounces = new HashSet<>();
    }
}
