package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.UncertaintyHandler;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SuperDebug extends PostPredictionCheck {
    private static final StringBuilder[] flags = new StringBuilder[1000];

    private static final HashMap<StringBuilder, Integer> continuedDebug = new HashMap<>();

    List<VectorData> predicted = new EvictingQueue<>(60);
    List<Vector> actually = new EvictingQueue<>(60);
    List<Vector> positions = new EvictingQueue<>(60);

    public SuperDebug(GrimPlayer player) {
        super(player);
    }

    public static StringBuilder getFlag(int identifier) {
        return flags[identifier];
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        for (Iterator<Map.Entry<StringBuilder, Integer>> it = continuedDebug.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<StringBuilder, Integer> debug = it.next();
            appendDebug(debug.getKey(), player.predictedVelocity, player.actualMovement, new Vector(player.x, player.y, player.z));
            debug.setValue(debug.getValue() - 1);
            if (debug.getValue() <= 0) it.remove();
        }

        predicted.add(player.predictedVelocity);
        actually.add(player.actualMovement);
        positions.add(new Vector(player.x, player.y, player.z));

        if (predictionComplete.getIdentifier() == 0) return; // 1 - 999 are valid possible values

        StringBuilder sb = new StringBuilder();
        sb.append("Player Name: ");
        sb.append(player.user.getName());
        sb.append("\nPing: ");
        sb.append(player.getTransactionPing() * 0.000001);
        sb.append("ms\n\n");

        for (int i = 0; i < predicted.size(); i++) {
            VectorData predict = predicted.get(i);
            Vector actual = actually.get(i);
            Vector position = positions.get(i);
            appendDebug(sb, predict, actual, position);
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

        flags[predictionComplete.getIdentifier()] = sb;
        continuedDebug.put(sb, 40);
    }

    private void appendDebug(StringBuilder sb, VectorData predict, Vector actual, Vector position) {
        if (predict.isZeroPointZeroThree()) {
            sb.append("Movement threshold/tick skipping\n");
        }
        if (predict.isKnockback()) {
            sb.append("Knockback\n");
        }
        if (predict.isExplosion()) {
            sb.append("Explosion\n");
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

        sb.append("Predicted: ");
        sb.append(predict.vector.toString());
        sb.append("\nActually: ");
        sb.append(actual.toString());
        sb.append("\nOffset Vector: ");
        Vector offset = actual.clone().subtract(predict.vector);
        sb.append(offset);
        sb.append("\nOffset: ");
        sb.append(offset.length());
        sb.append("\nPosition:  ");
        sb.append(position.toString());

        sb.append("\n\n");
    }
}