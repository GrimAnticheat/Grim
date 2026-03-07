package ac.grim.legacyac.check;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.impl.AutoClickerCheck;
import ac.grim.legacyac.check.impl.FastBreakCheck;
import ac.grim.legacyac.check.impl.FastPlaceCheck;
import ac.grim.legacyac.check.impl.FastUseCheck;
import ac.grim.legacyac.check.impl.FlyCheck;
import ac.grim.legacyac.check.impl.InventoryMoveCheck;
import ac.grim.legacyac.check.impl.JesusCheck;
import ac.grim.legacyac.check.impl.KillAuraCheck;
import ac.grim.legacyac.check.impl.NoFallCheck;
import ac.grim.legacyac.check.impl.NoSlowCheck;
import ac.grim.legacyac.check.impl.PhaseCheck;
import ac.grim.legacyac.check.impl.PredictionMovementCheck;
import ac.grim.legacyac.check.impl.ReachCheck;
import ac.grim.legacyac.check.impl.SpeedCheck;
import ac.grim.legacyac.check.impl.TimerCheck;
import ac.grim.legacyac.check.impl.KnockbackHandlerLegacy;
import ac.grim.legacyac.check.impl.VelocityCheck;
import ac.grim.legacyac.check.impl.GroundSpoofCheck;
import ac.grim.legacyac.check.impl.aim.AimDuplicateLookCheck;
import ac.grim.legacyac.check.impl.aim.AimModulo360Check;
import ac.grim.legacyac.check.impl.aim.AimProcessorCheck;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsA;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsC;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsD;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsE;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsF;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsG;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsI;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsL;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsO;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsQ;
import ac.grim.legacyac.check.impl.badpackets.CrashA;
import ac.grim.legacyac.check.impl.scaffold.AirLiquidPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.FarPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.MultiPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.DuplicateRotPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.FabricatedPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.PositionPlaceCheck;
import ac.grim.legacyac.check.impl.scaffold.RotationPlaceCheck;
import ac.grim.legacyac.check.impl.breaking.AirLiquidBreakCheck;
import ac.grim.legacyac.check.impl.breaking.FarBreakCheck;
import ac.grim.legacyac.check.impl.breaking.MultiBreakCheck;
import ac.grim.legacyac.check.impl.breaking.RotationBreakCheck;
import ac.grim.legacyac.combat.EntityIdIndex;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.data.state.CompensationState;
import ac.grim.legacyac.network.InternalPacketEvent;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import java.util.Locale;

public final class CheckManager implements Listener {
    private final LegacyAntiCheatPlugin plugin;
    private final EntityIdIndex entityIdIndex;
    private final List<SpeedCheck> speedChecks = new ArrayList<SpeedCheck>();
    private final List<FlyCheck> flyChecks = new ArrayList<FlyCheck>();
    private final List<PhaseCheck> phaseChecks = new ArrayList<PhaseCheck>();
    private final List<ReachCheck> reachChecks = new ArrayList<ReachCheck>();
    private final List<AutoClickerCheck> autoClickerChecks = new ArrayList<AutoClickerCheck>();
    private final List<NoFallCheck> noFallChecks = new ArrayList<NoFallCheck>();
    private final List<KillAuraCheck> killAuraChecks = new ArrayList<KillAuraCheck>();
    private final List<TimerCheck> timerChecks = new ArrayList<TimerCheck>();
    private final List<KnockbackHandlerLegacy> knockbackChecks = new ArrayList<KnockbackHandlerLegacy>();
    private final List<JesusCheck> jesusChecks = new ArrayList<JesusCheck>();
    private final List<FastPlaceCheck> fastPlaceChecks = new ArrayList<FastPlaceCheck>();
    private final List<FastBreakCheck> fastBreakChecks = new ArrayList<FastBreakCheck>();
    private final List<FastUseCheck> fastUseChecks = new ArrayList<FastUseCheck>();
    private final List<InventoryMoveCheck> inventoryMoveChecks = new ArrayList<InventoryMoveCheck>();
    private final List<PredictionMovementCheck> predictionChecks = new ArrayList<PredictionMovementCheck>();
    private final List<NoSlowCheck> noSlowChecks = new ArrayList<NoSlowCheck>();
    private final List<VelocityCheck> velocityChecks = new ArrayList<VelocityCheck>();
    private final List<GroundSpoofCheck> groundSpoofChecks = new ArrayList<GroundSpoofCheck>();
    private final List<AimProcessorCheck> aimProcessorChecks = new ArrayList<AimProcessorCheck>();
    private final List<AimModulo360Check> aimModulo360Checks = new ArrayList<AimModulo360Check>();
    private final List<AimDuplicateLookCheck> aimDuplicateLookChecks = new ArrayList<AimDuplicateLookCheck>();
    // BadPackets
    private final List<BadPacketsA> badPacketsAChecks = new ArrayList<BadPacketsA>();
    private final List<BadPacketsC> badPacketsCChecks = new ArrayList<BadPacketsC>();
    private final List<BadPacketsD> badPacketsDChecks = new ArrayList<BadPacketsD>();
    private final List<BadPacketsE> badPacketsEChecks = new ArrayList<BadPacketsE>();
    private final List<BadPacketsF> badPacketsFChecks = new ArrayList<BadPacketsF>();
    private final List<BadPacketsG> badPacketsGChecks = new ArrayList<BadPacketsG>();
    private final List<BadPacketsI> badPacketsIChecks = new ArrayList<BadPacketsI>();
    private final List<BadPacketsL> badPacketsLChecks = new ArrayList<BadPacketsL>();
    private final List<BadPacketsO> badPacketsOChecks = new ArrayList<BadPacketsO>();
    private final List<BadPacketsQ> badPacketsQChecks = new ArrayList<BadPacketsQ>();
    private final List<CrashA> crashAChecks = new ArrayList<CrashA>();
    // Scaffold
    private final List<AirLiquidPlaceCheck> airLiquidPlaceChecks = new ArrayList<AirLiquidPlaceCheck>();
    private final List<FarPlaceCheck> farPlaceChecks = new ArrayList<FarPlaceCheck>();
    private final List<RotationPlaceCheck> rotationPlaceChecks = new ArrayList<RotationPlaceCheck>();
    private final List<MultiPlaceCheck> multiPlaceChecks = new ArrayList<MultiPlaceCheck>();
    private final List<PositionPlaceCheck> positionPlaceChecks = new ArrayList<PositionPlaceCheck>();
    private final List<DuplicateRotPlaceCheck> duplicateRotPlaceChecks = new ArrayList<DuplicateRotPlaceCheck>();
    private final List<FabricatedPlaceCheck> fabricatedPlaceChecks = new ArrayList<FabricatedPlaceCheck>();
    private final List<AirLiquidBreakCheck> airLiquidBreakChecks = new ArrayList<AirLiquidBreakCheck>();
    private final List<FarBreakCheck> farBreakChecks = new ArrayList<FarBreakCheck>();
    private final List<RotationBreakCheck> rotationBreakChecks = new ArrayList<RotationBreakCheck>();
    private final List<MultiBreakCheck> multiBreakChecks = new ArrayList<MultiBreakCheck>();
    private long lastTickAtNanos;
    private double currentTps = 20.0D;
    private final ToleranceBudgetEngine.ConfigProvider budgetConfigProvider;

    public CheckManager(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.entityIdIndex = new EntityIdIndex(plugin.getLogger());
        this.budgetConfigProvider = ToleranceBudgetEngine.fromBukkitConfig(plugin.getConfig());
        speedChecks.add(new SpeedCheck(plugin));
        flyChecks.add(new FlyCheck(plugin));
        phaseChecks.add(new PhaseCheck(plugin));
        reachChecks.add(new ReachCheck(plugin));
        autoClickerChecks.add(new AutoClickerCheck(plugin));
        noFallChecks.add(new NoFallCheck(plugin));
        killAuraChecks.add(new KillAuraCheck(plugin));
        timerChecks.add(new TimerCheck(plugin));
        knockbackChecks.add(new KnockbackHandlerLegacy(plugin));
        jesusChecks.add(new JesusCheck(plugin));
        fastPlaceChecks.add(new FastPlaceCheck(plugin));
        fastBreakChecks.add(new FastBreakCheck(plugin));
        fastUseChecks.add(new FastUseCheck(plugin));
        inventoryMoveChecks.add(new InventoryMoveCheck(plugin));
        predictionChecks.add(new PredictionMovementCheck(plugin));
        noSlowChecks.add(new NoSlowCheck(plugin));
        velocityChecks.add(new VelocityCheck(plugin));
        groundSpoofChecks.add(new GroundSpoofCheck(plugin));
        aimProcessorChecks.add(new AimProcessorCheck(plugin));
        aimModulo360Checks.add(new AimModulo360Check(plugin));
        aimDuplicateLookChecks.add(new AimDuplicateLookCheck(plugin));
        // BadPackets
        badPacketsAChecks.add(new BadPacketsA(plugin));
        badPacketsCChecks.add(new BadPacketsC(plugin));
        badPacketsDChecks.add(new BadPacketsD(plugin));
        badPacketsEChecks.add(new BadPacketsE(plugin));
        badPacketsFChecks.add(new BadPacketsF(plugin));
        badPacketsGChecks.add(new BadPacketsG(plugin));
        badPacketsIChecks.add(new BadPacketsI(plugin));
        badPacketsLChecks.add(new BadPacketsL(plugin));
        badPacketsOChecks.add(new BadPacketsO(plugin));
        badPacketsQChecks.add(new BadPacketsQ(plugin));
        crashAChecks.add(new CrashA(plugin));
        // Scaffold
        airLiquidPlaceChecks.add(new AirLiquidPlaceCheck(plugin));
        farPlaceChecks.add(new FarPlaceCheck(plugin));
        rotationPlaceChecks.add(new RotationPlaceCheck(plugin));
        multiPlaceChecks.add(new MultiPlaceCheck(plugin));
        positionPlaceChecks.add(new PositionPlaceCheck(plugin));
        duplicateRotPlaceChecks.add(new DuplicateRotPlaceCheck(plugin));
        fabricatedPlaceChecks.add(new FabricatedPlaceCheck(plugin));
        airLiquidBreakChecks.add(new AirLiquidBreakCheck(plugin));
        farBreakChecks.add(new FarBreakCheck(plugin));
        rotationBreakChecks.add(new RotationBreakCheck(plugin));
        multiBreakChecks.add(new MultiBreakCheck(plugin));

        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                entityIdIndex.put(entity);
            }
        }
    }

    public int getCheckCount() {
        return speedChecks.size() + flyChecks.size() + phaseChecks.size() + reachChecks.size()
                + autoClickerChecks.size() + noFallChecks.size()
                + killAuraChecks.size() + timerChecks.size() + knockbackChecks.size() + jesusChecks.size()
                + fastPlaceChecks.size() + fastBreakChecks.size()
                + fastUseChecks.size() + inventoryMoveChecks.size() + predictionChecks.size() + noSlowChecks.size()
                + velocityChecks.size() + groundSpoofChecks.size()
                + aimProcessorChecks.size() + aimModulo360Checks.size() + aimDuplicateLookChecks.size()
                + duplicateRotPlaceChecks.size() + fabricatedPlaceChecks.size()
                + airLiquidBreakChecks.size() + farBreakChecks.size() + rotationBreakChecks.size() + multiBreakChecks.size()
                + badPacketsAChecks.size() + badPacketsCChecks.size() + badPacketsDChecks.size()
                + badPacketsEChecks.size() + badPacketsFChecks.size() + badPacketsGChecks.size()
                + badPacketsIChecks.size() + badPacketsLChecks.size() + badPacketsOChecks.size()
                + badPacketsQChecks.size() + crashAChecks.size()
                + airLiquidPlaceChecks.size() + farPlaceChecks.size() + rotationPlaceChecks.size()
                + multiPlaceChecks.size() + positionPlaceChecks.size();
    }

    public void tick() {
        long now = System.nanoTime();
        if (lastTickAtNanos != 0L) {
            double elapsed = (now - lastTickAtNanos) / 1000000000.0D;
            if (elapsed > 0.0D) {
                double measured = Math.min(20.0D, 1.0D / elapsed);
                currentTps = (currentTps * 0.8D) + (measured * 0.2D);
            }
        }
        lastTickAtNanos = now;

        double decay = plugin.getConfig().getDouble("violation-decay-per-second", 0.08D);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.getPlayerData(player).decayViolations(decay);
        }
    }

    public double getCurrentTps() {
        return currentTps;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        data.setJoinAt(System.currentTimeMillis());
        data.preloadCompensatedWorld(event.getPlayer(), 2);
        entityIdIndex.put(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        entityIdIndex.remove(event.getPlayer());
        plugin.removePlayerData(event.getPlayer());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        entityIdIndex.remove(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Player player = event.getPlayer();
        boolean packetFirst = plugin.getConfig().getBoolean("pipeline.packet-first", true);
        boolean packetActive = plugin.isPacketPipelineActive();
        boolean bukkitFallback = plugin.getConfig().getBoolean("pipeline.bukkit-fallback", true);

        if (packetFirst && packetActive) {
            if (!bukkitFallback) {
                return;
            }
            PlayerData fallbackData = plugin.getPlayerData(player);
            long nowNanos = System.nanoTime();
            long frameAgeNanos = nowNanos - fallbackData.getLastMovementFrameAtNanos();
            long staleThresholdNanos = plugin.getConfig().getLong("pipeline.bukkit-fallback-stale-nanos", 150000000L);
            if (fallbackData.getLastMovementFrameAtNanos() != 0L && frameAgeNanos <= staleThresholdNanos) {
                return;
            }
        }

        long now = System.nanoTime();
        MovementFrame frame = new MovementFrame(now, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch(),
                player.isOnGround(), true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
        consumeMovementFrame(player, frame, event.getFrom(), to);
    }

    public void onMovementFrame(Player player, MovementFrame frame) {
        consumeMovementFrame(player, frame, null, null);
    }

    private void consumeMovementFrame(Player player, MovementFrame frame, Location explicitFrom, Location explicitTo) {
        PlayerData data = plugin.getPlayerData(player);

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

        // ── FR-3: Compute tolerance budget for this frame ──
        ToleranceBudgetEngine.BudgetSnapshot budget = ToleranceBudgetEngine.compute(
                data.network(), data.compensation(), data.environment(), currentTps, budgetConfigProvider);
        data.setCurrentBudget(budget);

        if (data.isDebugEnabled() && plugin.getConfig().getBoolean("adaptive-lag.compare-log-enabled", false)) {
            plugin.getLogger().info("[GLAC-BUDGET] " + player.getName() + " " + budget.toDebugString());
        }

        CompensationState.MovementStateSnapshot snapshot = data.compensation().getMovementStateSnapshot();
        if (!snapshot.isTeleportAligned()) {
            if (data.isDebugEnabled()) {
                plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                        + " checks SKIPPED: teleport-not-aligned pending=" + snapshot.getPendingChanges());
            }
            if (trace != null)
                trace.addEntry("*", CheckStage.PRE, PipelineTrace.Status.SKIPPED, 0L, "teleport-not-aligned");
            runLegacyFallbackChecks(player, data, from, to, frame, "teleport-not-aligned", trace);
            emitPipelineTrace(trace, pipelineStart);
            return;
        }

        runPacketStatePreprocess(player, frame, data, trace);
        boolean predictionReady = runMovementPrediction(player, frame, to, data, trace);
        data.updateKnockbackStages();

        if (predictionReady) {
            if (trace != null)
                trace.addEntry("Prediction", CheckStage.PREDICTION, PipelineTrace.Status.RAN, 0L, null);
            runPostPredictionChecks(player, frame, from, to, data, trace);
        } else {
            if (trace != null)
                trace.addEntry("Prediction", CheckStage.PREDICTION, PipelineTrace.Status.SKIPPED, 0L,
                        "prediction-unavailable");
            runLegacyFallbackChecks(player, data, from, to, frame, "prediction-unavailable", trace);
        }

        if (frame.isOnGround() && data.getLastDeltaXZ() < 0.35D && Math.abs(data.getLastDeltaY()) < 0.02D) {
            data.setLastSafeLocation(to.clone());
        }

        emitPipelineTrace(trace, pipelineStart);
    }

    private void emitPipelineTrace(PipelineTrace trace, long startNanos) {
        if (trace == null)
            return;
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

    private void runTimingChecks(Player player, MovementFrame frame, PlayerData data) {
        for (TimerCheck check : timerChecks) {
            check.onMovementFrame(player, frame, data);
        }
    }

    private boolean runMovementPrediction(Player player, MovementFrame frame, Location to, PlayerData data,
            PipelineTrace trace) {
        long stageStart = System.nanoTime();
        data.beginPredictionFrame(frame.getTimestampNanos());
        for (PredictionMovementCheck check : predictionChecks) {
            check.onMovementFrame(player, frame, to, data);
        }
        boolean hasPrediction = data.hasPredictionForFrame(frame.getTimestampNanos());
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
        for (KnockbackHandlerLegacy check : knockbackChecks) {
            check.onMovementFrame(player, frame, data);
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
            trace.addEntry(CheckStage.POST, "NoSlow+Speed+Fly+Phase+KB+Jesus+Velocity+GroundSpoof",
                    System.nanoTime() - stageStart, true, null);
        }
    }

    private void runLegacyFallbackChecks(Player player, PlayerData data, Location from, Location to,
            MovementFrame frame, String reason, PipelineTrace trace) {
        if (!plugin.getConfig().getBoolean("pipeline.legacy-onmove-fallback", true)) {
            if (trace != null) {
                trace.addEntry(CheckStage.FALLBACK, "legacy-fallback",
                        0L, false, "disabled-in-config");
            }
            return;
        }
        long stageStart = System.nanoTime();
        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName() + " legacy onMove fallback active: " + reason
                    + " source=" + frame.getSource().name());
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

    public String describeMovementExecutionPath() {
        boolean packetFirst = plugin.getConfig().getBoolean("pipeline.packet-first", true);
        boolean packetActive = plugin.isPacketPipelineActive();
        boolean bukkitFallback = plugin.getConfig().getBoolean("pipeline.bukkit-fallback", true);
        if (packetFirst && packetActive) {
            if (bukkitFallback) {
                return "PACKET_FIRST_WITH_STALE_BUKKIT_FALLBACK";
            }
            return "PACKET_ONLY";
        }
        return "BUKKIT_MOVE_EVENT";
    }

    public String describeMovementPipelineTopology() {
        StringBuilder sb = new StringBuilder();
        sb.append("preprocess(packet/state): Timer, InventoryMove");
        sb.append(" | prediction(shared-frame): Prediction");
        sb.append(" | post-prediction: NoSlow=").append(!noSlowChecks.isEmpty());
        sb.append(", Speed=").append(!speedChecks.isEmpty());
        sb.append(", Fly=").append(!flyChecks.isEmpty());
        sb.append(", Phase=").append(!phaseChecks.isEmpty());
        sb.append(", Knockback=").append(!knockbackChecks.isEmpty());
        sb.append(", Jesus=").append(!jesusChecks.isEmpty());
        sb.append(", Reach=").append(!reachChecks.isEmpty()).append("(attack-stage)");
        sb.append(" | legacy-onMove-fallback=")
                .append(plugin.getConfig().getBoolean("pipeline.legacy-onmove-fallback", true));
        return sb.toString();
    }

    public void onInternalPacketEvent(InternalPacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        PlayerData data = plugin.getPlayerData(player);

        if (event.getType() == InternalPacketEvent.Type.CLIENT_MOVEMENT) {
            data.setLastRawMovementPacketAt(event.getCreatedAtNanos());
            data.incrementRawMovementPacketCounter();
            // BadPacketsD + BadPacketsE: check pitch and look-only packets
            Boolean hasPos = event.getHasPosition();
            Float pitch = event.getPitch();
            Float yaw = event.getYaw();
            if (pitch != null && yaw != null) {
                for (CrashA check : crashAChecks) {
                    check.onRotation(player, data, yaw.floatValue(), pitch.floatValue());
                }
                for (BadPacketsD check : badPacketsDChecks) {
                    check.onRotation(player, data, pitch.floatValue());
                }
            }
            if (hasPos != null) {
                for (BadPacketsE check : badPacketsEChecks) {
                    check.onFlyingPacket(player, data, hasPos.booleanValue());
                }
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_TRANSACTION_ACK) {
            Short actionId = event.getTransactionActionId();
            if (actionId != null) {
                data.acknowledgeTransaction(actionId.shortValue(), event.getCreatedAtNanos());
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_KEEP_ALIVE) {
            data.acknowledgeKeepAlive(System.currentTimeMillis());
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.SERVER_POSITION) {
            data.setLastServerPositionSyncAt(event.getCreatedAtNanos());
            Double x = event.getX();
            Double y = event.getY();
            Double z = event.getZ();
            if (x != null && y != null && z != null) {
                data.beginTeleportSync(x.doubleValue(), y.doubleValue(), z.doubleValue());
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_USE_ENTITY) {
            // BadPacketsC: self-interaction check
            Integer entityId = event.getEntityId();
            if (entityId != null) {
                for (BadPacketsC check : badPacketsCChecks) {
                    check.onUseEntity(player, data, entityId.intValue());
                }
            }
            if (event.isAttackAction() && entityId != null) {
                onUseEntityAttackPacket(player, entityId.intValue());
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.SERVER_ENTITY_VELOCITY) {
            Integer entityId = event.getEntityId();
            Integer vx = event.getVelocityX();
            Integer vy = event.getVelocityY();
            Integer vz = event.getVelocityZ();
            if (entityId == null || vx == null || vy == null || vz == null) {
                return;
            }
            for (KnockbackHandlerLegacy check : knockbackChecks) {
                check.onVelocityPacket(player, data, entityId.intValue(), vx.intValue(), vy.intValue(), vz.intValue(),
                        event.getCreatedAtNanos());
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_HELD_ITEM_CHANGE) {
            Integer slot = event.getSlot();
            if (slot != null) {
                data.clearUsingItemPacket();
                for (BadPacketsA check : badPacketsAChecks) {
                    check.onHeldItemChange(player, data, slot.intValue());
                }
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_ENTITY_ACTION) {
            Integer entityId = event.getEntityId();
            Integer actionId = event.getActionId();
            Integer jumpBoost = event.getJumpBoost();
            Boolean isSprint = event.getSprintAction();
            Boolean isSneak = event.getSneakAction();
            if (entityId != null && actionId != null && jumpBoost != null) {
                for (BadPacketsQ check : badPacketsQChecks) {
                    check.onEntityAction(player, data, entityId.intValue(), actionId.intValue(), jumpBoost.intValue());
                }
            }
            if (isSprint != null && isSprint.booleanValue()) {
                boolean startSprint = actionId != null && actionId.intValue() == 4; // START_SPRINT=4, STOP_SPRINT=5
                for (BadPacketsF check : badPacketsFChecks) {
                    check.onSprintAction(player, data, startSprint);
                }
            }
            if (isSneak != null && isSneak.booleanValue()) {
                boolean startSneak = actionId != null && actionId.intValue() == 1; // 1.7: START_SNEAK=1, STOP_SNEAK=2
                for (BadPacketsG check : badPacketsGChecks) {
                    check.onSneakAction(player, data, startSneak);
                }
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_ABILITIES) {
            Boolean claimsFlying = event.getClaimsFlying();
            if (claimsFlying != null) {
                for (BadPacketsI check : badPacketsIChecks) {
                    check.onAbilitiesPacket(player, data, claimsFlying.booleanValue());
                }
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_BLOCK_DIG) {
            Integer digAction = event.getDigAction();
            if (digAction != null) {
                if (digAction.intValue() == 5) {
                    data.clearUsingItemPacket();
                }
                for (BadPacketsL check : badPacketsLChecks) {
                    check.onDigAction(player, data, digAction.intValue());
                }
            }
        }
    }

    public void onUseEntityAttackPacket(final Player attacker, final int targetEntityId) {
        Entity targetEntity = entityIdIndex.get(targetEntityId);
        if (targetEntity == null) {
            entityIdIndex.recordFallbackScan();
            for (Entity entity : attacker.getWorld().getEntities()) {
                if (entity.getEntityId() == targetEntityId) {
                    targetEntity = entity;
                    entityIdIndex.put(entity);
                    break;
                }
            }
        }
        if (!(targetEntity instanceof Player)) {
            return;
        }

        final Player target = (Player) targetEntity;
        final PlayerData attackerData = plugin.getPlayerData(attacker);
        attackerData.setDetectionContext("USE_ENTITY_PACKET", attackerData.getMoveWindow());
        if (attackerData.isTeleportSyncPending()) {
            if (attackerData.isDebugEnabled()) {
                plugin.getLogger()
                        .info("[GLAC-DEBUG] " + attacker.getName() + " attack packet blocked: teleport-sync-pending");
            }
            return;
        }
        PlayerData targetData = plugin.getPlayerData(target);
        if (!target.isOnline() || target.isDead() || target.getHealth() <= 0.0D || targetData.isTeleportSyncPending()) {
            return;
        }
        double[] targetBox = plugin.resolveEntityBox(target);
        Location targetLoc = target.getLocation();
        boolean teleportMarker = System.currentTimeMillis() - targetData.getLastTeleportOrPearlAt() <= 400L;
        boolean transactionAligned = targetData.hasRecentTransactionAck(2000L);
        boolean enforceable = transactionAligned && !targetData.isTeleportSyncPending();
        targetData.recordCurrentHitbox(targetLoc.getX(), targetLoc.getY(), targetLoc.getZ(), targetBox[0], targetBox[1],
                teleportMarker, transactionAligned, enforceable);
        final long backtrackWindow = plugin.getConfig().getLong("combat.backtrack-window-ms", 400L);
        final ReachCheck.AttackEvaluation reachEval;
        if (reachChecks.isEmpty()) {
            reachEval = new ReachCheck.AttackEvaluation(true, 0.0D, 0L, false, true, ReachCheck.ReachEvidenceType.NONE);
        } else {
            reachEval = reachChecks.get(0).onUseEntityAttack(attacker, target, attackerData, backtrackWindow);
        }

        if (attackerData.isDebugEnabled()) {
            double baseReach = plugin.getConfig().getDouble("checks.Reach.Ray-Distance", 3.1D);
            plugin.getLogger().info("[GLAC-DEBUG] " + attacker.getName() + " -> " + target.getName()
                    + " Ray-Distance: " + String.format(Locale.ROOT, "%.2f", reachEval.getDirectDistance())
                    + ", Config: " + String.format(Locale.ROOT, "%.2f", baseReach)
                    + ", Box-Time-Offset: " + reachEval.getBoxTimeOffsetMs() + "ms");
        }

        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (KillAuraCheck check : killAuraChecks) {
                    check.onUseEntityAttack(attacker, target, attackerData, reachEval.isLegal());
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        PlayerData data = plugin.getPlayerData(attacker);
        data.setDetectionContext("ENTITY_DAMAGE_EVENT", data.getMoveWindow());
        for (ReachCheck check : reachChecks) {
            check.onAttack(event, attacker, (Player) event.getEntity(), data);
        }
        for (KillAuraCheck check : killAuraChecks) {
            check.onAttack(event, attacker, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }
        if (event.getCaught() instanceof Player) {
            PlayerData data = plugin.getPlayerData((Player) event.getCaught());
            data.recordRodPull();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        Material inHand = event.getPlayer().getItemInHand() == null ? Material.AIR
                : event.getPlayer().getItemInHand().getType();
        if (inHand == Material.ENDER_PEARL) {
            data.setLastTeleportOrPearlAt(System.currentTimeMillis());
        }
        for (AutoClickerCheck check : autoClickerChecks) {
            check.onInteract(event, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        data.recordPendingBlockChange("place:" + event.getBlockPlaced().getType().name());
        data.queueCompensatedBlockChange(event.getPlayer(), event.getBlockPlaced().getX(), event.getBlockPlaced().getY(), event.getBlockPlaced().getZ(),
                event.getBlockPlaced().getType(), event.getBlockPlaced().getData(), "event:block-place");
        for (FastPlaceCheck check : fastPlaceChecks) {
            check.onPlace(event, data);
        }
        for (AirLiquidPlaceCheck check : airLiquidPlaceChecks) {
            check.onPlace(event, data);
        }
        for (FarPlaceCheck check : farPlaceChecks) {
            check.onPlace(event, data);
        }
        for (RotationPlaceCheck check : rotationPlaceChecks) {
            check.onPlace(event, data);
        }
        for (MultiPlaceCheck check : multiPlaceChecks) {
            check.onPlace(event, data);
        }
        for (PositionPlaceCheck check : positionPlaceChecks) {
            check.onPlace(event, data);
        }
        for (DuplicateRotPlaceCheck check : duplicateRotPlaceChecks) {
            check.onPlace(event, data);
        }
        for (FabricatedPlaceCheck check : fabricatedPlaceChecks) {
            check.onPlace(event, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        data.recordPendingBlockChange("break:" + event.getBlock().getType().name());
        data.queueCompensatedBlockChange(event.getPlayer(), event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(),
                Material.AIR, (byte) 0, "event:block-break");
        for (FastBreakCheck check : fastBreakChecks) {
            check.onBreak(event, data);
        }
        for (AirLiquidBreakCheck check : airLiquidBreakChecks) {
            check.onBreak(event, data);
        }
        for (FarBreakCheck check : farBreakChecks) {
            check.onBreak(event, data);
        }
        for (RotationBreakCheck check : rotationBreakChecks) {
            check.onBreak(event, data);
        }
        for (MultiBreakCheck check : multiBreakChecks) {
            check.onBreak(event, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        data.clearUsingItemPacket();
        for (FastUseCheck check : fastUseChecks) {
            check.onConsume(event, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        data.clearUsingItemPacket();
        data.markSlotSwitch();
        int graceTicks = plugin.getConfig().getInt("checks.NoSlow.slot-switch-grace-ticks", 2);
        data.startSlotSwitchGrace(graceTicks);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            plugin.getPlayerData((Player) event.getPlayer()).setInventoryOpen(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            plugin.getPlayerData((Player) event.getPlayer()).setInventoryOpen(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        plugin.getPlayerData(event.getPlayer()).setLastTeleportAt(System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        data.setLastVelocityAt(System.currentTimeMillis());
        data.recordPendingVelocityChange();
        // Store the actual XZ magnitude so speed check can account for it
        org.bukkit.util.Vector vel = event.getVelocity();
        double xzMagnitude = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
        data.setLastVelocityXZ(xzMagnitude);
        // Feed VelocityCheck
        for (VelocityCheck check : velocityChecks) {
            check.onVelocity(event, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        Player player = (Player) event.getEntity();
        PlayerData data = plugin.getPlayerData(player);
        data.recordHighFallLanding();
        for (NoFallCheck check : noFallChecks) {
            check.onFallDamage(event, player, data);
        }
    }
}



