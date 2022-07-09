package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.UncertaintyHandler;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.math.GrimMath;
import club.minnced.discord.webhook.WebhookClient;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.bukkit.util.Vector;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class SuperDebug extends PostPredictionCheck {
    private static WebhookClient client;

    List<VectorData> predicted = new EvictingQueue<>(100);
    List<Vector> actually = new EvictingQueue<>(100);
    List<Vector> positions = new EvictingQueue<>(100);

    int lastFlag = Integer.MIN_VALUE;
    int flagCooldown = 0; // One player may send a webhook every 5 seconds, to stop redundant debug

    public SuperDebug(GrimPlayer player) {
        super(player);
        String webhookURL = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("super-debug-webhook", "");
        if (webhookURL.isEmpty()) return;
        try {
            client = WebhookClient.withUrl(webhookURL);
        } catch (Exception ignored) {
            LogUtil.warn("Invalid super debug webhook: " + webhookURL);
            client = null;
        }
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        predicted.add(player.predictedVelocity);
        actually.add(player.actualMovement);
        positions.add(new Vector(player.x, player.y, player.z));

        flagCooldown--;

        // If we are prepared for a flag
        if (predictionComplete.getOffset() > 0.001 && flagCooldown < 0) {
            lastFlag = 40; // Send a debug in 40 movements
            flagCooldown = 100; // Don't spam flags
        }

        lastFlag--;

        // Send the flag exactly once
        if (lastFlag != 0) {
            return;
        }


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

            sb.append("Predicted: ");
            sb.append(predict.vector.toString());
            sb.append("\nActually:  ");
            sb.append(actual.toString());
            sb.append("\nOffset Vector:    ");
            Vector offset = actual.clone().subtract(predict.vector);
            sb.append(offset);
            sb.append("\nOffset: ");
            sb.append(offset.length());
            sb.append("\nPosition:  ");
            sb.append(position.toString());

            sb.append("\nkb: ");
            sb.append(predict.isKnockback());
            sb.append(" explosion: ");
            sb.append(predict.isExplosion());
            sb.append(" trident: ");
            sb.append(predict.isTrident());
            sb.append(" 0.03: ");
            sb.append(predict.isZeroPointZeroThree());
            sb.append(" swimhop: ");
            sb.append(predict.isSwimHop());
            sb.append(" jump: ");
            sb.append(predict.isJump());
            sb.append("\n\n");
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
        sb.append("\n0.03 reset:");
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
        sb.append(player.boundingBox);
        sb.append("\n");

        for (int j = GrimMath.floor(player.boundingBox.minY) - 2; j <= GrimMath.ceil(player.boundingBox.maxY) + 2; j++) {
            for (int i = GrimMath.floor(player.boundingBox.minX) - 2; i <= GrimMath.ceil(player.boundingBox.maxX) + 2; i++) {
                for (int k = GrimMath.floor(player.boundingBox.minZ) - 2; k <= GrimMath.ceil(player.boundingBox.maxZ) + 2; k++) {
                    WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(i, j, k);
                    sb.append(i);
                    sb.append(",");
                    sb.append(j);
                    sb.append(",");
                    sb.append(k);
                    sb.append(" ");
                    sb.append(block);
                    sb.append("\n");
                }
            }
        }

        client.send(sb.toString().getBytes(StandardCharsets.UTF_8), "flag.txt");
    }
}