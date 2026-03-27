package ac.grim.legacyac.check;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.impl.FlyCheck;
import ac.grim.legacyac.check.impl.GroundSpoofCheck;
import ac.grim.legacyac.check.impl.InventoryMoveCheck;
import ac.grim.legacyac.check.impl.JesusCheck;
import ac.grim.legacyac.check.impl.NoSlowCheck;
import ac.grim.legacyac.check.impl.PhaseCheck;
import ac.grim.legacyac.check.impl.PredictionMovementCheck;
import ac.grim.legacyac.check.impl.SpeedCheck;
import ac.grim.legacyac.check.impl.TimerCheck;
import ac.grim.legacyac.check.impl.VelocityCheck;
import ac.grim.legacyac.check.impl.aim.AimDuplicateLookCheck;
import ac.grim.legacyac.check.impl.aim.AimModulo360Check;
import ac.grim.legacyac.check.impl.aim.AimProcessorCheck;
import ac.grim.legacyac.check.impl.breaking.AirLiquidBreakCheck;
import ac.grim.legacyac.check.impl.breaking.FarBreakCheck;
import ac.grim.legacyac.check.impl.breaking.MultiBreakCheck;
import ac.grim.legacyac.check.impl.breaking.RotationBreakCheck;
import ac.grim.legacyac.check.impl.scaffold.AirLiquidPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.DuplicateRotPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.FabricatedPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.FarPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.MultiPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.PositionPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.RotationPlaceCheck;
import ac.grim.legacyac.data.FrameContextSnapshot;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.data.state.CompensationState;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

final class MovementPipeline {
    private final LegacyAntiCheatPlugin plugin;
    private final ToleranceBudgetEngine.ConfigProvider budgetConfigProvider;
    private final DoubleSupplier currentTpsSupplier;
    private final CombatPipeline combatPipeline;
    private final List<TimerCheck> timerChecks;
    private final List<InventoryMoveCheck> inventoryMoveChecks;
    private final List<AirLiquidPlaceCheck> airLiquidPlaceChecks;
    private final List<FarPlaceCheck> farPlaceChecks;
    private final List<RotationPlaceCheck> rotationPlaceChecks;
    private final List<MultiPlaceCheck> multiPlaceChecks;
    private final List<PositionPlaceCheck> positionPlaceChecks;
    private final List<DuplicateRotPlaceCheck> duplicateRotPlaceChecks;
    private final List<FabricatedPlaceCheck> fabricatedPlaceChecks;
    private final List<AirLiquidBreakCheck> airLiquidBreakChecks;
    private final List<FarBreakCheck> farBreakChecks;
    private final List<RotationBreakCheck> rotationBreakChecks;
    private final List<MultiBreakCheck> multiBreakChecks;
    private final List<PredictionMovementCheck> predictionChecks;
    private final List<AimProcessorCheck> aimProcessorChecks;
    private final List<AimModulo360Check> aimModulo360Checks;
    private final List<AimDuplicateLookCheck> aimDuplicateLookChecks;
    private final List<NoSlowCheck> noSlowChecks;
    private final List<SpeedCheck> speedChecks;
    private final List<FlyCheck> flyChecks;
    private final List<PhaseCheck> phaseChecks;
    private final List<JesusCheck> jesusChecks;
    private final List<VelocityCheck> velocityChecks;
    private final List<GroundSpoofCheck> groundSpoofChecks;
    private long legacyFallbackHitCount;
    private long minimalPostPredictionMissHitCount;

    MovementPipeline(LegacyAntiCheatPlugin plugin,
            ToleranceBudgetEngine.ConfigProvider budgetConfigProvider,
            DoubleSupplier currentTpsSupplier,
            CombatPipeline combatPipeline,
            List<TimerCheck> timerChecks,
            List<InventoryMoveCheck> inventoryMoveChecks,
            List<AirLiquidPlaceCheck> airLiquidPlaceChecks,
            List<FarPlaceCheck> farPlaceChecks,
            List<RotationPlaceCheck> rotationPlaceChecks,
            List<MultiPlaceCheck> multiPlaceChecks,
            List<PositionPlaceCheck> positionPlaceChecks,
            List<DuplicateRotPlaceCheck> duplicateRotPlaceChecks,
            List<FabricatedPlaceCheck> fabricatedPlaceChecks,
            List<AirLiquidBreakCheck> airLiquidBreakChecks,
            List<FarBreakCheck> farBreakChecks,
            List<RotationBreakCheck> rotationBreakChecks,
            List<MultiBreakCheck> multiBreakChecks,
            List<PredictionMovementCheck> predictionChecks,
            List<AimProcessorCheck> aimProcessorChecks,
            List<AimModulo360Check> aimModulo360Checks,
            List<AimDuplicateLookCheck> aimDuplicateLookChecks,
            List<NoSlowCheck> noSlowChecks,
            List<SpeedCheck> speedChecks,
            List<FlyCheck> flyChecks,
            List<PhaseCheck> phaseChecks,
            List<JesusCheck> jesusChecks,
            List<VelocityCheck> velocityChecks,
            List<GroundSpoofCheck> groundSpoofChecks) {
        this.plugin = plugin;
        this.budgetConfigProvider = budgetConfigProvider;
        this.currentTpsSupplier = currentTpsSupplier;
        this.combatPipeline = combatPipeline;
        this.timerChecks = timerChecks;
        this.inventoryMoveChecks = inventoryMoveChecks;
        this.airLiquidPlaceChecks = airLiquidPlaceChecks;
        this.farPlaceChecks = farPlaceChecks;
        this.rotationPlaceChecks = rotationPlaceChecks;
        this.multiPlaceChecks = multiPlaceChecks;
        this.positionPlaceChecks = positionPlaceChecks;
        this.duplicateRotPlaceChecks = duplicateRotPlaceChecks;
        this.fabricatedPlaceChecks = fabricatedPlaceChecks;
        this.airLiquidBreakChecks = airLiquidBreakChecks;
        this.farBreakChecks = farBreakChecks;
        this.rotationBreakChecks = rotationBreakChecks;
        this.multiBreakChecks = multiBreakChecks;
        this.predictionChecks = predictionChecks;
        this.aimProcessorChecks = aimProcessorChecks;
        this.aimModulo360Checks = aimModulo360Checks;
        this.aimDuplicateLookChecks = aimDuplicateLookChecks;
        this.noSlowChecks = noSlowChecks;
        this.speedChecks = speedChecks;
        this.flyChecks = flyChecks;
        this.phaseChecks = phaseChecks;
        this.jesusChecks = jesusChecks;
        this.velocityChecks = velocityChecks;
        this.groundSpoofChecks = groundSpoofChecks;
    }

    void consumeMovementFrame(Player player, MovementFrame frame, Location explicitFrom, Location explicitTo) {
        PlayerData data = plugin.getPlayerData(player);
        data.touchMovementFrame(frame.getTimestampNanos());

        if (!frame.hasPosition()) {
            runTimingChecks(player, frame, data);
            return;
        }

        Location from = explicitFrom;
        if (from == null) {
            if (data.isMovementFrameInitialized()) {
                from = new Location(player.getWorld(), data.getLastFrameX(), data.getLastFrameY(), data.getLastFrameZ(),
                        data.getLastFrameYaw(), data.getLastFramePitch());
            } else {
                from = player.getLocation().clone();
            }
        }

        Location to = explicitTo;
        if (to == null) {
            to = new Location(player.getWorld(), frame.getX(), frame.getY(), frame.getZ(), frame.getYaw(),
                    frame.getPitch());
        }

        data.setMovementFrame(frame.getX(), frame.getY(), frame.getZ(), frame.getYaw(), frame.getPitch(),
                frame.getTimestampNanos());
        executeMovementPipeline(player, data, frame, from, to);
    }

    private void executeMovementPipeline(Player player, PlayerData data, MovementFrame frame, Location from,
            Location to) {
        long pipelineStart = System.nanoTime();
        PipelineTrace trace = data.isDebugEnabled() ? new PipelineTrace(pipelineStart, player.getName()) : null;

        data.handleMove(player, from, to, frame.isOnGround());
        data.preloadCompensatedWorld(player, 1);
        data.setDetectionContext(frame.getSource().name(), data.getMoveWindow());

        ToleranceBudgetEngine.BudgetSnapshot budget = ToleranceBudgetEngine.compute(
                data.network(), data.compensation(), data.environment(), currentTpsSupplier.getAsDouble(),
                budgetConfigProvider);
        data.setCurrentBudget(budget);

        CompensationState.MovementStateSnapshot snapshot = data.compensation().getMovementStateSnapshot();
        PlayerData.VelocitySample velocitySample = data.getCurrentVelocitySample();
        long actionWindowId = data.network().packetOrder().getCurrentActionWindowId();
        if (actionWindowId <= 0L) {
            actionWindowId = data.getMoveWindow();
        }
        FrameContextSnapshot frameContext = new FrameContextSnapshot(
                frame.getTimestampNanos(),
                data.getMoveWindow(),
                new FrameContextSnapshot.PredictionOutputSnapshot(false, 0.0D, 0.0D, 0.0D, 0.0D, "none"),
                new FrameContextSnapshot.TxWindowStateSnapshot(
                        velocitySample == null ? (short) 0 : velocitySample.getPreTxId(),
                        velocitySample == null ? (short) 0 : velocitySample.getPostTxId(),
                        velocitySample == null ? 0 : velocitySample.getStateFlags(),
                        velocitySample == null ? 0 : velocitySample.getTicksObserved()),
                null,
                budget,
                data.getPendingWorldChangeDebugSnapshot(),
                snapshot.getPrimaryBlocker(),
                actionWindowId,
                snapshot.isEnforceable());
        data.setCurrentFrameContext(frameContext);

        if (data.isDebugEnabled() && plugin.getConfig().getBoolean("adaptive-lag.compare-log-enabled", false)) {
            plugin.getLogger().info("[GLAC-BUDGET] " + player.getName() + " " + budget.toDebugString());
        }

        if (!snapshot.isTeleportAligned()) {
            String reason = snapshot.getPrimaryBlocker().name().toLowerCase(Locale.ROOT) + "-not-aligned";
            if (data.isDebugEnabled()) {
                plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                        + " checks SKIPPED: " + reason + " pending=" + snapshot.getPendingChanges());
            }
            if (trace != null) {
                trace.addEntry("*", CheckStage.PRE, PipelineTrace.Status.SKIPPED, 0L, reason);
            }
            runLegacyFallbackChecks(player, data, from, to, frame, reason, budget, trace);
            emitPipelineTrace(trace, pipelineStart);
            return;
        }

        runPacketStatePreprocess(player, frame, data, trace);
        runQueuedBlockInteractionChecks(player, data, trace);
        boolean predictionReady = runMovementPrediction(player, frame, to, data, trace);
        boolean oldPredictionReady = data.hasPredictionForFrame(frame.getTimestampNanos());
        if (plugin.getConfig().getBoolean("pipeline.frame-context.dual-track-log", true)
                && predictionReady != oldPredictionReady) {
            plugin.getLogger().info("[GLAC-FRAMECTX-DIFF] " + player.getName()
                    + " frame=" + frame.getTimestampNanos()
                    + " oldPredictionReady=" + oldPredictionReady
                    + " newPredictionReady=" + predictionReady);
        }
        data.updateKnockbackStages();

        if (predictionReady) {
            if (trace != null) {
                trace.addEntry("Prediction", CheckStage.PREDICTION, PipelineTrace.Status.RAN, 0L, null);
            }
            runPostPredictionChecks(player, frame, from, to, data, trace);
            runQueuedAttackChecks(player, data, trace);
        } else {
            if (trace != null) {
                trace.addEntry("Prediction", CheckStage.PREDICTION, PipelineTrace.Status.SKIPPED, 0L,
                        "prediction-unavailable");
            }
            runLegacyFallbackChecks(player, data, from, to, frame, "prediction-unavailable", budget, trace);
        }

        if (frame.isOnGround() && data.getLastDeltaXZ() < 0.35D && Math.abs(data.getLastDeltaY()) < 0.02D) {
            data.setLastSafeLocation(to.clone());
        }

        emitPipelineTrace(trace, pipelineStart);
    }

    private void emitPipelineTrace(PipelineTrace trace, long startNanos) {
        if (trace == null) {
            return;
        }
        trace.setTotalDurationNanos(System.nanoTime() - startNanos);
        plugin.getLogger().info(trace.toSummary());
    }

    private void runPacketStatePreprocess(Player player, MovementFrame frame, PlayerData data, PipelineTrace trace) {
        long stageStart = System.nanoTime();
        runTimingChecks(player, frame, data);
        for (InventoryMoveCheck check : inventoryMoveChecks) {
            check.onMovementFrame(player, frame, data);
        }
        if (trace != null) {
            trace.addEntry(CheckStage.PRE, "Timer+InventoryMove",
                    System.nanoTime() - stageStart, true, null);
        }
    }

    private void runQueuedBlockInteractionChecks(Player player, PlayerData data, PipelineTrace trace) {
        long stageStart = System.nanoTime();
        PlayerData.MovementStateSnapshot movementState = data.getMovementStateSnapshot();
        boolean readyForBlockChecks = movementState.isTeleportAligned() && movementState.isBlockAligned();
        for (PlayerData.QueuedBlockPlaceSnapshot snapshot : data.consumeQueuedBlockPlaces(data.getMoveWindow(),
                readyForBlockChecks)) {
            if (snapshot.getFace() == 255) {
                continue;
            }
            for (AirLiquidPlaceCheck check : airLiquidPlaceChecks) {
                check.onPacketPlace(player, data, snapshot);
            }
            for (FarPlaceCheck check : farPlaceChecks) {
                check.onPacketPlace(player, data, snapshot);
            }
            for (RotationPlaceCheck check : rotationPlaceChecks) {
                check.onPacketPlace(player, data, snapshot);
            }
            for (MultiPlaceCheck check : multiPlaceChecks) {
                check.onPacketPlace(player, data, snapshot);
            }
            for (PositionPlaceCheck check : positionPlaceChecks) {
                check.onPacketPlace(player, data, snapshot);
            }
            for (DuplicateRotPlaceCheck check : duplicateRotPlaceChecks) {
                check.onPacketPlace(player, data);
            }
            for (FabricatedPlaceCheck check : fabricatedPlaceChecks) {
                check.onPacketPlace(player, data, snapshot);
            }
        }

        for (PlayerData.QueuedBlockDigSnapshot snapshot : data.consumeQueuedBlockDigs(data.getMoveWindow(),
                readyForBlockChecks)) {
            if (snapshot.getDigAction() != 0 && snapshot.getDigAction() != 2) {
                continue;
            }
            for (AirLiquidBreakCheck check : airLiquidBreakChecks) {
                check.onPacketBreak(player, data, snapshot);
            }
            for (FarBreakCheck check : farBreakChecks) {
                check.onPacketBreak(player, data, snapshot);
            }
            for (RotationBreakCheck check : rotationBreakChecks) {
                check.onPacketBreak(player, data, snapshot);
            }
            for (MultiBreakCheck check : multiBreakChecks) {
                check.onPacketBreak(player, data, snapshot);
            }
        }

        if (trace != null) {
            trace.addEntry(CheckStage.PRE, "QueuedPlaceBreak",
                    System.nanoTime() - stageStart, true, null);
        }
    }

    private void runTimingChecks(Player player, MovementFrame frame, PlayerData data) {
        for (TimerCheck check : timerChecks) {
            check.onMovementFrame(player, frame, data);
        }
    }

    private void runQueuedAttackChecks(Player player, PlayerData data, PipelineTrace trace) {
        long stageStart = System.nanoTime();
        for (PlayerData.QueuedAttackSnapshot snapshot : data.consumeQueuedAttacks(data.getMoveWindow(), true)) {
            combatPipeline.onUseEntityAttackPacket(player, snapshot.getTargetEntityId(), snapshot);
        }
        if (trace != null) {
            trace.addEntry(CheckStage.COMBAT, "QueuedAttack",
                    System.nanoTime() - stageStart, true, null);
        }
    }

    private boolean runMovementPrediction(Player player, MovementFrame frame, Location to, PlayerData data,
            PipelineTrace trace) {
        long stageStart = System.nanoTime();
        data.beginPredictionFrame(frame.getTimestampNanos());
        for (PredictionMovementCheck check : predictionChecks) {
            check.onMovementFrame(player, frame, to, data);
        }
        FrameContextSnapshot context = data.getCurrentFrameContext();
        FrameContextSnapshot.PredictionOutputSnapshot output = new FrameContextSnapshot.PredictionOutputSnapshot(
                data.hasPredictionForFrame(frame.getTimestampNanos()),
                data.getPredictionMinDeviation(),
                data.getPredictionReducedDeviation(),
                data.getPredictionHorizontalDeviation(),
                data.getPredictionReducedHorizontalDeviation(),
                data.getPredictionBestProfile());
        if (context != null) {
            data.setCurrentFrameContext(context.withPredictionOutput(output));
        }
        boolean hasPrediction = output.isReady();
        if (trace != null) {
            trace.addEntry(CheckStage.PREDICTION, "Prediction",
                    System.nanoTime() - stageStart, hasPrediction, hasPrediction ? null : "no-prediction-frame");
        }
        return hasPrediction;
    }

    private void runPostPredictionChecks(Player player, MovementFrame frame, Location from, Location to,
            PlayerData data, PipelineTrace trace) {
        long stageStart = System.nanoTime();
        for (AimProcessorCheck check : aimProcessorChecks) {
            check.onMovementFrame(player, frame, data);
        }
        for (AimModulo360Check check : aimModulo360Checks) {
            check.onMovementFrame(player, frame, data);
        }
        for (AimDuplicateLookCheck check : aimDuplicateLookChecks) {
            check.onMovementFrame(player, frame, data);
        }
        for (NoSlowCheck check : noSlowChecks) {
            check.onMovementFrame(player, frame, data);
        }
        for (SpeedCheck check : speedChecks) {
            check.onMovementFrame(player, frame, from, to, data);
        }
        for (FlyCheck check : flyChecks) {
            check.onMovementFrame(player, frame, to, data);
        }
        for (PhaseCheck check : phaseChecks) {
            check.onMovementFrame(player, frame, to, data);
        }
        for (JesusCheck check : jesusChecks) {
            check.onMovementFrame(player, frame, data);
        }
        for (VelocityCheck check : velocityChecks) {
            check.onMovementFrame(player, frame, data);
        }
        for (GroundSpoofCheck check : groundSpoofChecks) {
            check.onMovementFrame(player, frame, to, data);
        }
        if (trace != null) {
            trace.addEntry(CheckStage.POST, "NoSlow+Speed+Fly+Phase+Jesus+Velocity+GroundSpoof",
                    System.nanoTime() - stageStart, true, null);
        }
    }

    private void runLegacyFallbackChecks(Player player, PlayerData data, Location from, Location to,
            MovementFrame frame, String reason, ToleranceBudgetEngine.BudgetSnapshot budget, PipelineTrace trace) {
        if (!plugin.getConfig().getBoolean("pipeline.legacy-onmove-fallback", true)) {
            if ("prediction-unavailable".equals(reason)
                    && plugin.getConfig().getBoolean("pipeline.minimal-post-on-prediction-miss", true)) {
                runMinimalPostChecks(player, data, frame, from, to, budget,
                        "prediction-unavailable-minimal-post", trace);
                return;
            }
            if (trace != null) {
                trace.addEntry(CheckStage.FALLBACK, "legacy-fallback",
                        0L, false, "disabled-in-config");
            }
            return;
        }
        legacyFallbackHitCount++;
        long stageStart = System.nanoTime();
        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName() + " legacy onMove fallback active: " + reason
                    + " source=" + frame.getSource().name() + " pathHits=" + legacyFallbackHitCount);
        }

        PlayerMoveEvent syntheticEvent = new PlayerMoveEvent(player, from, to);
        for (InventoryMoveCheck check : inventoryMoveChecks) {
            check.onMove(syntheticEvent, data);
        }
        for (NoSlowCheck check : noSlowChecks) {
            check.onMove(syntheticEvent, data);
        }
        for (FlyCheck check : flyChecks) {
            check.onMove(syntheticEvent, data);
        }
        for (PhaseCheck check : phaseChecks) {
            check.onMove(syntheticEvent, data);
        }
        for (JesusCheck check : jesusChecks) {
            check.onMove(syntheticEvent, data);
        }
        if (trace != null) {
            trace.addEntry(CheckStage.FALLBACK, "legacy-fallback(" + reason + ")",
                    System.nanoTime() - stageStart, true, null);
        }
    }

    private void runMinimalPostChecks(Player player, PlayerData data, MovementFrame frame, Location from, Location to,
            ToleranceBudgetEngine.BudgetSnapshot budget, String reasonCode, PipelineTrace trace) {
        long stageStart = System.nanoTime();
        minimalPostPredictionMissHitCount++;

        FileConfiguration config = plugin.getConfig();
        double movementAllowance = budget == null ? 0.0D : budget.getMovementAllowance();
        double velocitySlack = budget == null ? 0.0D : budget.getVelocityResponseSlack();

        double minimalSpeedThreshold = config.getDouble("pipeline.minimal-post.thresholds.speed-horizontal", 0.85D)
                + movementAllowance * config.getDouble("pipeline.minimal-post.thresholds.speed-budget-multiplier", 3.0D);
        int flyAirTicks = (int) Math.ceil(config.getDouble("pipeline.minimal-post.thresholds.fly-air-ticks", 14.0D)
                + velocitySlack * config.getDouble("pipeline.minimal-post.thresholds.fly-slack-multiplier", 30.0D));
        double flyMaxDy = config.getDouble("pipeline.minimal-post.thresholds.fly-max-dy", 0.035D)
                + movementAllowance * config.getDouble("pipeline.minimal-post.thresholds.fly-dy-budget-multiplier", 0.8D);
        double phaseRatio = config.getDouble("pipeline.minimal-post.thresholds.phase-min-overlap-ratio", 0.16D)
                + movementAllowance * config.getDouble("pipeline.minimal-post.thresholds.phase-ratio-budget-multiplier", 0.8D);
        double phaseVolume = config.getDouble("pipeline.minimal-post.thresholds.phase-min-overlap-volume", 0.05D)
                + movementAllowance * config.getDouble("pipeline.minimal-post.thresholds.phase-volume-budget-multiplier", 0.25D);
        int groundSpoofMinTicks = (int) Math.ceil(
                config.getDouble("pipeline.minimal-post.thresholds.groundspoof-min-ticks", 5.0D)
                        + velocitySlack * config.getDouble("pipeline.minimal-post.thresholds.groundspoof-slack-multiplier", 40.0D));
        double groundSpoofBuffer = config.getDouble("pipeline.minimal-post.thresholds.groundspoof-buffer", 4.0D)
                + movementAllowance * config.getDouble("pipeline.minimal-post.thresholds.groundspoof-budget-multiplier", 8.0D);

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName() + " minimal post checks active: " + reasonCode
                    + " source=" + frame.getSource().name() + " pathHits=" + minimalPostPredictionMissHitCount);
        }

        for (SpeedCheck check : speedChecks) {
            check.onPredictionMissMinimal(player, frame, from, to, data, minimalSpeedThreshold);
        }

        Object oldFlyAirTicks = config.get("checks.Fly.air-ticks-threshold");
        Object oldFlyDy = config.get("checks.Fly.max-dy");
        Object oldPhaseRatio = config.get("checks.Phase.min-overlap-ratio");
        Object oldPhaseVolume = config.get("checks.Phase.min-overlap-volume");
        Object oldGroundSpoofMinTicks = config.get("checks.GroundSpoof.min-ticks");
        Object oldGroundSpoofBuffer = config.get("checks.GroundSpoof.buffer");
        try {
            config.set("checks.Fly.air-ticks-threshold", Integer.valueOf(flyAirTicks));
            config.set("checks.Fly.max-dy", Double.valueOf(flyMaxDy));
            config.set("checks.Phase.min-overlap-ratio", Double.valueOf(phaseRatio));
            config.set("checks.Phase.min-overlap-volume", Double.valueOf(phaseVolume));
            config.set("checks.GroundSpoof.min-ticks", Integer.valueOf(groundSpoofMinTicks));
            config.set("checks.GroundSpoof.buffer", Double.valueOf(groundSpoofBuffer));

            for (FlyCheck check : flyChecks) {
                check.onMovementFrame(player, frame, to, data);
            }
            for (PhaseCheck check : phaseChecks) {
                check.onMovementFrame(player, frame, to, data);
            }
            for (GroundSpoofCheck check : groundSpoofChecks) {
                check.onMovementFrame(player, frame, to, data);
            }
        } finally {
            config.set("checks.Fly.air-ticks-threshold", oldFlyAirTicks);
            config.set("checks.Fly.max-dy", oldFlyDy);
            config.set("checks.Phase.min-overlap-ratio", oldPhaseRatio);
            config.set("checks.Phase.min-overlap-volume", oldPhaseVolume);
            config.set("checks.GroundSpoof.min-ticks", oldGroundSpoofMinTicks);
            config.set("checks.GroundSpoof.buffer", oldGroundSpoofBuffer);
        }

        if (trace != null) {
            trace.addEntry("minimal-post-checks", CheckStage.POST, PipelineTrace.Status.RAN,
                    System.nanoTime() - stageStart, reasonCode);
        }
    }
}
