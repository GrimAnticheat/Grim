package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.impl.aim.AimDuplicateLook;
import ac.grim.grimac.checks.impl.aim.AimModulo360;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.badpackets.*;
import ac.grim.grimac.checks.impl.breaking.AirLiquidBreak;
import ac.grim.grimac.checks.impl.breaking.FarBreak;
import ac.grim.grimac.checks.impl.breaking.FastBreak;
import ac.grim.grimac.checks.impl.breaking.InvalidBreak;
import ac.grim.grimac.checks.impl.breaking.MultiBreak;
import ac.grim.grimac.checks.impl.breaking.NoSwingBreak;
import ac.grim.grimac.checks.impl.breaking.PositionBreakA;
import ac.grim.grimac.checks.impl.breaking.PositionBreakB;
import ac.grim.grimac.checks.impl.breaking.RotationBreak;
import ac.grim.grimac.checks.impl.breaking.WrongBreak;
import ac.grim.grimac.checks.impl.combat.Hitboxes;
import ac.grim.grimac.checks.impl.combat.MultiInteractA;
import ac.grim.grimac.checks.impl.combat.MultiInteractB;
import ac.grim.grimac.checks.impl.combat.Reach;
import ac.grim.grimac.checks.impl.crash.*;
import ac.grim.grimac.checks.impl.elytra.ElytraA;
import ac.grim.grimac.checks.impl.elytra.ElytraB;
import ac.grim.grimac.checks.impl.elytra.ElytraC;
import ac.grim.grimac.checks.impl.elytra.ElytraD;
import ac.grim.grimac.checks.impl.elytra.ElytraE;
import ac.grim.grimac.checks.impl.elytra.ElytraF;
import ac.grim.grimac.checks.impl.elytra.ElytraG;
import ac.grim.grimac.checks.impl.elytra.ElytraH;
import ac.grim.grimac.checks.impl.elytra.ElytraI;
import ac.grim.grimac.checks.impl.exploit.ExploitA;
import ac.grim.grimac.checks.impl.exploit.ExploitB;
import ac.grim.grimac.checks.impl.exploit.ExploitC;
import ac.grim.grimac.checks.impl.groundspoof.NoFall;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.checks.impl.misc.GhostBlockMitigation;
import ac.grim.grimac.checks.impl.misc.TransactionOrder;
import ac.grim.grimac.checks.impl.movement.NoSlow;
import ac.grim.grimac.checks.impl.movement.PredictionRunner;
import ac.grim.grimac.checks.impl.movement.SetbackBlocker;
import ac.grim.grimac.checks.impl.movement.VehiclePredictionRunner;
import ac.grim.grimac.checks.impl.multiactions.*;
import ac.grim.grimac.checks.impl.packetorder.*;
import ac.grim.grimac.checks.impl.misc.Post;
import ac.grim.grimac.checks.impl.prediction.DebugHandler;
import ac.grim.grimac.checks.impl.prediction.GroundSpoof;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.checks.impl.prediction.Phase;
import ac.grim.grimac.checks.impl.scaffolding.AirLiquidPlace;
import ac.grim.grimac.checks.impl.scaffolding.DuplicateRotPlace;
import ac.grim.grimac.checks.impl.scaffolding.FabricatedPlace;
import ac.grim.grimac.checks.impl.scaffolding.FarPlace;
import ac.grim.grimac.checks.impl.scaffolding.InvalidPlaceA;
import ac.grim.grimac.checks.impl.scaffolding.InvalidPlaceB;
import ac.grim.grimac.checks.impl.scaffolding.MultiPlace;
import ac.grim.grimac.checks.impl.scaffolding.PositionPlace;
import ac.grim.grimac.checks.impl.scaffolding.RotationPlace;
import ac.grim.grimac.checks.impl.sprint.SprintA;
import ac.grim.grimac.checks.impl.sprint.SprintB;
import ac.grim.grimac.checks.impl.sprint.SprintC;
import ac.grim.grimac.checks.impl.sprint.SprintD;
import ac.grim.grimac.checks.impl.sprint.SprintE;
import ac.grim.grimac.checks.impl.sprint.SprintF;
import ac.grim.grimac.checks.impl.timer.NegativeTimer;
import ac.grim.grimac.checks.impl.timer.TickTimer;
import ac.grim.grimac.checks.impl.timer.Timer;
import ac.grim.grimac.checks.impl.timer.TimerLimit;
import ac.grim.grimac.checks.impl.timer.VehicleTimer;
import ac.grim.grimac.checks.impl.vehicle.VehicleA;
import ac.grim.grimac.checks.impl.vehicle.VehicleB;
import ac.grim.grimac.checks.impl.vehicle.VehicleC;
import ac.grim.grimac.checks.impl.vehicle.VehicleD;
import ac.grim.grimac.checks.impl.velocity.ExplosionHandler;
import ac.grim.grimac.checks.impl.velocity.KnockbackHandler;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.checks.type.VehicleCheck;
import ac.grim.grimac.events.packets.PacketChangeGameState;
import ac.grim.grimac.events.packets.PacketEntityReplication;
import ac.grim.grimac.events.packets.PacketPlayerAbilities;
import ac.grim.grimac.events.packets.PacketWorldBorder;
import ac.grim.grimac.manager.init.start.SuperDebug;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.predictionengine.SneakingEstimator;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;
import ac.grim.grimac.utils.latency.CompensatedCooldown;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import ac.grim.grimac.utils.latency.CompensatedInventory;
import ac.grim.grimac.utils.team.TeamHandler;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

import java.util.concurrent.atomic.AtomicBoolean;

public class CheckManager {
    private static final AtomicBoolean initedAtomic = new AtomicBoolean(false);
    private static boolean inited;
    public ClassToInstanceMap<AbstractCheck> allChecks;
    ClassToInstanceMap<PacketCheck> packetChecks;
    ClassToInstanceMap<PositionCheck> positionCheck;
    ClassToInstanceMap<RotationCheck> rotationCheck;
    ClassToInstanceMap<VehicleCheck> vehicleCheck;
    ClassToInstanceMap<PacketCheck> prePredictionChecks;
    ClassToInstanceMap<BlockBreakCheck> blockBreakChecks;
    ClassToInstanceMap<BlockPlaceCheck> blockPlaceCheck;
    ClassToInstanceMap<PostPredictionCheck> postPredictionCheck;
    private PacketEntityReplication packetEntityReplication = null;
    private CompensatedInventory inventory = null;

    public CheckManager(GrimPlayer player) {
        // Include post checks in the packet check too
        packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(PacketOrderProcessor.class, player.packetOrderProcessor)
                .put(Reach.class, new Reach(player))
                .put(PacketEntityReplication.class, new PacketEntityReplication(player))
                .put(PacketChangeGameState.class, new PacketChangeGameState(player))
                .put(CompensatedInventory.class, new CompensatedInventory(player))
                .put(PacketPlayerAbilities.class, new PacketPlayerAbilities(player))
                .put(PacketWorldBorder.class, new PacketWorldBorder(player))
                .put(ActionManager.class, player.actionManager)
                .put(TeamHandler.class, new TeamHandler(player))
                .put(ClientBrand.class, new ClientBrand(player))
                .put(NoFall.class, new NoFall(player))
                .put(ExploitA.class, new ExploitA(player))
                .put(ExploitB.class, new ExploitB(player))
                .put(ExploitC.class, new ExploitC(player))
                .put(BadPacketsA.class, new BadPacketsA(player))
                .put(BadPacketsB.class, new BadPacketsB(player))
                .put(BadPacketsC.class, new BadPacketsC(player))
                .put(BadPacketsD.class, new BadPacketsD(player))
                .put(BadPacketsE.class, new BadPacketsE(player))
                .put(BadPacketsF.class, new BadPacketsF(player))
                .put(BadPacketsG.class, new BadPacketsG(player))
                .put(BadPacketsI.class, new BadPacketsI(player))
                .put(BadPacketsJ.class, new BadPacketsJ(player))
                .put(BadPacketsK.class, new BadPacketsK(player))
                .put(BadPacketsL.class, new BadPacketsL(player))
                .put(BadPacketsM.class, new BadPacketsM(player))
                .put(BadPacketsO.class, new BadPacketsO(player))
                .put(BadPacketsP.class, new BadPacketsP(player))
                .put(BadPacketsQ.class, new BadPacketsQ(player))
                .put(BadPacketsR.class, new BadPacketsR(player))
                .put(BadPacketsS.class, new BadPacketsS(player))
                .put(BadPacketsT.class, new BadPacketsT(player))
                .put(BadPacketsU.class, new BadPacketsU(player))
                .put(BadPacketsV.class, new BadPacketsV(player))
                .put(BadPacketsY.class, new BadPacketsY(player))
                .put(MultiActionsA.class, new MultiActionsA(player))
                .put(MultiActionsB.class, new MultiActionsB(player))
                .put(MultiActionsC.class, new MultiActionsC(player))
                .put(MultiActionsD.class, new MultiActionsD(player))
                .put(MultiActionsE.class, new MultiActionsE(player))
                .put(MultiActionsG.class, new MultiActionsG(player))
                .put(PacketOrderB.class, new PacketOrderB(player))
                .put(PacketOrderC.class, new PacketOrderC(player))
                .put(PacketOrderD.class, new PacketOrderD(player))
                .put(SprintA.class, new SprintA(player))
                .put(VehicleA.class, new VehicleA(player))
                .put(VehicleB.class, new VehicleB(player))
                .put(VehicleD.class, new VehicleD(player))
                .put(CrashB.class, new CrashB(player))
                .put(CrashD.class, new CrashD(player))
                .put(CrashE.class, new CrashE(player))
                .put(CrashF.class, new CrashF(player))
                .put(CrashH.class, new CrashH(player))
                .put(CrashI.class, new CrashI(player))
                .put(SetbackBlocker.class, new SetbackBlocker(player)) // Must be last class otherwise we can't check while blocking packets
                .build();
        positionCheck = new ImmutableClassToInstanceMap.Builder<PositionCheck>()
                .put(PredictionRunner.class, new PredictionRunner(player))
                .put(CompensatedCooldown.class, new CompensatedCooldown(player))
                .build();
        rotationCheck = new ImmutableClassToInstanceMap.Builder<RotationCheck>()
                .put(AimProcessor.class, new AimProcessor(player))
                .put(AimModulo360.class, new AimModulo360(player))
                .put(AimDuplicateLook.class, new AimDuplicateLook(player))
//                .put(Baritone.class, new Baritone(player))
                .build();
        vehicleCheck = new ImmutableClassToInstanceMap.Builder<VehicleCheck>()
                .put(VehiclePredictionRunner.class, new VehiclePredictionRunner(player))
                .build();

        postPredictionCheck = new ImmutableClassToInstanceMap.Builder<PostPredictionCheck>()
                .put(NegativeTimer.class, new NegativeTimer(player))
                .put(ExplosionHandler.class, new ExplosionHandler(player))
                .put(KnockbackHandler.class, new KnockbackHandler(player))
                .put(GhostBlockDetector.class, new GhostBlockDetector(player))
                .put(Phase.class, new Phase(player))
                .put(Post.class, new Post(player))
                .put(PacketOrderA.class, new PacketOrderA(player))
                .put(PacketOrderE.class, new PacketOrderE(player))
                .put(PacketOrderF.class, new PacketOrderF(player))
                .put(PacketOrderG.class, new PacketOrderG(player))
                .put(PacketOrderH.class, new PacketOrderH(player))
                .put(PacketOrderI.class, new PacketOrderI(player))
                .put(PacketOrderJ.class, new PacketOrderJ(player))
                .put(PacketOrderK.class, new PacketOrderK(player))
                .put(PacketOrderL.class, new PacketOrderL(player))
                .put(PacketOrderM.class, new PacketOrderM(player))
                .put(GroundSpoof.class, new GroundSpoof(player))
                .put(OffsetHandler.class, new OffsetHandler(player))
                .put(SuperDebug.class, new SuperDebug(player))
                .put(DebugHandler.class, new DebugHandler(player))
                .put(BadPacketsX.class, new BadPacketsX(player))
                .put(NoSlow.class, new NoSlow(player))
                .put(SprintB.class, new SprintB(player))
                .put(SprintC.class, new SprintC(player))
                .put(SprintD.class, new SprintD(player))
                .put(SprintE.class, new SprintE(player))
                .put(SprintF.class, new SprintF(player))
                .put(MultiInteractA.class, new MultiInteractA(player))
                .put(MultiInteractB.class, new MultiInteractB(player))
                .put(ElytraA.class, new ElytraA(player))
                .put(ElytraB.class, new ElytraB(player))
                .put(ElytraC.class, new ElytraC(player))
                .put(ElytraD.class, new ElytraD(player))
                .put(ElytraE.class, new ElytraE(player))
                .put(ElytraF.class, new ElytraF(player))
                .put(ElytraG.class, new ElytraG(player))
                .put(ElytraH.class, new ElytraH(player))
                .put(ElytraI.class, new ElytraI(player))
                .put(SetbackTeleportUtil.class, new SetbackTeleportUtil(player)) // Avoid teleporting to new position, update safe pos last
                .put(CompensatedFireworks.class, player.fireworks)
                .put(SneakingEstimator.class, new SneakingEstimator(player))
                .put(LastInstanceManager.class, player.lastInstanceManager)
                .build();

        blockPlaceCheck = new ImmutableClassToInstanceMap.Builder<BlockPlaceCheck>()
                .put(InvalidPlaceA.class, new InvalidPlaceA(player))
                .put(InvalidPlaceB.class, new InvalidPlaceB(player))
                .put(AirLiquidPlace.class, new AirLiquidPlace(player))
                .put(MultiPlace.class, new MultiPlace(player))
                .put(MultiActionsF.class, new MultiActionsF(player))
                .put(BadPacketsH.class, new BadPacketsH(player))
                .put(CrashG.class, new CrashG(player))
                .put(FarPlace.class, new FarPlace(player))
                .put(FabricatedPlace.class, new FabricatedPlace(player))
                .put(PositionPlace.class, new PositionPlace(player))
                .put(RotationPlace.class, new RotationPlace(player))
                .put(PacketOrderN.class, new PacketOrderN(player))
                .put(DuplicateRotPlace.class, new DuplicateRotPlace(player))
                .put(GhostBlockMitigation.class, new GhostBlockMitigation(player))
                .build();

        prePredictionChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(Timer.class, new Timer(player))
                .put(TickTimer.class, new TickTimer(player))
                .put(TimerLimit.class, new TimerLimit(player))
                .put(CrashA.class, new CrashA(player))
                .put(CrashC.class, new CrashC(player))
                .put(VehicleTimer.class, new VehicleTimer(player))
                .build();

        blockBreakChecks = new ImmutableClassToInstanceMap.Builder<BlockBreakCheck>()
                .put(AirLiquidBreak.class, new AirLiquidBreak(player))
                .put(WrongBreak.class, new WrongBreak(player))
                .put(RotationBreak.class, new RotationBreak(player))
                .put(FastBreak.class, new FastBreak(player))
                .put(MultiBreak.class, new MultiBreak(player))
                .put(NoSwingBreak.class, new NoSwingBreak(player))
                .put(FarBreak.class, new FarBreak(player))
                .put(InvalidBreak.class, new InvalidBreak(player))
                .put(PositionBreakA.class, new PositionBreakA(player))
                .put(PositionBreakB.class, new PositionBreakB(player))
                .build();

        // All checks that have no listeners, generally invoked by other code to flag
        // TODO migrate more checks to here
        ClassToInstanceMap<AbstractCheck> noneModules = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                // BadPacketsN/W + VehicleC + TransactionOrder are packet checks with no listener
                .put(BadPacketsN.class, new BadPacketsN(player))
                .put(BadPacketsW.class, new BadPacketsW(player))
                .put(TransactionOrder.class, new TransactionOrder(player))
                .put(VehicleC.class, new VehicleC(player))
                .put(Hitboxes.class, new Hitboxes(player)) // Hitboxes is invoked by Reach
                .build();

        allChecks = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                .putAll(packetChecks)
                .putAll(positionCheck)
                .putAll(rotationCheck)
                .putAll(vehicleCheck)
                .putAll(postPredictionCheck)
                .putAll(blockPlaceCheck)
                .putAll(prePredictionChecks)
                .putAll(blockBreakChecks)
                .putAll(noneModules)
                .build();

        init();
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractCheck> T getCheck(Class<T> check) {
        return (T) allChecks.get(check);
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
    public <T extends BlockPlaceCheck> T getBlockPlaceCheck(Class<T> check) {
        return (T) blockPlaceCheck.get(check);
    }

    public void onPrePredictionReceivePacket(final PacketReceiveEvent packet) {
        for (PacketCheck check : prePredictionChecks.values()) {
            check.onPacketReceive(packet);
        }
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        for (PacketCheck check : packetChecks.values()) {
            check.onPacketReceive(packet);
        }
        for (PostPredictionCheck check : postPredictionCheck.values()) {
            check.onPacketReceive(packet);
        }
        for (BlockPlaceCheck check : blockPlaceCheck.values()) {
            check.onPacketReceive(packet);
        }
        for (BlockBreakCheck check : blockBreakChecks.values()) {
            check.onPacketReceive(packet);
        }
    }

    public void onPacketSend(final PacketSendEvent packet) {
        for (PacketCheck check : prePredictionChecks.values()) {
            check.onPacketSend(packet);
        }
        for (PacketCheck check : packetChecks.values()) {
            check.onPacketSend(packet);
        }
        for (PostPredictionCheck check : postPredictionCheck.values()) {
            check.onPacketSend(packet);
        }
        for (BlockPlaceCheck check : blockPlaceCheck.values()) {
            check.onPacketSend(packet);
        }
        for (BlockBreakCheck check : blockBreakChecks.values()) {
            check.onPacketSend(packet);
        }
    }

    public void onPositionUpdate(final PositionUpdate position) {
        for (PositionCheck check : positionCheck.values()) {
            check.onPositionUpdate(position);
        }
    }

    public void onRotationUpdate(final RotationUpdate rotation) {
        for (RotationCheck check : rotationCheck.values()) {
            check.process(rotation);
        }
        for (BlockPlaceCheck check : blockPlaceCheck.values()) {
            check.process(rotation);
        }
    }

    public void onVehiclePositionUpdate(final VehiclePositionUpdate update) {
        for (VehicleCheck check : vehicleCheck.values()) {
            check.process(update);
        }
    }

    public void onPredictionFinish(final PredictionComplete complete) {
        for (PostPredictionCheck check : postPredictionCheck.values()) {
            check.onPredictionComplete(complete);
        }
        for (BlockPlaceCheck check : blockPlaceCheck.values()) {
            check.onPredictionComplete(complete);
        }
        for (BlockBreakCheck check : blockBreakChecks.values()) {
            check.onPredictionComplete(complete);
        }
    }

    public void onBlockPlace(final BlockPlace place) {
        for (BlockPlaceCheck check : blockPlaceCheck.values()) {
            check.onBlockPlace(place);
        }
    }

    public void onPostFlyingBlockPlace(final BlockPlace place) {
        for (BlockPlaceCheck check : blockPlaceCheck.values()) {
            check.onPostFlyingBlockPlace(place);
        }
    }

    public void onBlockBreak(final BlockBreak blockBreak) {
        for (BlockBreakCheck check : blockBreakChecks.values()) {
            check.onBlockBreak(blockBreak);
        }
        for (BlockPlaceCheck check : blockPlaceCheck.values()) {
            check.onBlockBreak(blockBreak);
        }
    }

    public void onPostFlyingBlockBreak(final BlockBreak blockBreak) {
        for (BlockBreakCheck check : blockBreakChecks.values()) {
            check.onPostFlyingBlockBreak(blockBreak);
        }
        for (BlockPlaceCheck check : blockPlaceCheck.values()) {
            check.onPostFlyingBlockBreak(blockBreak);
        }
    }

    public ExplosionHandler getExplosionHandler() {
        return getPostPredictionCheck(ExplosionHandler.class);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPacketCheck(Class<T> check) {
        return (T) packetChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPrePredictionCheck(Class<T> check) {
        return (T) prePredictionChecks.get(check);
    }

    public PacketEntityReplication getEntityReplication() {
        if (packetEntityReplication == null)
            packetEntityReplication = getPacketCheck(PacketEntityReplication.class);
        return packetEntityReplication;
    }

    public NoFall getNoFall() {
        return getPacketCheck(NoFall.class);
    }

    public CompensatedInventory getInventory() {
        if (inventory == null) inventory = getPacketCheck(CompensatedInventory.class);
        return inventory;
    }

    public KnockbackHandler getKnockbackHandler() {
        return getPostPredictionCheck(KnockbackHandler.class);
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

    private void init() {
        if (inited || initedAtomic.getAndSet(true)) return;
        inited = true;

        final String[] permissions = {
                "grim.exempt.",
                "grim.nosetback.",
                "grim.nomodifypacket.",
        };

        for (final AbstractCheck check : allChecks.values()) {
            if (check.getConfigName() == null) continue;
            final String id = check.getConfigName().toLowerCase();
            for (String permissionName : permissions) {
                permissionName += id;
                GrimAPI.INSTANCE.getPermissionManager().registerPermission(permissionName, PermissionDefaultValue.FALSE);
            }
        }
    }
}
