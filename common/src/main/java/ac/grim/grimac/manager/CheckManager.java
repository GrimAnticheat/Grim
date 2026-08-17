package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.checks.impl.aim.AimDuplicateLook;
import ac.grim.grimac.checks.impl.aim.AimModulo360;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.badpackets.*;
import ac.grim.grimac.checks.impl.breaking.*;
import ac.grim.grimac.checks.impl.chat.ChatA;
import ac.grim.grimac.checks.impl.chat.ChatB;
import ac.grim.grimac.checks.impl.chat.ChatC;
import ac.grim.grimac.checks.impl.chat.ChatD;
import ac.grim.grimac.checks.impl.combat.*;
import ac.grim.grimac.checks.impl.crash.*;
import ac.grim.grimac.checks.impl.elytra.*;
import ac.grim.grimac.checks.impl.exploit.ExploitA;
import ac.grim.grimac.checks.impl.exploit.ExploitB;
import ac.grim.grimac.checks.impl.groundspoof.NoFall;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.checks.impl.misc.GhostBlockMitigation;
import ac.grim.grimac.checks.impl.misc.Post;
import ac.grim.grimac.checks.impl.misc.TransactionOrder;
import ac.grim.grimac.checks.impl.movement.NoSlow;
import ac.grim.grimac.checks.impl.movement.PredictionRunner;
import ac.grim.grimac.checks.impl.movement.SetbackBlocker;
import ac.grim.grimac.checks.impl.movement.VehiclePredictionRunner;
import ac.grim.grimac.checks.impl.multiactions.*;
import ac.grim.grimac.checks.impl.packetorder.*;
import ac.grim.grimac.checks.impl.prediction.DebugHandler;
import ac.grim.grimac.checks.impl.prediction.GroundSpoof;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.checks.impl.prediction.Phase;
import ac.grim.grimac.checks.impl.scaffolding.*;
import ac.grim.grimac.checks.impl.sprint.*;
import ac.grim.grimac.checks.impl.timer.*;
import ac.grim.grimac.checks.impl.vehicle.*;
import ac.grim.grimac.checks.impl.velocity.ExplosionHandler;
import ac.grim.grimac.checks.impl.velocity.KnockbackHandler;
import ac.grim.grimac.checks.type.*;
import ac.grim.grimac.events.packets.PacketChangeGameState;
import ac.grim.grimac.events.packets.PacketEntityReplication;
import ac.grim.grimac.events.packets.PacketPlayerAbilities;
import ac.grim.grimac.events.packets.PacketWorldBorder;
import ac.grim.grimac.internal.storage.verbose.VerboseRegistry;
import ac.grim.grimac.manager.init.start.SuperDebug;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.predictionengine.SneakingEstimator;
import ac.grim.grimac.utils.anticheat.update.*;
import ac.grim.grimac.utils.latency.CompensatedCameraEntity;
import ac.grim.grimac.utils.latency.CompensatedCooldown;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import ac.grim.grimac.utils.latency.CompensatedInventory;
import ac.grim.grimac.utils.team.TeamHandler;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class CheckManager {
    private static final AtomicBoolean initedAtomic = new AtomicBoolean(false);
    private static boolean inited;
    public final ClassToInstanceMap<AbstractCheck> checks;

    private final PreViaPacketReceiveListener[] preViaPacketReceiveListeners;
    private final PacketHandlerRegistry<PacketSendEvent> preViaPacketSendRegistry;
    private final PacketReceiveListener[] packetReceiveListeners;
    private final PacketHandlerRegistry<PacketSendEvent> packetSendRegistry;
    private final PositionListener[] positionListeners;
    private final RotationListener[] rotationListeners;
    private final VehicleListener[] vehicleListeners;
    private final PrePredictionPacketReceiveListener[] prePredictionPacketReceiveListeners;
    private final BlockBreakListener[] blockBreakListeners;
    private final BlockPlaceListener[] blockPlaceListeners;
    private final PostFlyingBlockPlaceListener[] postFlyingBlockPlaceListeners;
    private final PostFlyingBlockBreakListener[] postFlyingBlockBreakListeners;
    private final PostPredictionListener[] postPredictionListeners;

    public CheckManager(GrimPlayer player) {
        checks = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                .put(CompensatedCameraEntity.class, player.cameraEntity)
                .put(ChatA.class, new ChatA(player))
                .put(ChatB.class, new ChatB(player))
                .put(ChatC.class, new ChatC(player))
                .put(ChatD.class, new ChatD(player))
                .put(BadPacketsA.class, new BadPacketsA(player))
                .put(BadPacketsC.class, new BadPacketsC(player))
                .put(BadPacketsF.class, new BadPacketsF(player))
                .put(BadPacketsG.class, new BadPacketsG(player))
                .put(BadPacketsI.class, new BadPacketsI(player))
                .put(BadPacketsK.class, new BadPacketsK(player))
                .put(BadPacketsM.class, new BadPacketsM(player))
                .put(BadPacketsY.class, new BadPacketsY(player))
                .put(BadPacketsZ.class, new BadPacketsZ(player))
                .put(PacketOrderB.class, new PacketOrderB(player))
                .put(PacketOrderC.class, new PacketOrderC(player))
                .put(PacketOrderD.class, new PacketOrderD(player))
                .put(SelfInteract.class, new SelfInteract(player))
                .put(MultiActionsA.class, new MultiActionsA(player))
                .put(MultiActionsE.class, new MultiActionsE(player))
                .put(VehicleA.class, new VehicleA(player))
                .put(VehicleB.class, new VehicleB(player))

        // TODO: migrate the rest of these to pre-via
                .put(PacketOrderProcessor.class, player.packetOrderProcessor)
                .put(Reach.class, new Reach(player))
                .put(PacketEntityReplication.class, player.packetEntityReplication)
                .put(PacketChangeGameState.class, new PacketChangeGameState(player))
                .put(CompensatedInventory.class, player.inventory)
                .put(PacketPlayerAbilities.class, new PacketPlayerAbilities(player))
                .put(PacketWorldBorder.class, new PacketWorldBorder(player))
                .put(AttackCooldownHandler.class, player.attackCooldown)
                .put(TeamHandler.class, new TeamHandler(player))
                .put(ClientBrand.class, new ClientBrand(player))
                .put(NoFall.class, new NoFall(player))
                .put(ExploitA.class, new ExploitA(player))
                .put(ExploitB.class, new ExploitB(player))
                .put(BadPacketsD.class, new BadPacketsD(player))
                .put(BadPacketsE.class, new BadPacketsE(player))
                .put(BadPacketsJ.class, new BadPacketsJ(player))
                .put(BadPacketsL.class, new BadPacketsL(player))
                .put(BadPacketsO.class, new BadPacketsO(player))
                .put(BadPacketsP.class, new BadPacketsP(player))
                .put(BadPacketsQ.class, new BadPacketsQ(player))
                .put(BadPacketsR.class, new BadPacketsR(player))
                .put(BadPacketsS.class, new BadPacketsS(player))
                .put(BadPacketsT.class, new BadPacketsT(player))
                .put(BadPacketsU.class, new BadPacketsU(player))
                .put(BadPacketsV.class, new BadPacketsV(player))
                .put(MultiActionsC.class, new MultiActionsC(player))
                .put(MultiActionsD.class, new MultiActionsD(player))
                .put(PacketOrderO.class, new PacketOrderO(player))
//                .put(PacketOrderP.class, new PacketOrderP(player))
                .put(VehicleD.class, new VehicleD(player))
                .put(VehicleE.class, new VehicleE(player))
                .put(VehicleF.class, new VehicleF(player))
                .put(CrashB.class, new CrashB(player))
                .put(CrashD.class, new CrashD(player))
                .put(CrashE.class, new CrashE(player))
                .put(CrashF.class, new CrashF(player))
                .put(CrashH.class, new CrashH(player))
                .put(CrashI.class, new CrashI(player))
                .put(SetbackBlocker.class, new SetbackBlocker(player)) // Must be last class otherwise we can't check while blocking packets

                .put(PredictionRunner.class, new PredictionRunner(player))
                .put(CompensatedCooldown.class, new CompensatedCooldown(player))
                .put(AimProcessor.class, new AimProcessor(player))
                .put(AimModulo360.class, new AimModulo360(player))
                .put(AimDuplicateLook.class, new AimDuplicateLook(player))
                .put(VehiclePredictionRunner.class, new VehiclePredictionRunner(player))

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
                .put(SprintA.class, new SprintA(player))
                .put(SprintB.class, new SprintB(player))
                .put(SprintC.class, new SprintC(player))
                .put(SprintD.class, new SprintD(player))
                .put(SprintE.class, new SprintE(player))
                .put(SprintF.class, new SprintF(player))
                .put(SprintG.class, new SprintG(player))
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

                .put(InvalidPlaceA.class, new InvalidPlaceA(player))
                .put(InvalidPlaceB.class, new InvalidPlaceB(player))
                .put(AirLiquidPlace.class, new AirLiquidPlace(player))
                .put(MultiPlace.class, new MultiPlace(player))
                .put(MultiActionsF.class, new MultiActionsF(player))
                .put(MultiActionsG.class, new MultiActionsG(player))
                .put(BadPacketsH.class, new BadPacketsH(player))
                .put(CrashG.class, new CrashG(player))
                .put(FarPlace.class, new FarPlace(player))
                .put(FabricatedPlace.class, new FabricatedPlace(player))
                .put(PositionPlace.class, new PositionPlace(player))
                .put(RotationPlace.class, new RotationPlace(player))
                .put(PacketOrderN.class, new PacketOrderN(player))
                .put(DuplicateRotPlace.class, new DuplicateRotPlace(player))
                .put(GhostBlockMitigation.class, new GhostBlockMitigation(player))

                .put(Timer.class, new Timer(player))
                .put(TickTimer.class, new TickTimer(player))
                .put(TimerLimit.class, new TimerLimit(player))
                .put(CrashA.class, new CrashA(player))
                .put(CrashC.class, new CrashC(player))
                .put(VehicleTimer.class, new VehicleTimer(player))

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
                .put(MultiActionsB.class, new MultiActionsB(player))

        // All checks that have no listeners, generally invoked by other code to flag
        // TODO migrate more checks to here
                // BadPacketsB/N/W, VehicleC, and TransactionOrder are packet checks with no listener
                .put(BadPacketsB.class, new BadPacketsB(player))
                .put(BadPacketsN.class, new BadPacketsN(player))
                .put(BadPacketsW.class, new BadPacketsW(player))
                .put(TransactionOrder.class, new TransactionOrder(player))
                .put(VehicleC.class, new VehicleC(player))
                .put(Hitboxes.class, new Hitboxes(player)) // Hitboxes is invoked by Reach
                .build();

        ArrayList<PreViaPacketReceiveListener> preViaPacketReceiveListeners = new ArrayList<>();
        this.preViaPacketSendRegistry = new PacketHandlerRegistry<>();
        ArrayList<PacketReceiveListener> packetReceiveListeners = new ArrayList<>();
        this.packetSendRegistry = new PacketHandlerRegistry<>();
        ArrayList<PrePredictionPacketReceiveListener> prePredictionPacketReceiveListeners = new ArrayList<>();
        ArrayList<PositionListener> positionListeners = new ArrayList<>();
        ArrayList<RotationListener> rotationListeners = new ArrayList<>();
        ArrayList<VehicleListener> vehicleListeners = new ArrayList<>();
        ArrayList<PostPredictionListener> postPredictionListeners = new ArrayList<>();
        ArrayList<BlockPlaceListener> blockPlaceListeners = new ArrayList<>();
        ArrayList<PostFlyingBlockPlaceListener> postFlyingBlockPlaceListeners = new ArrayList<>();
        ArrayList<BlockBreakListener> blockBreakListeners = new ArrayList<>();
        ArrayList<PostFlyingBlockBreakListener> postFlyingBlockBreakListeners = new ArrayList<>();

        for (AbstractCheck check : checks.values()) {
            if (check instanceof Check grimCheck && !grimCheck.isApplicable()) continue;

            if (check instanceof PacketReceiveListener packetReceiveListener) packetReceiveListeners.add(packetReceiveListener);
            if (check instanceof PrePredictionPacketReceiveListener prePredictionPacketReceiveListener) prePredictionPacketReceiveListeners.add(prePredictionPacketReceiveListener);
            if (check instanceof PreViaPacketReceiveListener preViaPacketReceiveListener) preViaPacketReceiveListeners.add(preViaPacketReceiveListener);
            if (check instanceof PacketSendListener packetSendListener) packetSendListener.registerSend(packetSendRegistry);;
            if (check instanceof PreViaPacketSendListener preViaPacketSendListener) preViaPacketSendListener.registerPreViaSend(preViaPacketSendRegistry);
            if (check instanceof PositionListener positionListener) positionListeners.add(positionListener);
            if (check instanceof RotationListener rotationListener) rotationListeners.add(rotationListener);
            if (check instanceof VehicleListener vehicleListener) vehicleListeners.add(vehicleListener);
            if (check instanceof PostPredictionListener postPredictionListener) postPredictionListeners.add(postPredictionListener);
            if (check instanceof BlockPlaceListener blockPlaceListener) blockPlaceListeners.add(blockPlaceListener);
            if (check instanceof PostFlyingBlockPlaceListener postFlyingBlockPlaceListener) postFlyingBlockPlaceListeners.add(postFlyingBlockPlaceListener);
            if (check instanceof BlockBreakListener blockBreakListener) blockBreakListeners.add(blockBreakListener);
            if (check instanceof PostFlyingBlockBreakListener postFlyingBlockBreakListener) postFlyingBlockBreakListeners.add(postFlyingBlockBreakListener);
        }

        this.preViaPacketReceiveListeners = preViaPacketReceiveListeners.toArray(new PreViaPacketReceiveListener[preViaPacketReceiveListeners.size()]);
        this.packetReceiveListeners = packetReceiveListeners.toArray(new PacketReceiveListener[packetReceiveListeners.size()]);
        this.prePredictionPacketReceiveListeners = prePredictionPacketReceiveListeners.toArray(new PrePredictionPacketReceiveListener[prePredictionPacketReceiveListeners.size()]);
        this.positionListeners = positionListeners.toArray(new PositionListener[positionListeners.size()]);
        this.rotationListeners = rotationListeners.toArray(new RotationListener[rotationListeners.size()]);
        this.vehicleListeners = vehicleListeners.toArray(new VehicleListener[vehicleListeners.size()]);
        this.postPredictionListeners = postPredictionListeners.toArray(new PostPredictionListener[postPredictionListeners.size()]);
        this.blockPlaceListeners = blockPlaceListeners.toArray(new BlockPlaceListener[blockPlaceListeners.size()]);
        this.postFlyingBlockPlaceListeners = postFlyingBlockPlaceListeners.toArray(new PostFlyingBlockPlaceListener[postFlyingBlockPlaceListeners.size()]);
        this.blockBreakListeners = blockBreakListeners.toArray(new BlockBreakListener[blockBreakListeners.size()]);
        this.postFlyingBlockBreakListeners = postFlyingBlockBreakListeners.toArray(new PostFlyingBlockBreakListener[postFlyingBlockBreakListeners.size()]);

        init();
    }

    private void registerBuiltInVerboseTemplates() {
        VerboseRegistry registry = GrimAPI.INSTANCE.getDataStoreLifecycle().verboseRegistry();
        if (registry == null) return;
        registry.registerTemplates(() -> {
            for (AbstractCheck check : checks.values()) {
                if (check instanceof Check grimCheck) {
                    grimCheck.registerVerboseTemplates(registry);
                }
            }
        });
    }

    public <T extends AbstractCheck> T getCheck(Class<T> check) {
        return checks.getInstance(check);
    }

    public void onPrePredictionReceivePacket(final PacketReceiveEvent packet) {
        for (PrePredictionPacketReceiveListener check : prePredictionPacketReceiveListeners) {
            check.onPrePredictionPacketReceive(packet);
        }
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        for (PacketReceiveListener check : packetReceiveListeners) {
            check.onPacketReceive(packet);
        }
    }

    public void onPreViaPacketReceive(final PacketReceiveEvent packet) {
        for (PreViaPacketReceiveListener check : preViaPacketReceiveListeners) {
            check.onPreViaPacketReceive(packet);
        }
    }

    public void onPacketSend(final PacketSendEvent packet) {
        packetSendRegistry.handle(packet);
    }

    public void onPreViaPacketSend(final PacketSendEvent packet) {
        preViaPacketSendRegistry.handle(packet);
    }

    public void onPositionUpdate(final PositionUpdate position) {
        for (PositionListener check : positionListeners) {
            check.onPositionUpdate(position);
        }
    }

    public void onRotationUpdate(final RotationUpdate rotation) {
        for (RotationListener check : rotationListeners) {
            check.process(rotation);
        }
    }

    public void onVehiclePositionUpdate(final VehiclePositionUpdate update) {
        for (VehicleListener check : vehicleListeners) {
            check.process(update);
        }
    }

    public void onPredictionFinish(final PredictionComplete complete) {
        for (PostPredictionListener check : postPredictionListeners) {
            check.onPredictionComplete(complete);
        }
    }

    public void onBlockPlace(final BlockPlace place) {
        for (BlockPlaceListener check : blockPlaceListeners) {
            check.onBlockPlace(place);
        }
    }

    public void onPostFlyingBlockPlace(final BlockPlace place) {
        for (PostFlyingBlockPlaceListener check : postFlyingBlockPlaceListeners) {
            check.onPostFlyingBlockPlace(place);
        }
    }

    public void onBlockBreak(final BlockBreak blockBreak) {
        for (BlockBreakListener check : blockBreakListeners) {
            check.onBlockBreak(blockBreak);
        }
    }

    public void onPostFlyingBlockBreak(final BlockBreak blockBreak) {
        for (PostFlyingBlockBreakListener check : postFlyingBlockBreakListeners) {
            check.onPostFlyingBlockBreak(blockBreak);
        }
    }

    public ExplosionHandler getExplosionHandler() {
        return getCheck(ExplosionHandler.class);
    }

    public NoFall getNoFall() {
        return getCheck(NoFall.class);
    }

    public KnockbackHandler getKnockbackHandler() {
        return getCheck(KnockbackHandler.class);
    }

    public CompensatedCooldown getCompensatedCooldown() {
        return getCheck(CompensatedCooldown.class);
    }

    public NoSlow getNoSlow() {
        return getCheck(NoSlow.class);
    }

    public SetbackTeleportUtil getSetbackUtil() {
        return getCheck(SetbackTeleportUtil.class);
    }

    public DebugHandler getDebugHandler() {
        return getCheck(DebugHandler.class);
    }

    private void init() {
        if (inited || initedAtomic.getAndSet(true)) return;
        inited = true;

        registerBuiltInVerboseTemplates();

        final String[] permissions = {
                "grim.exempt.",
                "grim.nosetback.",
                "grim.nomodifypacket.",
        };

        for (final AbstractCheck check : checks.values()) {
            if (check.getConfigName() == null) continue;
            final String id = check.getConfigName().toLowerCase();
            for (String permissionName : permissions) {
                GrimAPI.INSTANCE.getPermissionManager().registerPermission(permissionName + id, PermissionDefaultValue.FALSE);
            }
        }
    }
}
