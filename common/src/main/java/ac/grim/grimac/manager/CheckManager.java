package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.AbstractProcessor;
import ac.grim.grimac.api.common.BasicReloadable;
import ac.grim.grimac.checks.Check;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class CheckManager implements BasicReloadable {
    private static final AtomicBoolean initedAtomic = new AtomicBoolean(false);
    private static boolean inited;
    public final ClassToInstanceMap<AbstractProcessor> processors;
    public final Collection<AbstractCheck> checks;

    private final PreViaPacketReceiveListener[] preViaPacketReceiveListeners;
    private final PreViaPacketSendListener[] preViaPacketSendListeners;
    private final PacketReceiveListener[] packetReceiveListeners;
    private final PacketSendListener[] packetSendListeners;
    private final PositionListener[] positionListeners;
    private final RotationListener[] rotationListeners;
    private final VehicleListener[] vehicleListeners;
    private final PrePredictionPacketReceiveListener[] prePredictionPacketReceiveListeners;
    private final BlockBreakListener[] blockBreakListeners;
    private final BlockPlaceListener[] blockPlaceListeners;
    private final PostFlyingBlockPlaceListener[] postFlyingBlockPlaceListeners;
    private final PostFlyingBlockBreakListener[] postFlyingBlockBreakListeners;
    private final PostPredictionListener[] postPredictionListeners;

    public CheckManager(@NotNull GrimPlayer player) {
        // TODO: migrate all packet listeners to pre-via
        processors = new ImmutableClassToInstanceMap.Builder<AbstractProcessor>()
                // processors that must run before checks
                .put(PacketOrderProcessor.class, player.packetOrderProcessor)
                .put(AimProcessor.class, new AimProcessor(player))

                // other non-check processors
                .put(GhostBlockDetector.class, new GhostBlockDetector(player))
                .put(GhostBlockMitigation.class, new GhostBlockMitigation(player))
                .put(PredictionRunner.class, new PredictionRunner(player))
                .put(CompensatedCooldown.class, new CompensatedCooldown(player))
                .put(VehiclePredictionRunner.class, new VehiclePredictionRunner(player))
                .put(CompensatedInventory.class, player.inventory)
                .put(PacketEntityReplication.class, player.packetEntityReplication)
                .put(CompensatedCameraEntity.class, player.cameraEntity)
                .put(AttackCooldownHandler.class, player.attackCooldown)
                .put(PacketChangeGameState.class, new PacketChangeGameState(player))
                .put(PacketPlayerAbilities.class, new PacketPlayerAbilities(player))
                .put(PacketWorldBorder.class, new PacketWorldBorder(player))
                .put(TeamHandler.class, new TeamHandler(player))
                .put(ClientBrand.class, new ClientBrand(player))

                // aim
                .put(AimModulo360.class, new AimModulo360(player))
                .put(AimDuplicateLook.class, new AimDuplicateLook(player))

                // badpackets
                .put(BadPacketsA.class, new BadPacketsA(player))
                .put(BadPacketsB.class, new BadPacketsB(player))
                .put(BadPacketsC.class, new BadPacketsC(player))
                .put(BadPacketsD.class, new BadPacketsD(player))
                .put(BadPacketsE.class, new BadPacketsE(player))
                .put(BadPacketsF.class, new BadPacketsF(player))
                .put(BadPacketsG.class, new BadPacketsG(player))
                .put(BadPacketsH.class, new BadPacketsH(player))
                .put(BadPacketsI.class, new BadPacketsI(player))
                .put(BadPacketsJ.class, new BadPacketsJ(player))
                .put(BadPacketsK.class, new BadPacketsK(player))
                .put(BadPacketsL.class, new BadPacketsL(player))
                .put(BadPacketsM.class, new BadPacketsM(player))
                .put(BadPacketsN.class, new BadPacketsN(player))
                .put(BadPacketsO.class, new BadPacketsO(player))
                .put(BadPacketsP.class, new BadPacketsP(player))
                .put(BadPacketsQ.class, new BadPacketsQ(player))
                .put(BadPacketsR.class, new BadPacketsR(player))
                .put(BadPacketsS.class, new BadPacketsS(player))
                .put(BadPacketsT.class, new BadPacketsT(player))
                .put(BadPacketsU.class, new BadPacketsU(player))
                .put(BadPacketsV.class, new BadPacketsV(player))
                .put(BadPacketsX.class, new BadPacketsX(player))
                .put(BadPacketsY.class, new BadPacketsY(player))
                .put(BadPacketsZ.class, new BadPacketsZ(player))

                // break
                .put(AirLiquidBreak.class, new AirLiquidBreak(player))
                .put(FarBreak.class, new FarBreak(player))
                .put(FastBreak.class, new FastBreak(player))
                .put(InvalidBreak.class, new InvalidBreak(player))
                .put(MultiBreak.class, new MultiBreak(player))
                .put(NoSwingBreak.class, new NoSwingBreak(player))
                .put(PositionBreakA.class, new PositionBreakA(player))
                .put(PositionBreakB.class, new PositionBreakB(player))
                .put(RotationBreak.class, new RotationBreak(player))
                .put(WrongBreak.class, new WrongBreak(player))

                // chat
                .put(ChatA.class, new ChatA(player))
                .put(ChatB.class, new ChatB(player))
                .put(ChatC.class, new ChatC(player))
                .put(ChatD.class, new ChatD(player))

                // combat
                .put(Hitboxes.class, new Hitboxes(player)) // invoked by Reach
                .put(InvalidInteractCursor.class, new InvalidInteractCursor(player))
                .put(InvalidInteractTarget.class, new InvalidInteractTarget(player))
                .put(MultiInteractA.class, new MultiInteractA(player))
                .put(MultiInteractB.class, new MultiInteractB(player))
                .put(Reach.class, new Reach(player))
                .put(SelfInteract.class, new SelfInteract(player))

                // crash
                .put(CrashA.class, new CrashA(player))
                .put(CrashB.class, new CrashB(player))
                .put(CrashC.class, new CrashC(player))
                .put(CrashD.class, new CrashD(player))
                .put(CrashE.class, new CrashE(player))
                .put(CrashF.class, new CrashF(player))
                .put(CrashG.class, new CrashG(player))
                .put(CrashH.class, new CrashH(player))
                .put(CrashI.class, new CrashI(player))

                // elytra
                .put(ElytraA.class, new ElytraA(player))
                .put(ElytraB.class, new ElytraB(player))
                .put(ElytraC.class, new ElytraC(player))
                .put(ElytraD.class, new ElytraD(player))
                .put(ElytraE.class, new ElytraE(player))
                .put(ElytraF.class, new ElytraF(player))
                .put(ElytraG.class, new ElytraG(player))
                .put(ElytraH.class, new ElytraH(player))
                .put(ElytraI.class, new ElytraI(player))
                .put(ExploitA.class, new ExploitA(player))
                .put(ExploitB.class, new ExploitB(player))

                // movement
                .put(ExplosionHandler.class, new ExplosionHandler(player))
                .put(KnockbackHandler.class, new KnockbackHandler(player))
                .put(Phase.class, new Phase(player))
                .put(GroundSpoof.class, new GroundSpoof(player))
                .put(NoFall.class, new NoFall(player))
                .put(OffsetHandler.class, new OffsetHandler(player))
                .put(NoSlow.class, new NoSlow(player))

                // multiactions
                .put(MultiActionsA.class, new MultiActionsA(player))
                .put(MultiActionsB.class, new MultiActionsB(player))
                .put(MultiActionsC.class, new MultiActionsC(player))
                .put(MultiActionsD.class, new MultiActionsD(player))
                .put(MultiActionsE.class, new MultiActionsE(player))
                .put(MultiActionsF.class, new MultiActionsF(player))
                .put(MultiActionsG.class, new MultiActionsG(player))

                // packetorder
                .put(Post.class, new Post(player))
                .put(PacketOrderA.class, new PacketOrderA(player))
                .put(PacketOrderB.class, new PacketOrderB(player))
                .put(PacketOrderC.class, new PacketOrderC(player))
                .put(PacketOrderD.class, new PacketOrderD(player))
                .put(PacketOrderE.class, new PacketOrderE(player))
                .put(PacketOrderF.class, new PacketOrderF(player))
                .put(PacketOrderG.class, new PacketOrderG(player))
                .put(PacketOrderH.class, new PacketOrderH(player))
                .put(PacketOrderI.class, new PacketOrderI(player))
                .put(PacketOrderJ.class, new PacketOrderJ(player))
                .put(PacketOrderK.class, new PacketOrderK(player))
                .put(PacketOrderL.class, new PacketOrderL(player))
                .put(PacketOrderM.class, new PacketOrderM(player))
                .put(PacketOrderN.class, new PacketOrderN(player))
                .put(PacketOrderO.class, new PacketOrderO(player))
                .put(PacketOrderP.class, new PacketOrderP(player))
                .put(TransactionOrder.class, new TransactionOrder(player))

                // place
                .put(AirLiquidPlace.class, new AirLiquidPlace(player))
                .put(DuplicateRotPlace.class, new DuplicateRotPlace(player))
                .put(FabricatedPlace.class, new FabricatedPlace(player))
                .put(FarPlace.class, new FarPlace(player))
                .put(InvalidPlaceCursor.class, new InvalidPlaceCursor(player))
                .put(InvalidPlaceFace.class, new InvalidPlaceFace(player))
                .put(MultiPlace.class, new MultiPlace(player))
                .put(PositionPlace.class, new PositionPlace(player))
                .put(RotationPlace.class, new RotationPlace(player))

                // sprint
                .put(SprintA.class, new SprintA(player))
                .put(SprintB.class, new SprintB(player))
                .put(SprintC.class, new SprintC(player))
                .put(SprintD.class, new SprintD(player))
                .put(SprintE.class, new SprintE(player))
                .put(SprintF.class, new SprintF(player))
                .put(SprintG.class, new SprintG(player))

                // timer
                .put(NegativeTimer.class, new NegativeTimer(player))
                .put(TickTimer.class, new TickTimer(player))
                .put(Timer.class, new Timer(player))
                .put(TimerLimit.class, new TimerLimit(player))
                .put(VehicleTimer.class, new VehicleTimer(player))

                // vehicle
                .put(VehicleA.class, new VehicleA(player))
                .put(VehicleB.class, new VehicleB(player))
                .put(VehicleC.class, new VehicleC(player))
                .put(VehicleD.class, new VehicleD(player))
                .put(VehicleE.class, new VehicleE(player))
                .put(VehicleF.class, new VehicleF(player))

                // debug
                .put(SuperDebug.class, new SuperDebug(player))
                .put(DebugHandler.class, new DebugHandler(player))

                // processors intended to run last
                .put(SetbackTeleportUtil.class, new SetbackTeleportUtil(player)) // Avoid teleporting to new position, update safe pos last
                .put(CompensatedFireworks.class, player.fireworks)
                .put(SneakingEstimator.class, new SneakingEstimator(player))
                .put(LastInstanceManager.class, player.lastInstanceManager)
                .put(SetbackBlocker.class, new SetbackBlocker(player)) // Must be last class otherwise we can't check while blocking packets
                .build();

        ArrayList<AbstractCheck> checks = new ArrayList<>();
        ArrayList<PreViaPacketReceiveListener> preViaPacketReceiveListeners = new ArrayList<>();
        ArrayList<PreViaPacketSendListener> preViaPacketSendListeners = new ArrayList<>();
        ArrayList<PacketReceiveListener> packetReceiveListeners = new ArrayList<>();
        ArrayList<PacketSendListener> packetSendListeners = new ArrayList<>();
        ArrayList<PrePredictionPacketReceiveListener> prePredictionPacketReceiveListeners = new ArrayList<>();
        ArrayList<PositionListener> positionListeners = new ArrayList<>();
        ArrayList<RotationListener> rotationListeners = new ArrayList<>();
        ArrayList<VehicleListener> vehicleListeners = new ArrayList<>();
        ArrayList<PostPredictionListener> postPredictionListeners = new ArrayList<>();
        ArrayList<BlockPlaceListener> blockPlaceListeners = new ArrayList<>();
        ArrayList<PostFlyingBlockPlaceListener> postFlyingBlockPlaceListeners = new ArrayList<>();
        ArrayList<BlockBreakListener> blockBreakListeners = new ArrayList<>();
        ArrayList<PostFlyingBlockBreakListener> postFlyingBlockBreakListeners = new ArrayList<>();

        for (AbstractProcessor processor : processors.values()) {
            if (processor instanceof AbstractCheck abstractCheck) {
                checks.add(abstractCheck);
                if (processor instanceof Check grimCheck && !grimCheck.isApplicable()) continue;
            }

            if (processor instanceof PacketReceiveListener packetReceiveListener) packetReceiveListeners.add(packetReceiveListener);
            if (processor instanceof PrePredictionPacketReceiveListener prePredictionPacketReceiveListener) prePredictionPacketReceiveListeners.add(prePredictionPacketReceiveListener);
            if (processor instanceof PreViaPacketReceiveListener preViaPacketReceiveListener) preViaPacketReceiveListeners.add(preViaPacketReceiveListener);
            if (processor instanceof PacketSendListener packetSendListener) packetSendListeners.add(packetSendListener);
            if (processor instanceof PreViaPacketSendListener preViaPacketSendListener) preViaPacketSendListeners.add(preViaPacketSendListener);
            if (processor instanceof PositionListener positionListener) positionListeners.add(positionListener);
            if (processor instanceof RotationListener rotationListener) rotationListeners.add(rotationListener);
            if (processor instanceof VehicleListener vehicleListener) vehicleListeners.add(vehicleListener);
            if (processor instanceof PostPredictionListener postPredictionListener) postPredictionListeners.add(postPredictionListener);
            if (processor instanceof BlockPlaceListener blockPlaceListener) blockPlaceListeners.add(blockPlaceListener);
            if (processor instanceof PostFlyingBlockPlaceListener postFlyingBlockPlaceListener) postFlyingBlockPlaceListeners.add(postFlyingBlockPlaceListener);
            if (processor instanceof BlockBreakListener blockBreakListener) blockBreakListeners.add(blockBreakListener);
            if (processor instanceof PostFlyingBlockBreakListener postFlyingBlockBreakListener) postFlyingBlockBreakListeners.add(postFlyingBlockBreakListener);
        }

        checks.trimToSize();
        this.checks = Collections.unmodifiableCollection(checks);
        this.preViaPacketReceiveListeners = preViaPacketReceiveListeners.toArray(new PreViaPacketReceiveListener[preViaPacketReceiveListeners.size()]);
        this.preViaPacketSendListeners = preViaPacketSendListeners.toArray(new PreViaPacketSendListener[preViaPacketSendListeners.size()]);
        this.packetReceiveListeners = packetReceiveListeners.toArray(new PacketReceiveListener[packetReceiveListeners.size()]);
        this.packetSendListeners = packetSendListeners.toArray(new PacketSendListener[packetSendListeners.size()]);
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
            for (AbstractCheck check : checks) {
                if (check instanceof Check grimCheck) {
                    grimCheck.registerVerboseTemplates(registry);
                }
            }
        });
    }

    @Override
    public void reload() {
        for (AbstractProcessor processor : processors.values()) {
            processor.reload();
        }
    }

    public <T extends AbstractProcessor> T get(@NotNull Class<T> check) {
        return processors.getInstance(check);
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
        for (PacketSendListener check : packetSendListeners) {
            check.onPacketSend(packet);
        }
    }

    public void onPreViaPacketSend(final PacketSendEvent packet) {
        for (PreViaPacketSendListener check : preViaPacketSendListeners) {
            check.onPreViaPacketSend(packet);
        }
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
        return get(ExplosionHandler.class);
    }

    public NoFall getNoFall() {
        return get(NoFall.class);
    }

    public KnockbackHandler getKnockbackHandler() {
        return get(KnockbackHandler.class);
    }

    public CompensatedCooldown getCompensatedCooldown() {
        return get(CompensatedCooldown.class);
    }

    public NoSlow getNoSlow() {
        return get(NoSlow.class);
    }

    public SetbackTeleportUtil getSetbackUtil() {
        return get(SetbackTeleportUtil.class);
    }

    public DebugHandler getDebugHandler() {
        return get(DebugHandler.class);
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

        for (final AbstractCheck check : checks) {
            if (check.getConfigName() == null) continue;
            final String id = check.getConfigName().toLowerCase();
            for (String permissionName : permissions) {
                GrimAPI.INSTANCE.getPermissionManager().registerPermission(permissionName + id, PermissionDefaultValue.FALSE);
            }
        }
    }
}
