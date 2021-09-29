package ac.grim.grimac.manager;

import ac.grim.grimac.checks.impl.combat.Reach;
import ac.grim.grimac.checks.impl.groundspoof.NoFallA;
import ac.grim.grimac.checks.impl.movement.*;
import ac.grim.grimac.checks.impl.prediction.DebugHandler;
import ac.grim.grimac.checks.impl.prediction.NoFallB;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.checks.impl.scaffolding.AirLiquidPlace;
import ac.grim.grimac.checks.impl.velocity.ExplosionHandler;
import ac.grim.grimac.checks.impl.velocity.KnockbackHandler;
import ac.grim.grimac.checks.type.*;
import ac.grim.grimac.events.packets.patch.ResyncWorldUtil;
import ac.grim.grimac.manager.tick.impl.PositionTransactionSetter;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.utils.anticheat.update.*;
import ac.grim.grimac.utils.latency.CompensatedCooldown;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;

public class CheckManager {
    ClassToInstanceMap<PacketCheck> packetChecks;
    ClassToInstanceMap<PositionCheck> positionCheck;
    ClassToInstanceMap<RotationCheck> rotationCheck;
    ClassToInstanceMap<VehicleCheck> vehicleCheck;

    ClassToInstanceMap<BlockPlaceCheck> blockPlaceCheck;

    ClassToInstanceMap<PostPredictionCheck> postPredictionCheck;

    public CheckManager(GrimPlayer player) {
        // Include post checks in the packet check too
        packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(Reach.class, new Reach(player))
                .put(ExplosionHandler.class, new ExplosionHandler(player))
                .put(KnockbackHandler.class, new KnockbackHandler(player))
                .put(NoFallA.class, new NoFallA(player))
                .put(TimerCheck.class, new TimerCheck(player))
                .put(VehicleTimer.class, new VehicleTimer(player))
                // This desync class causes too many desync's to be used in production, blocks missing on client side
                // This has to be fixed with packet based block placing instead of spamming blocks to the player
                //.put(AntiUseItemDesync.class, new AntiUseItemDesync(player))
                .put(ResyncWorldUtil.class, new ResyncWorldUtil(player))
                .put(SetbackBlocker.class, new SetbackBlocker(player)) // Must be last class otherwise we can't check while blocking packets
                .build();
        positionCheck = new ImmutableClassToInstanceMap.Builder<PositionCheck>()
                .put(PredictionRunner.class, new PredictionRunner(player))
                .put(PositionTransactionSetter.class, new PositionTransactionSetter(player))
                .put(CompensatedCooldown.class, new CompensatedCooldown(player))
                .build();
        rotationCheck = new ImmutableClassToInstanceMap.Builder<RotationCheck>()
                .build();
        vehicleCheck = new ImmutableClassToInstanceMap.Builder<VehicleCheck>()
                .put(VehiclePredictionRunner.class, new VehiclePredictionRunner(player))
                .build();

        postPredictionCheck = new ImmutableClassToInstanceMap.Builder<PostPredictionCheck>()
                .put(GhostBlockDetector.class, new GhostBlockDetector(player))
                .put(NoFallB.class, new NoFallB(player))
                .put(OffsetHandler.class, new OffsetHandler(player))
                .put(DebugHandler.class, new DebugHandler(player))
                .put(EntityControl.class, new EntityControl(player))
                .put(NoSlow.class, new NoSlow(player))
                .put(SetbackTeleportUtil.class, new SetbackTeleportUtil(player)) // Avoid teleporting to new position, update safe pos last
                .build();

        blockPlaceCheck = new ImmutableClassToInstanceMap.Builder<BlockPlaceCheck>()
                .put(AirLiquidPlace.class, new AirLiquidPlace(player))
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
        // Allow the reach check to listen to filtered position packets
        packetChecks.values().forEach(packetCheck -> packetCheck.onPositionUpdate(position));
    }

    public void onRotationUpdate(final RotationUpdate rotation) {
        rotationCheck.values().forEach(rotationCheck -> rotationCheck.process(rotation));
    }

    public void onVehiclePositionUpdate(final VehiclePositionUpdate update) {
        vehicleCheck.values().forEach(vehicleCheck -> vehicleCheck.process(update));
    }

    public void onPredictionFinish(final PredictionComplete complete) {
        postPredictionCheck.values().forEach(predictionCheck -> predictionCheck.onPredictionComplete(complete));
    }

    public void onBlockPlace(final BlockPlace place) {
        blockPlaceCheck.values().forEach(check -> check.onBlockPlace(place));
    }

    public ExplosionHandler getExplosionHandler() {
        return (ExplosionHandler) getPacketCheck(ExplosionHandler.class);
    }

    private PacketCheck getPacketCheck(Class<? extends PacketCheck> check) {
        return packetChecks.get(check);
    }

    public Reach getReach() {
        return (Reach) getPacketCheck(Reach.class);
    }

    public NoFallA getNoFall() {
        return (NoFallA) getPacketCheck(NoFallA.class);
    }

    public KnockbackHandler getKnockbackHandler() {
        return (KnockbackHandler) getPacketCheck(KnockbackHandler.class);
    }

    public CompensatedCooldown getCompensatedCooldown() {
        return (CompensatedCooldown) getPositionCheck(CompensatedCooldown.class);
    }

    public SetbackTeleportUtil getSetbackUtil() {
        return ((SetbackTeleportUtil) getPostPredictionCheck(SetbackTeleportUtil.class));
    }

    public DebugHandler getDebugHandler() {
        return ((DebugHandler) getPostPredictionCheck(DebugHandler.class));
    }

    public OffsetHandler getOffsetHandler() {
        return ((OffsetHandler) getPostPredictionCheck(OffsetHandler.class));
    }

    public PostPredictionCheck getPostPredictionCheck(Class<? extends PostPredictionCheck> check) {
        return postPredictionCheck.get(check);
    }

    public ResyncWorldUtil getResyncWorldUtil() {
        return ((ResyncWorldUtil) getPacketCheck(ResyncWorldUtil.class));
    }
}
