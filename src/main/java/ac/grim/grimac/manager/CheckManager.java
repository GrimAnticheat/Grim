package ac.grim.grimac.manager;

import ac.grim.grimac.checks.impl.aim.AimA;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.aim.processor.Cinematic;
import ac.grim.grimac.checks.impl.badpackets.*;
import ac.grim.grimac.checks.impl.combat.Reach;
import ac.grim.grimac.checks.impl.crash.CrashA;
import ac.grim.grimac.checks.impl.disabler.DisablerA;
import ac.grim.grimac.checks.impl.disabler.DisablerB;
import ac.grim.grimac.checks.impl.disabler.DisablerC;
import ac.grim.grimac.checks.impl.disabler.DisablerD;
import ac.grim.grimac.checks.impl.groundspoof.NoFallA;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.checks.impl.movement.*;
import ac.grim.grimac.checks.impl.pingspoof.PingSpoofA;
import ac.grim.grimac.checks.impl.pingspoof.PingSpoofB;
import ac.grim.grimac.checks.impl.post.*;
import ac.grim.grimac.checks.impl.prediction.DebugHandler;
import ac.grim.grimac.checks.impl.prediction.NoFallB;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.checks.impl.scaffolding.AirLiquidPlace;
import ac.grim.grimac.checks.impl.scaffolding.FarPlace;
import ac.grim.grimac.checks.impl.velocity.ExplosionHandler;
import ac.grim.grimac.checks.impl.velocity.KnockbackHandler;
import ac.grim.grimac.checks.type.*;
import ac.grim.grimac.events.packets.PacketChangeGameState;
import ac.grim.grimac.events.packets.PacketEntityReplication;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.utils.anticheat.update.*;
import ac.grim.grimac.utils.latency.CompensatedCooldown;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import ac.grim.grimac.utils.latency.CompensatedInventory;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

public class CheckManager {
    ClassToInstanceMap<PacketCheck> packetChecks;
    ClassToInstanceMap<PositionCheck> positionCheck;
    ClassToInstanceMap<RotationCheck> rotationCheck;
    ClassToInstanceMap<VehicleCheck> vehicleCheck;
    ClassToInstanceMap<PacketCheck> timerCheck;

    ClassToInstanceMap<BlockPlaceCheck> blockPlaceCheck;

    ClassToInstanceMap<PostPredictionCheck> postPredictionCheck;

    public CheckManager(GrimPlayer player) {
        // Include post checks in the packet check too
        packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(Reach.class, new Reach(player))
                .put(PacketEntityReplication.class, new PacketEntityReplication(player))
                .put(PacketChangeGameState.class, new PacketChangeGameState(player))
                .put(ExplosionHandler.class, new ExplosionHandler(player))
                .put(KnockbackHandler.class, new KnockbackHandler(player))
                .put(CompensatedInventory.class, new CompensatedInventory(player))
                .put(ClientBrand.class, new ClientBrand(player))
                .put(NoFallA.class, new NoFallA(player))
                .put(PingSpoofA.class, new PingSpoofA(player))
                .put(PingSpoofB.class, new PingSpoofB(player))
                .put(BadPacketsA.class, new BadPacketsA(player))
                .put(BadPacketsB.class, new BadPacketsB(player))
                .put(BadPacketsC.class, new BadPacketsC(player))
                .put(BadPacketsD.class, new BadPacketsD(player))
                .put(BadPacketsE.class, new BadPacketsE(player))
                .put(BadPacketsF.class, new BadPacketsF(player))
                .put(BadPacketsG.class, new BadPacketsG(player))
                .put(BadPacketsH.class, new BadPacketsH(player))
                .put(CrashA.class, new CrashA(player))
                .put(DisablerA.class, new DisablerA(player))
                .put(DisablerB.class, new DisablerB(player))
                .put(DisablerC.class, new DisablerC(player))
                .put(DisablerD.class, new DisablerD(player))
                .put(PostA.class, new PostA(player))
                .put(PostB.class, new PostB(player))
                .put(PostC.class, new PostC(player))
                .put(PostD.class, new PostD(player))
                .put(PostE.class, new PostE(player))
                .put(PostF.class, new PostF(player))
                .put(PostG.class, new PostG(player))
                .put(PostH.class, new PostH(player))
                .put(SetbackBlocker.class, new SetbackBlocker(player)) // Must be last class otherwise we can't check while blocking packets
                .build();
        positionCheck = new ImmutableClassToInstanceMap.Builder<PositionCheck>()
                .put(PredictionRunner.class, new PredictionRunner(player))
                .put(CompensatedCooldown.class, new CompensatedCooldown(player))
                .build();
        rotationCheck = new ImmutableClassToInstanceMap.Builder<RotationCheck>()
                .put(AimProcessor.class, new AimProcessor(player))
                .put(Cinematic.class, new Cinematic(player))
                .put(AimA.class, new AimA(player))
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
                .put(CompensatedFireworks.class, player.compensatedFireworks)
                .build();

        blockPlaceCheck = new ImmutableClassToInstanceMap.Builder<BlockPlaceCheck>()
                .put(AirLiquidPlace.class, new AirLiquidPlace(player))
                .put(FarPlace.class, new FarPlace(player))
                .build();

        timerCheck = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(TimerCheck.class, new TimerCheck(player))
                .put(VehicleTimer.class, new VehicleTimer(player))
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

    public void onPrePredictionReceivePacket(final PacketReceiveEvent packet) {
        timerCheck.values().forEach(check -> check.onPacketReceive(packet));
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        packetChecks.values().forEach(packetCheck -> packetCheck.onPacketReceive(packet));
    }

    public void onPacketSend(final PacketSendEvent packet) {
        timerCheck.values().forEach(check -> check.onPacketSend(packet));
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

    public PacketCheck getPacketCheck(Class<? extends PacketCheck> check) {
        return packetChecks.get(check);
    }

    public PacketEntityReplication getEntityReplication() {
        return (PacketEntityReplication) getPacketCheck(PacketEntityReplication.class);
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

    public NoSlow getNoSlow() {
        return (NoSlow) getPostPredictionCheck(NoSlow.class);
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
}
