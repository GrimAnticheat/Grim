package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.types.Position;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

@CheckData(name = "MultiActionsC", description = "Clicked in inventory while moving")
public class MultiActionsC extends Check implements PacketCheck {

    public MultiActionsC(GrimPlayer player) {
        super(player);
    }

    // Actual movement check (No lightning grim)
    public static boolean isActuallyMoving(GrimPlayer player) {

        // Use actualMovement from prediction
        double dx = player.actualMovement.getX();
        double dy = player.actualMovement.getY();
        double dz = player.actualMovement.getZ();

        double lenSq = dx * dx + dy * dy + dz * dz;
        double threshold = player.getMovementThreshold();
        double tSq = threshold * threshold;

        if (lenSq > tSq)
            return true;

        // Fallback
        if (player.x != player.lastX || player.z != player.lastZ)
            return true;

        // Jump
        if (player.isJumping && !player.lastJumping)
            return true;

        return false;
    }

    // Verbose builder with unified motion detector.
    @Contract(pure = true)
    public static String getVerbose(@NotNull GrimPlayer player) {
        StringJoiner verbose = new StringJoiner(", ");

        // Sprint check
        if (player.isSprinting && (!player.isSwimming || !player.clientClaimsLastOnGround)) {
            verbose.add("sprinting");
        }

        // Legacy sneak movement
        if (player.isSneaking && player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) {
            verbose.add("sneaking");
        }

        // Version check
        boolean usingInput = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_4);

        if (usingInput) {
            // Input (1.21.4+)
            if (player.packetStateData.knownInput.moving()) {
                verbose.add("input");
            }
        } else {
            // Full custom motion detector for all other versions
            if (isActuallyMoving(player)) {
                verbose.add("moving");
            }
        }

        return verbose.toString();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW
                && !player.serverOpenedInventoryThisTick) {

            String verbose = getVerbose(player);

            if (!verbose.isEmpty() && flagAndAlert(verbose) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
