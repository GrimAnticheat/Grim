package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class ReachInterpolationData {
    public final SimpleCollisionBox targetLocation;
    public SimpleCollisionBox startingLocation;
    public int interpolationStepsLowBound = 0;
    public int interpolationStepsHighBound = 0;

    public ReachInterpolationData(SimpleCollisionBox startingLocation, double x, double y, double z) {
        this.startingLocation = startingLocation;
        this.targetLocation = GetBoundingBox.getBoundingBoxFromPosAndSize(x, y, z, 0.6, 1.8);
    }

    // To avoid huge branching when bruteforcing interpolation -
    // we combine the collision boxes for the steps.
    //
    // Designed around being unsure of minimum interp, maximum interp, and target location on 1.9 clients
    public SimpleCollisionBox getPossibleLocationCombined() {
        double stepMinX = (targetLocation.minX - startingLocation.minX) / 3;
        double stepMaxX = (targetLocation.maxX - startingLocation.maxX) / 3;
        double stepMinY = (targetLocation.minY - startingLocation.minY) / 3;
        double stepMaxY = (targetLocation.maxY - startingLocation.maxY) / 3;
        double stepMinZ = (targetLocation.minZ - startingLocation.minZ) / 3;
        double stepMaxZ = (targetLocation.maxZ - startingLocation.maxZ) / 3;

        SimpleCollisionBox minimumInterpLocation = new SimpleCollisionBox(
                startingLocation.minX + (interpolationStepsLowBound * stepMinX),
                startingLocation.minY + (interpolationStepsLowBound * stepMinY),
                startingLocation.minZ + (interpolationStepsLowBound * stepMinZ),
                startingLocation.maxX + (interpolationStepsLowBound * stepMaxX),
                startingLocation.maxY + (interpolationStepsLowBound * stepMaxY),
                startingLocation.maxZ + (interpolationStepsLowBound * stepMaxZ));

        for (int step = interpolationStepsLowBound + 1; step <= interpolationStepsHighBound; step++) {
            minimumInterpLocation = combineCollisionBox(minimumInterpLocation, new SimpleCollisionBox(
                    startingLocation.minX + (step * stepMinX),
                    startingLocation.minY + (step * stepMinY),
                    startingLocation.minZ + (step * stepMinZ),
                    startingLocation.maxX + (step * stepMaxX),
                    startingLocation.maxY + (step * stepMaxY),
                    startingLocation.maxZ + (step * stepMaxZ)));
        }

        return minimumInterpLocation;
    }

    public static SimpleCollisionBox combineCollisionBox(SimpleCollisionBox one, SimpleCollisionBox two) {
        double minX = Math.min(one.minX, two.minX);
        double maxX = Math.max(one.maxX, two.maxX);
        double minY = Math.min(one.minY, two.minY);
        double maxY = Math.max(one.maxY, two.maxY);
        double minZ = Math.min(one.minZ, two.minZ);
        double maxZ = Math.max(one.maxZ, two.maxZ);

        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void updatePossibleStartingLocation(SimpleCollisionBox possibleLocationCombined) {
        Bukkit.broadcastMessage(ChatColor.GOLD + "WARNING: Desync has been protected!");
        this.startingLocation = combineCollisionBox(startingLocation, possibleLocationCombined);
    }

    public void tickMovement() {
        this.interpolationStepsLowBound = Math.min(interpolationStepsLowBound + 1, 3);
        this.interpolationStepsHighBound = Math.min(interpolationStepsHighBound + 1, 3);
    }
}
