package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.lists.EvictingList;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class UncertaintyHandler {
    private final GrimPlayer player;
    // Handles uncertainty when a piston could have pushed a player in a direction
    // Only the required amount of uncertainty is given
    public double pistonX;
    public double pistonY;
    public double pistonZ;
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
    public boolean wasZeroPointThreeVertically = false;
    public EvictingList<Double> slimeBlockUpwardsUncertainty = new EvictingList<>(3);
    // The player landed while jumping but without new position information because of 0.03
    public boolean onGroundUncertain = false;
    // Marks previous didGroundStatusChangeWithoutPositionPacket from last tick
    public boolean lastPacketWasGroundPacket = false;
    // Marks previous lastPacketWasGroundPacket from last tick
    public boolean lastLastPacketWasGroundPacket = false;
    // Slime sucks in terms of bouncing and stuff.  Trust client onGround when on slime
    public boolean isSteppingOnSlime = false;
    public boolean isSteppingOnIce = false;
    public boolean isSteppingOnHoney = false;
    public boolean wasSteppingOnBouncyBlock = false;
    public boolean isSteppingOnBouncyBlock = false;
    public boolean isSteppingNearBubbleColumn = false;
    public boolean isNearGlitchyBlock = false;
    public boolean isOrWasNearGlitchyBlock = false;
    // Did the player claim to leave stuck speed? (0.03 messes these calculations up badly)
    public boolean claimingLeftStuckSpeed = false;
    public int stuckOnEdge = -100;
    public int lastStuckNorth = -100;
    public int lastStuckSouth = -100;
    public int lastStuckWest = -100;
    public int lastStuckEast = -100;
    public boolean nextTickScaffoldingOnEdge = false;
    public boolean scaffoldingOnEdge = false;
    // Marks whether the player could have landed but without position packet because 0.03
    public boolean lastTickWasNearGroundZeroPointZeroThree = false;
    // Give horizontal lenience if the previous movement was 0.03 because their velocity is unknown
    public boolean lastMovementWasZeroPointZeroThree = false;
    // Give horizontal lenience if the last movement reset velocity because 0.03 becomes unknown then
    public boolean lastMovementWasUnknown003VectorReset = false;
    // Did the player change their look with elytra between tick (we can't calculate 0.03 here)
    public boolean claimedLookChangedBetweenTick = false;
    // How many entities are within 0.5 blocks of the player's bounding box?
    public EvictingList<Integer> collidingEntities = new EvictingList<>(3);
    public EvictingList<Double> pistonPushing = new EvictingList<>(20);

    // Fireworks are pure uncertainty and cause issues (Their implementation is terrible)
    public boolean lastUsingFirework = false;
    public int lastFireworkStatusChange = -100;

    public int lastTeleportTicks = -100;
    public int lastFlyingTicks = -100;
    public int lastSneakingChangeTicks = -100;
    public int lastGlidingChangeTicks = -100;
    public int lastMetadataDesync = -100;
    public int lastFlyingStatusChange = -100;
    public int lastUnderwaterFlyingHack = -100;
    public int lastStuckSpeedMultiplier = -100;
    public int lastHardCollidingLerpingEntity = -100;
    public int lastThirtyMillionHardBorder = -100;

    public double lastHorizontalOffset = 0;
    public double lastVerticalOffset = 0;

    public UncertaintyHandler(GrimPlayer player) {
        this.player = player;

        // Add stuff to evicting list to avoid issues later on
        slimeBlockUpwardsUncertainty.add(0d);
        slimeBlockUpwardsUncertainty.add(0d);

        tick();
    }

    public void tick() {
        pistonX = 0;
        pistonY = 0;
        pistonZ = 0;
        isStepMovement = false;
        slimePistonBounces = new HashSet<>();
    }

    public boolean wasAffectedByStuckSpeed() {
        return lastStuckSpeedMultiplier > -5;
    }

    public double getOffsetHorizontal(VectorData data) {
        boolean newVectorPointThree = (player.couldSkipTick && data.isKnockback()) || data.isTrident();
        boolean explicit003 = data.isZeroPointZeroThree() || lastMovementWasZeroPointZeroThree;
        boolean either003 = newVectorPointThree || explicit003;

        double pointThree = newVectorPointThree || lastMovementWasUnknown003VectorReset ? 0.03 : 0;

        // 0.91 * 0.6 * 0.06 = 0.03276 + 0.03 offset
        if (explicit003) {
            pointThree = 0.06276;
        }

        // 0.06 * 0.91 * 0.8 = max + 0.03 offset
        if (either003 && (influencedByBouncyBlock() || isSteppingOnHoney))
            pointThree = 0.07368;

        // 0.06 * 0.91 * 0.989 = max + 0.03 offset
        if (either003 && isSteppingOnIce)
            pointThree = 0.084;

        // Reduce second tick uncertainty by minimum friction amount
        if (!newVectorPointThree && either003)
            pointThree *= 0.91 * 0.989;

        // 0.06 * 0.91 = max + 0.03 offset
        if (either003 && (player.isGliding || player.lastOnGround || player.specialFlying))
            pointThree = 0.0846;

        if (player.uncertaintyHandler.claimingLeftStuckSpeed)
            pointThree = 0.15;

        if (lastThirtyMillionHardBorder > -3)
            pointThree = 0.15;

        if (player.vehicleData.lastVehicleSwitch < 5)
            pointThree = Math.max(pointThree, player.speed * 2);

        if (player.uncertaintyHandler.scaffoldingOnEdge) {
            pointThree = Math.max(pointThree, player.speed * 1.6);
        }

        // 0.03 plus being able to maintain velocity even when shifting is brutal
        // Value patched - I have no idea why these things are different in liquid vs in air
        if (stuckOnEdge == ((player.wasTouchingWater || player.wasTouchingLava) ? 0 : -1)) {
            pointThree = Math.max(pointThree, player.speed * 2);
        }

        return pointThree;
    }

    public boolean influencedByBouncyBlock() {
        return isSteppingOnBouncyBlock || wasSteppingOnBouncyBlock;
    }

    public double getVerticalOffset(VectorData data) {
        if (lastThirtyMillionHardBorder > -3)
            return 0.15;

        if (player.uncertaintyHandler.claimingLeftStuckSpeed)
            return 0.06;

        if (player.vehicleData.lastVehicleSwitch < 2)
            return 0.1;

        // We don't know if the player was pressing jump or not
        if (player.uncertaintyHandler.wasSteppingOnBouncyBlock && (player.wasTouchingWater || player.wasTouchingLava))
            return 0.06;

        // Not worth my time to fix this because checking flying generally sucks - if player was flying in last 2 ticks
        if ((lastFlyingTicks < 5) && Math.abs(data.vector.getY()) < (4.5 * player.flySpeed - 0.25))
            return 0.06;

        // This swim hop could be 0.03-influenced movement
        if (data.isTrident())
            return 0.06;

        // Velocity resets velocity, so we only have to give 0.03 uncertainty rather than 0.06
        if (player.couldSkipTick && data.isKnockback())
            return 0.03;

        if (player.pointThreeEstimator.controlsVerticalMovement()) {
            // Yeah, the second 0.06 isn't mathematically correct but fucking 0.03 fucks everything up...
            // Water pushing, elytras, EVERYTHING vertical movement gets messed up by this shit.  What the fuck mojang.  Why the fuck did you do this.
            if (data.isZeroPointZeroThree()) return 0.06;
            if (lastMovementWasZeroPointZeroThree) return 0.06;
            if (wasZeroPointThreeVertically || player.uncertaintyHandler.lastPacketWasGroundPacket)
                return 0.03;
            return 0;
        }

        if (wasZeroPointThreeVertically || player.uncertaintyHandler.lastPacketWasGroundPacket)
            return 0.03;

        return 0;
    }

    public double reduceOffset(double offset) {
        // Exempt players from piston checks by giving them 1 block of lenience for any piston pushing
        if (Collections.max(player.uncertaintyHandler.pistonPushing) > 0) {
            offset -= 1;
        }

        // Boats are too glitchy to check.
        // Yes, they have caused an insane amount of uncertainty!
        // Even 1 block offset reduction isn't enough... damn it mojang
        if (player.uncertaintyHandler.lastHardCollidingLerpingEntity > -3) {
            offset -= 1.2;
        }

        if (player.uncertaintyHandler.lastFlyingStatusChange > -5) {
            offset -= 0.25;
        }

        if (player.uncertaintyHandler.isOrWasNearGlitchyBlock) {
            offset -= 0.25;
        }

        if (player.uncertaintyHandler.stuckOnEdge > -3) {
            offset -= 0.05;
        }

        // Exempt flying status change
        if (player.uncertaintyHandler.lastFlyingStatusChange > -20) {
            offset = 0;
        }

        // Errors are caused by a combination of client/server desync while climbing
        // desync caused by 0.03 and the lack of an idle packet
        //
        // I can't solve this.  This is on Mojang to fix.
        //
        // Don't even attempt to fix the poses code... garbage in garbage out - I did the best I could
        // you can likely look at timings of packets to extrapolate better... but I refuse to use packet timings for stuff like this
        // Does anyone at mojang understand netcode??? (the answer is no)
        //
        // Don't give me the excuse that it was originally a singleplayer game so the netcode is terrible...
        // the desync's and netcode has progressively gotten worse starting with 1.9!
        if (!Collisions.isEmpty(player, GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.6f, 1.8f).expand(-SimpleCollisionBox.COLLISION_EPSILON).offset(0, 0.03, 0)) && player.isClimbing) {
            offset -= 0.12;
        }

        // I can't figure out how the client exactly tracks boost time
        if (player.playerVehicle instanceof PacketEntityRideable) {
            PacketEntityRideable vehicle = (PacketEntityRideable) player.playerVehicle;
            if (vehicle.currentBoostTime < vehicle.boostTimeMax + 20)
                offset -= 0.01;
        }

        // Sneaking near edge cases a ton of issues
        // Don't give this bonus if the Y axis is wrong though.
        // Another temporary permanent hack.
        if (player.uncertaintyHandler.stuckOnEdge == -2 && player.clientVelocity.getY() > 0 && Math.abs(player.clientVelocity.getY() - player.actualMovement.getY()) < 1e-6)
            offset -= 0.1;

        return Math.max(0, offset);
    }

    public void checkForHardCollision() {
        // Look for boats the player could collide with
        player.uncertaintyHandler.lastHardCollidingLerpingEntity--;
        if (hasHardCollision()) player.uncertaintyHandler.lastHardCollidingLerpingEntity = 0;
    }

    private boolean hasHardCollision() {
        SimpleCollisionBox expandedBB = player.boundingBox.copy().expandToCoordinate(player.clientVelocity.getX(), player.clientVelocity.getY(), player.clientVelocity.getZ()).expand(1);
        return regularHardCollision(expandedBB) || striderCollision(expandedBB) || boatCollision(expandedBB);
    }

    private boolean regularHardCollision(SimpleCollisionBox expandedBB) {
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if ((entity.type == EntityTypes.BOAT || entity.type == EntityTypes.SHULKER) && entity != player.playerVehicle &&
                    entity.getPossibleCollisionBoxes().isIntersected(expandedBB)) {
                return true;
            }
        }

        return false;
    }

    private boolean striderCollision(SimpleCollisionBox expandedBB) {
        // Stiders can walk on top of other striders
        if (player.playerVehicle instanceof PacketEntityStrider) {
            for (Map.Entry<Integer, PacketEntity> entityPair : player.compensatedEntities.entityMap.int2ObjectEntrySet()) {
                PacketEntity entity = entityPair.getValue();
                if (entity.type == EntityTypes.STRIDER && entity != player.playerVehicle && !entity.hasPassenger(entityPair.getKey())
                        && entity.getPossibleCollisionBoxes().isIntersected(expandedBB)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean boatCollision(SimpleCollisionBox expandedBB) {
        // Boats can collide with quite literally anything
        if (player.playerVehicle != null && player.playerVehicle.type == EntityTypes.BOAT) {
            for (Map.Entry<Integer, PacketEntity> entityPair : player.compensatedEntities.entityMap.int2ObjectEntrySet()) {
                PacketEntity entity = entityPair.getValue();
                if (entity != player.playerVehicle && !entity.hasPassenger(entityPair.getKey()) &&
                        entity.getPossibleCollisionBoxes().isIntersected(expandedBB)) {
                    return true;
                }
            }
        }

        return false;
    }
}
