package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.UncertaintyHandler;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import lombok.AllArgsConstructor;
import org.bukkit.util.Vector;

import java.util.*;

public final class SuperDebug extends Check implements PostPredictionCheck {
    private static final StringBuilder[] flags = new StringBuilder[256]; //  17 MB of logs in memory

    Map<StringBuilder, Integer> continuedDebug = new HashMap<>();

    List<VectorData> predicted = new EvictingQueue<>(60);
    List<Vector> actually = new EvictingQueue<>(60);
    List<Location> locations = new EvictingQueue<>(60);
    List<Vector> startTickClientVel = new EvictingQueue<>(60);
    List<Vector> baseTickAddition = new EvictingQueue<>(60);
    List<Vector> baseTickWater = new EvictingQueue<>(60);

    public SuperDebug(GrimPlayer player) {
        super(player);
    }

    public static StringBuilder getFlag(int identifier) {
        identifier--;
        if (identifier >= flags.length || identifier < 0) return null;
        return flags[identifier];
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        Location location = new Location(player.x, player.y, player.z, player.xRot, player.yRot, player.bukkitPlayer == null ? "null" : player.bukkitPlayer.getWorld().getName());

        for (Iterator<Map.Entry<StringBuilder, Integer>> it = continuedDebug.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<StringBuilder, Integer> debug = it.next();
            appendDebug(debug.getKey(), player.predictedVelocity, player.actualMovement, location, player.startTickClientVel, player.baseTickAddition, player.baseTickWaterPushing);
            debug.setValue(debug.getValue() - 1);
            if (debug.getValue() <= 0) it.remove();
        }

        predicted.add(player.predictedVelocity);
        actually.add(player.actualMovement);
        locations.add(location);
        startTickClientVel.add(player.startTickClientVel);
        baseTickAddition.add(player.baseTickAddition);
        baseTickWater.add(player.baseTickWaterPushing);

        if (predictionComplete.getIdentifier() == 0) return; // 1 - 256 are valid possible values

        StringBuilder sb = new StringBuilder();
        sb.append("Player Name: ");
        sb.append(player.user.getName());
        sb.append("\nClient Version: ");
        sb.append(player.getClientVersion().getReleaseName());
        sb.append("\nClient Brand: ");
        sb.append(player.getBrand());
        sb.append("\nServer Version: ");
        sb.append(PacketEvents.getAPI().getServerManager().getVersion().getReleaseName());
        sb.append("\nPing: ");
        sb.append(player.getTransactionPing());
        sb.append("ms\n\n");

        for (int i = 0; i < predicted.size(); i++) {
            VectorData predict = predicted.get(i);
            Vector actual = actually.get(i);
            Location loc = locations.get(i);
            Vector startTickVel = startTickClientVel.get(i);
            Vector addition = baseTickAddition.get(i);
            Vector water = baseTickWater.get(i);
            appendDebug(sb, predict, actual, loc, startTickVel, addition, water);
        }

        UncertaintyHandler uncertaintyHandler = player.uncertaintyHandler;
        sb.append("XNeg: ");
        sb.append(uncertaintyHandler.xNegativeUncertainty);
        sb.append("\nXPos: ");
        sb.append(uncertaintyHandler.xPositiveUncertainty);
        sb.append("\nYNeg: ");
        sb.append(uncertaintyHandler.yNegativeUncertainty);
        sb.append("\nYPos: ");
        sb.append(uncertaintyHandler.yPositiveUncertainty);
        sb.append("\nZNeg: ");
        sb.append(uncertaintyHandler.zNegativeUncertainty);
        sb.append("\nZPos: ");
        sb.append(uncertaintyHandler.zPositiveUncertainty);
        sb.append("\nStuck: ");
        sb.append(uncertaintyHandler.stuckOnEdge.hasOccurredSince(1));
        sb.append("\n\n0.03: ");
        sb.append(uncertaintyHandler.lastMovementWasZeroPointZeroThree);
        sb.append("\n0.03 reset: ");
        sb.append(uncertaintyHandler.lastMovementWasUnknown003VectorReset);
        sb.append("\n0.03 vertical: ");
        sb.append(uncertaintyHandler.wasZeroPointThreeVertically);

        sb.append("\n\nIs gliding: ");
        sb.append(player.isGliding);
        sb.append("\nIs swimming: ");
        sb.append(player.isSwimming);
        sb.append("\nIs on ground: ");
        sb.append(player.onGround);
        sb.append("\nClient claims ground: ");
        sb.append(player.clientClaimsLastOnGround);
        sb.append("\nLast on ground: ");
        sb.append(player.lastOnGround);
        sb.append("\nWater: ");
        sb.append(player.wasTouchingWater);
        sb.append("\nLava: ");
        sb.append(player.wasTouchingLava);
        sb.append("\nVehicle: ");
        sb.append(player.compensatedEntities.getSelf().inVehicle());

        sb.append("\n\n");
        sb.append("Bounding box: ");
        sb.append("minX=");
        sb.append(player.boundingBox.minX);
        sb.append(", minY=");
        sb.append(player.boundingBox.minY);
        sb.append(", minZ=");
        sb.append(player.boundingBox.minZ);
        sb.append(", maxX=");
        sb.append(player.boundingBox.maxX);
        sb.append(", maxY=");
        sb.append(player.boundingBox.maxY);
        sb.append(", maxZ=");
        sb.append(player.boundingBox.maxZ);
        sb.append('}');
        sb.append("\n");

        int maxLength = 0;
        int maxPosLength = 0;

        // We can use Math.log10() to calculate the length of the number without string concatenation
        for (int y = GrimMath.floor(player.boundingBox.minY) - 2; y <= GrimMath.ceil(player.boundingBox.maxY) + 2; y++) {
            for (int z = GrimMath.floor(player.boundingBox.minZ) - 2; z <= GrimMath.ceil(player.boundingBox.maxZ) + 2; z++) {
                maxPosLength = (int) Math.max(maxPosLength, Math.ceil(Math.log10(Math.abs(z))));
                for (int x = GrimMath.floor(player.boundingBox.minX) - 2; x <= GrimMath.ceil(player.boundingBox.maxX) + 2; x++) {
                    maxPosLength = (int) Math.max(maxPosLength, Math.ceil(Math.log10(Math.abs(x))));
                    WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
                    maxLength = Math.max(block.toString().replace("minecraft:", "").length(), maxLength);
                }
            }
        }

        maxPosLength += 4; // To handle "x: [num] "
        maxLength++; // Add a space between blocks

        for (int y = GrimMath.ceil(player.boundingBox.maxY) + 2; y >= GrimMath.floor(player.boundingBox.minY) - 2; y--) {
            sb.append("y: ");
            sb.append(y);
            sb.append("\n");

            sb.append(String.format("%-" + maxPosLength + "s", "x: "));
            for (int x = GrimMath.floor(player.boundingBox.minX) - 2; x <= GrimMath.ceil(player.boundingBox.maxX) + 2; x++) {
                sb.append(String.format("%-" + maxLength + "s", x));
            }
            sb.append("\n");

            for (int z = GrimMath.floor(player.boundingBox.minZ) - 2; z <= GrimMath.ceil(player.boundingBox.maxZ) + 2; z++) {
                sb.append(String.format("%-" + maxPosLength + "s", "z: " + z + " "));
                for (int x = GrimMath.floor(player.boundingBox.minX) - 2; x <= GrimMath.ceil(player.boundingBox.maxX) + 2; x++) {
                    WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
                    sb.append(String.format("%-" + maxLength + "s", block.toString().replace("minecraft:", "")));
                }
                sb.append("\n");
            }

            sb.append("\n\n\n");
        }

        flags[predictionComplete.getIdentifier() - 1] = sb;
        continuedDebug.put(sb, 40);
    }

    private void appendDebug(StringBuilder sb, VectorData predict, Vector actual, Location location, Vector startTick, Vector addition, Vector water) {
        if (predict.isZeroPointZeroThree()) {
            sb.append("Movement threshold/tick skipping\n");
        }
        if (predict.isAttackSlow()) {
            sb.append("* 0.6 horizontal attack slowdown\n");
        }
        if (predict.isKnockback()) {
            if (player.firstBreadKB != null) {
                sb.append("First bread knockback: ").append(player.firstBreadKB.vector).append("\n");
            }
            if (player.likelyKB != null) {
                sb.append("Second bread knockback: ").append(player.likelyKB.vector).append("\n");
            }
        }
        if (predict.isExplosion()) {
            if (player.firstBreadExplosion != null) {
                sb.append("First bread explosion: ").append(player.firstBreadExplosion.vector).append("\n");
            }
            if (player.likelyExplosions != null) {
                sb.append("Second bread explosion: ").append(player.likelyExplosions.vector).append("\n");
            }
        }
        if (predict.isTrident()) {
            sb.append("Trident\n");
        }
        if (predict.isSwimHop()) {
            sb.append("Swim hop\n");
        }
        if (predict.isJump()) {
            sb.append("Jump\n");
        }

        // Apply 0.003/0.005 to make numbers more accurate
        Set<VectorData> set = new HashSet<>(Collections.singletonList(new VectorData(startTick.clone(), VectorData.VectorType.BestVelPicked)));
        new PredictionEngine().applyMovementThreshold(player, set);
        Vector trueStartVel = ((VectorData) set.toArray()[0]).vector;

        Vector clientMovement = getPlayerMathMovement(player, actual.clone().subtract(trueStartVel), location.xRot);
        Vector simulatedMovement = getPlayerMathMovement(player, predict.vector.clone().subtract(trueStartVel), location.xRot);
        Vector offset = actual.clone().subtract(predict.vector);
        trueStartVel.add(addition);
        trueStartVel.add(water);

        sb.append("Simulated: ");
        sb.append(predict.vector.toString());
        sb.append("\nActually:  ");
        sb.append(actual);
        sb.append("\nOffset Vector: ");
        sb.append(offset);
        sb.append("\nOffset: ");
        sb.append(offset.length());
        sb.append("\nLocation:  ");
        sb.append(location);
        sb.append("\nInitial velocity: ");
        sb.append(startTick);

        if (addition.lengthSquared() > 0) {
            sb.append("\nInitial vel addition: ");
            sb.append(addition);
        }
        if (water.lengthSquared() > 0) {
            sb.append("\nWater vel addition: ");
            sb.append(water);
        }

        sb.append("\nClient input:    ");
        sb.append(clientMovement);
        sb.append(" length: ");
        sb.append(clientMovement.length());
        sb.append("\nSimulated input: ");
        sb.append(simulatedMovement);
        sb.append(" length: ");
        sb.append(simulatedMovement.length());


        sb.append("\n\n");
    }

    private Vector getPlayerMathMovement(GrimPlayer player, Vector wantedMovement, float f2) {
        float f3 = player.trigHandler.sin(f2 * 0.017453292f);
        float f4 = player.trigHandler.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4);
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4);

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    @AllArgsConstructor
    private static final class Location {
        double x, y, z;
        float xRot, yRot;
        String world;

        @Override
        public String toString() {
            return "x: " + x + " y: " + y + " z: " + z + " xRot: " + xRot + " yRot: " + yRot + " world: " + world;
        }
    }
}