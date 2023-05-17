package ac.grim.grimac.manager;

import ac.grim.grimac.AbstractCheck;
import ac.grim.grimac.checks.CachedCheck;
import ac.grim.grimac.checks.CheckCategory;
import ac.grim.grimac.checks.impl.aim.AimDuplicateLook;
import ac.grim.grimac.checks.impl.aim.AimModulo360;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.badpackets.*;
import ac.grim.grimac.checks.impl.baritone.Baritone;
import ac.grim.grimac.checks.impl.combat.Reach;
import ac.grim.grimac.checks.impl.crash.*;
import ac.grim.grimac.checks.impl.exploit.ExploitA;
import ac.grim.grimac.checks.impl.exploit.ExploitB;
import ac.grim.grimac.checks.impl.groundspoof.NoFallA;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.checks.impl.misc.FastBreak;
import ac.grim.grimac.checks.impl.movement.*;
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
import ac.grim.grimac.manager.init.start.SuperDebug;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.predictionengine.SneakingEstimator;
import ac.grim.grimac.utils.anticheat.update.*;
import ac.grim.grimac.utils.latency.CompensatedCooldown;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import ac.grim.grimac.utils.latency.CompensatedInventory;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.cache.Cache;
import com.google.common.collect.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;


@Getter
public class CheckManager {

    private static final Map<Class<? extends AbstractCheck>,
            CachedCheck<PacketCheck>> packetChecksCached;

    private static final Map<Class<? extends AbstractCheck>,
            CachedCheck<PositionCheck>> positionCheckCached;

    private static final Map<Class<? extends AbstractCheck>,
            CachedCheck<RotationCheck>> rotationCheckCached;


    private static final Map<Class<? extends AbstractCheck>,
            CachedCheck<VehicleCheck>> vehicleCheckCached;

    private static final Map<Class<? extends AbstractCheck>,
            CachedCheck<PacketCheck>> prePredictionChecksCached;

    private static final Map<Class<? extends AbstractCheck>,
            CachedCheck<BlockPlaceCheck>> blockPlaceCheckCached;

    private static final Map<Class<? extends AbstractCheck>,
            CachedCheck<PostPredictionCheck>> postPredictionCheckCached;

    static {
        try {

            packetChecksCached = ImmutableMap.<Class<? extends AbstractCheck>,
                            CachedCheck<PacketCheck>>builder()
                    .put(Reach.class, new CachedCheck<>(Reach.class))
                    .put(PacketEntityReplication.class, new CachedCheck<>(PacketEntityReplication.class, true))
                    .put(PacketChangeGameState.class, new CachedCheck<>(PacketChangeGameState.class, true))
                    .put(CompensatedInventory.class, new CachedCheck<>(CompensatedInventory.class, true))
                    .put(PacketPlayerAbilities.class, new CachedCheck<>(PacketPlayerAbilities.class, true))
                    .put(PacketWorldBorder.class, new CachedCheck<>(PacketWorldBorder.class, true))
                    .put(ClientBrand.class, new CachedCheck<>(ClientBrand.class, true))
                    .put(NoFallA.class, new CachedCheck<>(NoFallA.class))
                    .put(BadPacketsO.class, new CachedCheck<>(BadPacketsO.class))
                    .put(BadPacketsA.class, new CachedCheck<>(BadPacketsA.class))
                    .put(BadPacketsB.class, new CachedCheck<>(BadPacketsB.class))
                    .put(BadPacketsC.class, new CachedCheck<>(BadPacketsC.class))
                    .put(BadPacketsD.class, new CachedCheck<>(BadPacketsD.class))
                    .put(BadPacketsE.class, new CachedCheck<>(BadPacketsE.class))
                    .put(BadPacketsF.class, new CachedCheck<>(BadPacketsF.class))
                    .put(BadPacketsG.class, new CachedCheck<>(BadPacketsG.class))
                    .put(BadPacketsH.class, new CachedCheck<>(BadPacketsH.class))
                    .put(BadPacketsI.class, new CachedCheck<>(BadPacketsI.class))
                    .put(BadPacketsJ.class, new CachedCheck<>(BadPacketsJ.class))
                    .put(BadPacketsK.class, new CachedCheck<>(BadPacketsK.class))
                    .put(BadPacketsL.class, new CachedCheck<>(BadPacketsL.class))
                    .put(BadPacketsN.class, new CachedCheck<>(BadPacketsN.class))
                    .put(BadPacketsP.class, new CachedCheck<>(BadPacketsP.class))
                    .put(BadPacketsQ.class, new CachedCheck<>(BadPacketsQ.class))
                    .put(PostCheck.class, new CachedCheck<>(PostCheck.class, true))
                    .put(FastBreak.class, new CachedCheck<>(FastBreak.class))
                    .put(NoSlowB.class, new CachedCheck<>(NoSlowB.class))
                    .put(SetbackBlocker.class, new CachedCheck<>(SetbackBlocker.class)) // Must be last class otherwise we can't check while blocking packets
                    .build();

            positionCheckCached = ImmutableMap.<Class<? extends AbstractCheck>,
                            CachedCheck<PositionCheck>>builder()
                    .put(PredictionRunner.class, new CachedCheck<>(PredictionRunner.class, true))
                    .put(CompensatedCooldown.class, new CachedCheck<>(CompensatedCooldown.class, true))
                    .build();

            rotationCheckCached = ImmutableMap.<Class<? extends AbstractCheck>,
                            CachedCheck<RotationCheck>>builder()
                    .put(AimProcessor.class, new CachedCheck<>(AimProcessor.class, true))
                    .put(AimModulo360.class, new CachedCheck<>(AimModulo360.class))
                    .put(AimDuplicateLook.class, new CachedCheck<>(AimDuplicateLook.class))
                    .put(Baritone.class, new CachedCheck<>(Baritone.class))
                    .build();

            vehicleCheckCached = ImmutableMap.<Class<? extends AbstractCheck>,
                            CachedCheck<VehicleCheck>>builder()
                    .put(VehiclePredictionRunner.class, new CachedCheck<>(VehiclePredictionRunner.class, true))
                    .build();

            postPredictionCheckCached = ImmutableMap.<Class<? extends AbstractCheck>,
                            CachedCheck<PostPredictionCheck>>builder()
                    .put(NegativeTimerCheck.class, new CachedCheck<>(NegativeTimerCheck.class))
                    .put(ExplosionHandler.class, new CachedCheck<>(ExplosionHandler.class, true))
                    .put(KnockbackHandler.class, new CachedCheck<>(KnockbackHandler.class, true))
                    .put(GhostBlockDetector.class, new CachedCheck<>(GhostBlockDetector.class, true))
                    .put(Phase.class, new CachedCheck<>(Phase.class))
                    .put(NoFallB.class, new CachedCheck<>(NoFallB.class))
                    .put(OffsetHandler.class, new CachedCheck<>(OffsetHandler.class, true))
                    .put(SuperDebug.class, new CachedCheck<>(SuperDebug.class))
                    .put(DebugHandler.class, new CachedCheck<>(DebugHandler.class, true))
                    .put(EntityControl.class, new CachedCheck<>(EntityControl.class, true))
                    .put(NoSlowA.class, new CachedCheck<>(NoSlowA.class))
                    .put(SetbackTeleportUtil.class, new CachedCheck<>(SetbackTeleportUtil.class, true)) // Avoid teleporting to new position, update safe pos last
                    .put(CompensatedFireworks.class, new CachedCheck<>(CompensatedFireworks.class, true))
                    .put(SneakingEstimator.class, new CachedCheck<>(SneakingEstimator.class, true))
                    .put(LastInstanceManager.class, new CachedCheck<>(LastInstanceManager.class, true))
                    .build();

            blockPlaceCheckCached = ImmutableMap.<Class<? extends AbstractCheck>,
                            CachedCheck<BlockPlaceCheck>>builder()
                    .put(AirLiquidPlace.class, new CachedCheck<>(AirLiquidPlace.class))
                    .put(FarPlace.class, new CachedCheck<>(FarPlace.class))
                    .put(FabricatedPlace.class, new CachedCheck<>(FabricatedPlace.class))
                    .put(PositionPlace.class, new CachedCheck<>(PositionPlace.class))
                    .put(RotationPlace.class, new CachedCheck<>(RotationPlace.class))
                    .put(DuplicateRotPlace.class, new CachedCheck<>(DuplicateRotPlace.class))
                    .build();

            prePredictionChecksCached = ImmutableMap.<Class<? extends AbstractCheck>,
                            CachedCheck<PacketCheck>>builder()
                    .put(TimerCheck.class, new CachedCheck<>(TimerCheck.class))
                    .put(CrashA.class, new CachedCheck<>(CrashA.class))
                    .put(CrashB.class, new CachedCheck<>(CrashB.class))
                    .put(CrashC.class, new CachedCheck<>(CrashC.class))
                    .put(CrashD.class, new CachedCheck<>(CrashD.class))
                    .put(CrashE.class, new CachedCheck<>(CrashE.class))
                    .put(ExploitA.class, new CachedCheck<>(ExploitA.class))
                    .put(ExploitB.class, new CachedCheck<>(ExploitB.class))
                    .put(VehicleTimer.class, new CachedCheck<>(VehicleTimer.class))
                    .build();

            packetChecksCached.forEach(((aClass, packetCheckCachedCheck)
                    -> packetCheckCachedCheck.checkCategory(CheckCategory.PACKET)));

            positionCheckCached.forEach(((aClass, packetCheckCachedCheck)
                    -> packetCheckCachedCheck.checkCategory(CheckCategory.POSITION)));

            rotationCheckCached.forEach(((aClass, packetCheckCachedCheck)
                    -> packetCheckCachedCheck.checkCategory(CheckCategory.ROTATION)));

            postPredictionCheckCached.forEach(((aClass, packetCheckCachedCheck)
                    -> packetCheckCachedCheck.checkCategory(CheckCategory.POST_PREDICTION)));

            vehicleCheckCached.forEach(((aClass, packetCheckCachedCheck)
                    -> packetCheckCachedCheck.checkCategory(CheckCategory.VEHICLE)));

            blockPlaceCheckCached.forEach(((aClass, packetCheckCachedCheck)
                    -> packetCheckCachedCheck.checkCategory(CheckCategory.BLOCK)));

            prePredictionChecksCached.forEach(((aClass, packetCheckCachedCheck)
                    -> packetCheckCachedCheck.checkCategory(CheckCategory.PRE_PREDICTION)));

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    public static final Map<Class<? extends AbstractCheck>,
            CachedCheck<?>> cachedAllChecks = ImmutableMap.<Class<? extends AbstractCheck>,
                    CachedCheck<? extends AbstractCheck>>builder()
            .putAll(packetChecksCached)
            .putAll(positionCheckCached)
            .putAll(rotationCheckCached)
            .putAll(vehicleCheckCached)
            .putAll(blockPlaceCheckCached)
            .putAll(prePredictionChecksCached)
            .putAll(postPredictionCheckCached)
            .build();

    private final Map<Class<? extends AbstractCheck>, PacketCheck> packetChecks;
    private final Map<Class<? extends AbstractCheck>, PositionCheck> positionCheck;
    private final Map<Class<? extends AbstractCheck>, RotationCheck> rotationCheck;
    private final Map<Class<? extends AbstractCheck>, VehicleCheck> vehicleCheck;
    private final Map<Class<? extends AbstractCheck>, PacketCheck> prePredictionChecks;
    private final Map<Class<? extends AbstractCheck>, BlockPlaceCheck> blockPlaceCheck;
    private final Map<Class<? extends AbstractCheck>, PostPredictionCheck> postPredictionCheck;
    public final Map<Class<? extends AbstractCheck>, AbstractCheck> allChecks;

    @Getter
    private final GrimPlayer player;

    public CheckManager(GrimPlayer player) {
        // Include post checks in the packet check too
        packetChecks = new HashMap<>(packetChecksCached.size());
        rotationCheck = new HashMap<>(rotationCheckCached.size());
        vehicleCheck = new HashMap<>(vehicleCheckCached.size());
        postPredictionCheck = new HashMap<>(postPredictionCheckCached.size());
        blockPlaceCheck = new HashMap<>(blockPlaceCheckCached.size());
        prePredictionChecks = new HashMap<>(prePredictionChecksCached.size());
        positionCheck = new HashMap<>(positionCheckCached.size());
        allChecks = new HashMap<>();
        this.player = player;
    }

    public void initAllChecks() {
        cachedAllChecks.forEach(((aClass, cachedCheck) -> {
            if (cachedCheck.isImportant() ||
                    !cachedCheck.isDisabled() && !getPlayer().hasPermission(cachedCheck.getPermission())) {
                AbstractCheck abstractCheck = cachedCheck.apply(getPlayer());
                initCheck(aClass, cachedCheck, abstractCheck);
            }
        }));
    }

    public void reload() {
        Iterator<Map.Entry<Class<? extends AbstractCheck>, AbstractCheck>> abstractCheckIterator
                = getPlayer().getCheckManager().allChecks.entrySet().iterator();
        while (abstractCheckIterator.hasNext()) {
            Map.Entry<Class<? extends AbstractCheck>, AbstractCheck> entry = abstractCheckIterator.next();
            AbstractCheck value = entry.getValue();
            Class<? extends AbstractCheck> key = entry.getKey();
            if (cachedAllChecks.get(key).isDisabled()) {
                if (removeCheckUnchecked(key)) abstractCheckIterator.remove();
            } else {
                value.reload();
            }
        }
        CheckManager.cachedAllChecks.forEach((aClass, cachedCheck) -> {
            if (!getPlayer().getCheckManager().allChecks.containsKey(aClass) &&
                    !cachedCheck.isDisabled()) {
                addCheck(aClass);
            }
        });
    }

    private <T extends AbstractCheck> void initCheck(Class<? extends T> checkClass,
                                                     CachedCheck<? extends T> cachedCheck,
                                                     T abstractCheck) {
        abstractCheck.setEnabled(true);
        CheckCategory<T> checkCategory = (CheckCategory<T>) cachedCheck.getCheckCategory();
        checkCategory.getFunction().apply(getPlayer()).put(checkClass, abstractCheck);
        allChecks.put(checkClass, abstractCheck);
    }

    public boolean isEnabledCheck(Class<? extends AbstractCheck> aClass) {
        return allChecks.containsKey(aClass);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractCheck> void addCheck(Class<T> checkClass) {
        if (getAllChecks().containsKey(checkClass)) return;
        CachedCheck<T> cachedCheck = (CachedCheck<T>) cachedAllChecks.get(checkClass);
        if (!cachedCheck.isImportant() && getPlayer().hasPermission(cachedCheck.getPermission())) return;
        T abstractCheck = cachedCheck.apply(getPlayer());
        getAllChecks().put(checkClass, abstractCheck);
        abstractCheck.setEnabled(true);
        CheckCategory<T> checkCategory = cachedCheck.getCheckCategory();
        checkCategory.getFunction().apply(getPlayer()).put(checkClass, abstractCheck);
    }

    public void removeCheck(Class<? extends AbstractCheck> aClass) {
        removeCheckUnchecked(aClass);
        allChecks.remove(aClass);
    }

    private boolean removeCheckUnchecked(Class<? extends AbstractCheck> aClass) {
        CachedCheck<?> cachedCheck = cachedAllChecks.get(aClass);
        AbstractCheck value = allChecks.get(aClass);
        value.setEnabled(false);
        if (cachedCheck.isImportant()) return false;
        CheckCategory<?> checkCategory = cachedCheck.getCheckCategory();
        checkCategory.getFunction().apply(getPlayer()).remove(aClass);
        return true;
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

    public ExplosionHandler getExplosionHandler() {
        return getPostPredictionCheck(ExplosionHandler.class);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> Optional<T> getPacketCheck(Class<T> check) {
        return Optional.ofNullable((T) packetChecks.get(check));
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPacketCheckSafe(Class<T> check) {
        return (T) packetChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPrePredictionCheck(Class<T> check) {
        return (T) prePredictionChecks.get(check);
    }

    public PacketEntityReplication getEntityReplication() {
        return getPacketCheckSafe(PacketEntityReplication.class);
    }

    public NoFallA getNoFall() {
        return getPacketCheckSafe(NoFallA.class);
    }

    public KnockbackHandler getKnockbackHandler() {
        return getPostPredictionCheck(KnockbackHandler.class);
    }

    public CompensatedCooldown getCompensatedCooldown() {
        return getPositionCheck(CompensatedCooldown.class);
    }

    public NoSlowA getNoSlow() {
        return getPostPredictionCheck(NoSlowA.class);
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
