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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import ac.grim.grimac.api.config.ConfigManager;

@CheckData(name = "AimAcceleration", stableKey = "grim.aim.acceleration", description = "Player starts rotating without acceleration.")
public class AimAcceleration extends BlockPlaceCheck implements RotationListener, PacketReceiveListener, BlockPlaceListener {

    private float lastXRotDelta;
    private float lastLastXRotDelta;
    private float xRotAccel;
    private float lastXRotAccel;
    // Tick when player starts to rotate.
    private int startRotatingTicks;
    // Last tick with a rotation.
    private float lastRotatingTick;
    // Last tick with acceleration fail.
    private int suspiciousRotationTick;
    private float buffer;
    private float maxBuffer;

    public AimAcceleration(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);

            if (!wrapper.hasRotationChanged()) {
                reset();
            }
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK)
                check();
        }
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (place.isBlock && place.getFace().getModY() == 0)
            check();
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        // Fix false positives in boats and other entities
        boolean isRiding = player.compensatedEntities.self.getRiding() != null;
        float xRotDelta = rotationUpdate.getDeltaXRotABS();
        float lastLastXRotAccel = lastXRotAccel;
        lastXRotAccel = xRotAccel;
        xRotAccel = xRotDelta - lastXRotDelta;

        if (player.packetStateData.lastPacketWasTeleport || xRotDelta == 0 || isRiding)
            return;

        // Last tick player not rotate, he probably sent flying or position packet.
        if (startRotatingTicks == -1) {
            // if player trying to simulate shor step in long distance rotation, its suspicious.
            if (lastXRotDelta > 20 && xRotDelta > 20 && player.getLastTransactionReceived() - lastRotatingTick == 2) {
                flag("Short stop");
            }

            startRotatingTicks = player.getLastTransactionReceived();
        }

        int startRotatingTickDelta = player.getLastTransactionReceived() - startRotatingTicks;

        // This means that it is second packet after start rotating or more.
        // We should not check accel with previous rotation before slow.
        if (startRotatingTickDelta > 0) {
            boolean movingFastestEnough = xRotDelta > 20 && lastXRotDelta > 20;
            boolean slowing = xRotAccel <= 0 && movingFastestEnough && (startRotatingTickDelta < 3 || lastLastXRotAccel < 1 && lastLastXRotDelta < 10);

            if (xRotDelta > 20 && slowing) {
                suspiciousRotationTick = player.getLastTransactionReceived();
            } else if (movingFastestEnough) {
                buffer -= Math.min(buffer, 0.02F);
            }
        }

        lastLastXRotDelta = lastXRotDelta;
        lastXRotDelta = xRotDelta;
        lastRotatingTick = player.getLastTransactionReceived();
    }

    private void check() {
        float tickDelta = player.getLastTransactionReceived() - suspiciousRotationTick;

        if (tickDelta < 4) {
            if (buffer++ > maxBuffer) {
                flag("" + tickDelta);
            }

            // Reset suspicious rotation tick state, because we are already handle it.
            suspiciousRotationTick = -1;
        }
    }

    private void reset() {
        startRotatingTicks = -1;
    }

    @Override
    public void onReload(ConfigManager config) {
        this.maxBuffer = (float) config.getDoubleElse(getConfigName() + ".buffer", 4);
    }
}
