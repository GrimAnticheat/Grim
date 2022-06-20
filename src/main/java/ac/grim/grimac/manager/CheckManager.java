package ac.grim.grimac.manager;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.impl.aim.*;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.aim.processor.Cinematic;
import ac.grim.grimac.checks.impl.badpackets.*;
import ac.grim.grimac.checks.impl.baritone.Baritone;
import ac.grim.grimac.checks.impl.combat.Reach;
import ac.grim.grimac.checks.impl.crash.CrashA;
import ac.grim.grimac.checks.impl.crash.CrashB;
import ac.grim.grimac.checks.impl.crash.CrashD;
import ac.grim.grimac.checks.impl.groundspoof.NoFallA;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.checks.impl.misc.FastBreak;
import ac.grim.grimac.checks.impl.movement.*;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsO;
import ac.grim.grimac.checks.impl.post.PostCheck;
import ac.grim.grimac.checks.impl.prediction.DebugHandler;
import ac.grim.grimac.checks.impl.prediction.NoFallB;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.checks.impl.prediction.Phase;
import ac.grim.grimac.checks.impl.scaffolding.*;
import ac.grim.grimac.checks.impl.velocity.ExplosionHandler;
import ac.grim.grimac.checks.impl.velocity.KnockbackHandler;
import ac.grim.grimac.checks.type.*;
import ac.grim.grimac.events.packets.PacketChangeGameState;
import ac.grim.grimac.events.packets.PacketEntityReplication;
import ac.grim.grimac.events.packets.PacketPlayerAbilities;
import ac.grim.grimac.events.packets.PacketWorldBorder;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.predictionengine.SneakingEstimator;
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

    public ClassToInstanceMap<Check<?>> allChecks;

    public CheckManager(GrimPlayer player) {
        // Include post checks in the packet check too
        packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(Reach.class, new Reach(player))
                .put(PacketEntityReplication.class, new PacketEntityReplication(player))
                .put(PacketChangeGameState.class, new PacketChangeGameState(player))
                .put(ExplosionHandler.class, new ExplosionHandler(player))
                .put(KnockbackHandler.class, new KnockbackHandler(player))
                .put(CompensatedInventory.class, new CompensatedInventory(player))
                .put(PacketPlayerAbilities.class, new PacketPlayerAbilities(player))
                .put(PacketWorldBorder.class, new PacketWorldBorder(player))
                .put(ClientBrand.class, new ClientBrand(player))
                .put(NoFallA.class, new NoFallA(player))
                .put(BadPacketsO.class, new BadPacketsO(player))
                .put(BadPacketsA.class, new BadPacketsA(player))
                .put(BadPacketsB.class, new BadPacketsB(player))
                .put(BadPacketsC.class, new BadPacketsC(player))
                .put(BadPacketsD.class, new BadPacketsD(player))
                .put(BadPacketsE.class, new BadPacketsE(player))
                .put(BadPacketsF.class, new BadPacketsF(player))
                .put(BadPacketsG.class, new BadPacketsG(player))
                .put(BadPacketsH.class, new BadPacketsH(player))
                .put(CrashA.class, new CrashA(player))
                .put(CrashB.class, new CrashB(player))
                .put(CrashD.class, new CrashD(player))
                .put(BadPacketsI.class, new BadPacketsI(player))
                .put(BadPacketsJ.class, new BadPacketsJ(player))
                .put(BadPacketsK.class, new BadPacketsK(player))
                .put(BadPacketsL.class, new BadPacketsL(player))
                .put(BadPacketsM.class, new BadPacketsM(player))
                .put(BadPacketsN.class, new BadPacketsN(player))
                .put(PostCheck.class, new PostCheck(player))
                .put(FastBreak.class, new FastBreak(player))
                .put(SetbackBlocker.class, new SetbackBlocker(player)) // Must be last class otherwise we can't check while blocking packets
                .build();
        positionCheck = new ImmutableClassToInstanceMap.Builder<PositionCheck>()
                .put(PredictionRunner.class, new PredictionRunner(player))
                .put(CompensatedCooldown.class, new CompensatedCooldown(player))
                .build();
        rotationCheck = new ImmutableClassToInstanceMap.Builder<RotationCheck>()
                .put(AimProcessor.class, new AimProcessor(player))
                .put(Cinematic.class, new Cinematic(player))
                .put(AimModulo360.class, new AimModulo360(player))
                .put(AimDuplicateLook.class, new AimDuplicateLook(player))
                .put(Baritone.class, new Baritone(player))
                .build();
        vehicleCheck = new ImmutableClassToInstanceMap.Builder<VehicleCheck>()
                .put(VehiclePredictionRunner.class, new VehiclePredictionRunner(player))
                .build();

        postPredictionCheck = new ImmutableClassToInstanceMap.Builder<PostPredictionCheck>()
                .put(GhostBlockDetector.class, new GhostBlockDetector(player))
                .put(Phase.class, new Phase(player))
                .put(NoFallB.class, new NoFallB(player))
                .put(OffsetHandler.class, new OffsetHandler(player))
                .put(DebugHandler.class, new DebugHandler(player))
                .put(EntityControl.class, new EntityControl(player))
                .put(NoSlow.class, new NoSlow(player))
                .put(SetbackTeleportUtil.class, new SetbackTeleportUtil(player)) // Avoid teleporting to new position, update safe pos last
                .put(CompensatedFireworks.class, player.compensatedFireworks)
                .put(SneakingEstimator.class, new SneakingEstimator(player))
                .put(LastInstanceManager.class, new LastInstanceManager(player))
                .build();

        blockPlaceCheck = new ImmutableClassToInstanceMap.Builder<BlockPlaceCheck>()
                .put(AirLiquidPlace.class, new AirLiquidPlace(player))
                .put(FarPlace.class, new FarPlace(player))
                .put(FabricatedPlace.class, new FabricatedPlace(player))
                .put(PositionPlace.class, new PositionPlace(player))
                .put(RotationPlace.class, new RotationPlace(player))
                .build();

        timerCheck = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(TimerCheck.class, new TimerCheck(player))
                .put(VehicleTimer.class, new VehicleTimer(player))
                .build();

        allChecks = new ImmutableClassToInstanceMap.Builder<Check<?>>()
                .putAll(packetChecks)
                .putAll(positionCheck)
                .putAll(rotationCheck)
                .putAll(vehicleCheck)
                .putAll(postPredictionCheck)
                .putAll(blockPlaceCheck)
                .putAll(timerCheck)
                .build();
    }

    @SuppressWarnings("unchecked")
    public <T extends PositionCheck> T getPositionCheck(Class<T> check) {
        return (T) positionCheck.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends RotationCheck> T getRotationCheck(Class<T> check) {
        return (T) rotationCheck.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends VehicleCheck> T getVehicleCheck(Class<T> check) {
        return (T) vehicleCheck.get(check);
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

    public void onPostFlyingBlockPlace(final BlockPlace place) {
        blockPlaceCheck.values().forEach(check -> check.onPostFlyingBlockPlace(place));
    }

    public ExplosionHandler getExplosionHandler() {
        return getPacketCheck(ExplosionHandler.class);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPacketCheck(Class<T> check) {
        return (T) packetChecks.get(check);
    }

    public PacketEntityReplication getEntityReplication() {
        return getPacketCheck(PacketEntityReplication.class);
    }

    public NoFallA getNoFall() {
        return getPacketCheck(NoFallA.class);
    }

    public KnockbackHandler getKnockbackHandler() {
        return getPacketCheck(KnockbackHandler.class);
    }

    public CompensatedCooldown getCompensatedCooldown() {
        return getPositionCheck(CompensatedCooldown.class);
    }

    public NoSlow getNoSlow() {
        return getPostPredictionCheck(NoSlow.class);
    }

    public SetbackTeleportUtil getSetbackUtil() {
        return getPostPredictionCheck(SetbackTeleportUtil.class);
    }

    public DebugHandler getDebugHandler() {
        return getPostPredictionCheck(DebugHandler.class);
    }

    public OffsetHandler getOffsetHandler() {
        return getPostPredictionCheck(OffsetHandler.class);
    }

    @SuppressWarnings("unchecked")
    public <T extends PostPredictionCheck> T getPostPredictionCheck(Class<T> check) {
        return (T) postPredictionCheck.get(check);
    }
}
