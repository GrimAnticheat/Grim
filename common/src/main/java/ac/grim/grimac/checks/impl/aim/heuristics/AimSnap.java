package ac.grim.grimac.checks.impl.aim.heuristics;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.checks.type.BlockPlaceListener;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.RotationListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import ac.grim.grimac.api.config.ConfigManager;

@CheckData(name = "AimSnap", stableKey = "grim.aim.snap", description = "Snap rotation with invalid acceleration.")
public class AimSnap extends BlockPlaceCheck implements RotationListener, PacketReceiveListener, BlockPlaceListener {

    private float lastDeltaXRot, lastLastDeltaXRot;
    private float buffer;
    private float maxBuffer;
    private int lastDetectedTick;
    private int lastActionTick = -1;
    private String lastFlagValues;

    public AimSnap(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        float deltaX = rotationUpdate.getDeltaXRotABS();
        float deltaY = rotationUpdate.getDeltaYRotABS();

        // Prevent false flags with teleport.
        boolean lastPacketWasTeleport = player.packetStateData.lastPacketWasTeleport;
        // When player is riding his yaw can be abnormally like boat.
        boolean isRiding = player.compensatedEntities.self.getRiding() != null;
        // Ignore 90 and -90 pitch rotations.
        boolean lookingDownOrUp = deltaY == 90;

        if (lastPacketWasTeleport || isRiding || lookingDownOrUp) {
            return;
        }

        if (deltaX < 5F && lastDeltaXRot > 20F && lastLastDeltaXRot < 5F) {
            final double low = (deltaX + lastLastDeltaXRot) / 2;
            final double high = lastDeltaXRot;

            // Scaffold/Killaura without smooth rotation after disable?
            if (isRecentAction()) {
                buffer++;
            }

            handleFlag("rotate");

            lastFlagValues = String.format("low=%.2f, high=%.2f", low, high);
            lastDetectedTick = player.getLastTransactionReceived();
        } else {
            buffer -= Math.min(buffer, 1E-2f);
        }

        lastLastDeltaXRot = lastDeltaXRot;
        lastDeltaXRot = deltaX;
    }

    private boolean isRecentAction() {
        return player.getLastTransactionReceived() - lastActionTick <= 10;
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        lastActionTick = player.getLastTransactionReceived();
        checkPostAction("block");
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                lastActionTick = player.getLastTransactionReceived();
            }

            checkPostAction("interact");
        }
    }

    /**
     * If player start attacking or bridging after flag for snap, increase buffer or flag him.
     */
    private void checkPostAction(String action) {
        int tickDelta = player.getLastTransactionReceived() - lastDetectedTick;

        if (tickDelta < 3) {
            handleFlag(action);
        }
    }

    private void handleFlag(String action) {
        if (buffer++ > maxBuffer) {
            flag(action + " " + lastFlagValues);
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        this.maxBuffer = (float) config.getDoubleElse(getConfigName() + ".buffer", 3);
    }
}
