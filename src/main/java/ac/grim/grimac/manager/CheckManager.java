package ac.grim.grimac.manager;

import ac.grim.grimac.checks.impl.combat.Reach;
import ac.grim.grimac.checks.impl.movement.*;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.checks.type.VehicleCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;

public class CheckManager {
    ClassToInstanceMap<PacketCheck> packetChecks;
    ClassToInstanceMap<PositionCheck> positionCheck;
    ClassToInstanceMap<RotationCheck> rotationCheck;
    ClassToInstanceMap<VehicleCheck> vehicleCheck;

    public CheckManager(GrimPlayer player) {
        // Include post checks in the packet check too
        packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(Reach.class, new Reach(player))
                .put(ExplosionHandler.class, new ExplosionHandler(player))
                .put(KnockbackHandler.class, new KnockbackHandler(player))
                .put(NoFall.class, new NoFall(player))
                .build();
        positionCheck = new ImmutableClassToInstanceMap.Builder<PositionCheck>()
                .put(PredictionRunner.class, new PredictionRunner(player))
                .put(TimerCheck.class, new TimerCheck(player))
                .build();
        rotationCheck = new ImmutableClassToInstanceMap.Builder<RotationCheck>()
                .build();
        vehicleCheck = new ImmutableClassToInstanceMap.Builder<VehicleCheck>()
                .put(VehiclePredictionRunner.class, new VehiclePredictionRunner(player))
                .build();
    }

    public PositionCheck getPositionCheck(Class<? extends PositionCheck> check) {
        return positionCheck.get(check);
    }

    public RotationCheck getRotationCheck(Class<? extends RotationCheck> check) {
        return rotationCheck.get(check);
    }

    public VehicleCheck getVehicleCheck(Class<? extends VehicleCheck> check) {
        return vehicleCheck.get(check);
    }

    public void onPacketReceive(final PacketPlayReceiveEvent packet) {
        packetChecks.values().forEach(packetCheck -> packetCheck.onPacketReceive(packet));
    }

    public void onPacketSend(final PacketPlaySendEvent packet) {
        packetChecks.values().forEach(packetCheck -> packetCheck.onPacketSend(packet));
    }

    public void onPositionUpdate(final PositionUpdate position) {
        positionCheck.values().forEach(positionCheck -> positionCheck.onPositionUpdate(position));
    }

    public void onRotationUpdate(final RotationUpdate rotation) {
        rotationCheck.values().forEach(rotationCheck -> rotationCheck.process(rotation));
    }

    public void onVehiclePositionUpdate(final VehiclePositionUpdate update) {
        vehicleCheck.values().forEach(vehicleCheck -> vehicleCheck.process(update));
    }

    public ExplosionHandler getExplosionHandler() {
        return (ExplosionHandler) getPacketCheck(ExplosionHandler.class);
    }

    public PacketCheck getPacketCheck(Class<? extends PacketCheck> check) {
        return packetChecks.get(check);
    }

    public KnockbackHandler getKnockbackHandler() {
        return (KnockbackHandler) getPacketCheck(KnockbackHandler.class);
    }
}
