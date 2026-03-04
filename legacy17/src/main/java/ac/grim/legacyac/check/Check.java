package ac.grim.legacyac.check;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.debug.DetectionEvidence;
import ac.grim.legacyac.regression.ViolationLedger;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import java.util.Locale;

public abstract class Check {
    protected final LegacyAntiCheatPlugin plugin;
    private final String name;

    // ── Stage map: CheckName → CheckStage (FR-2) ────────────────────────
    private static final Map<String, CheckStage> STAGE_MAP;
    static {
        Map<String, CheckStage> map = new HashMap<String, CheckStage>();
        // PRE stage — packet-level preprocessing
        map.put("Timer", CheckStage.PRE);
        map.put("InventoryMove", CheckStage.PRE);
        // PREDICTION stage — movement prediction
        map.put("Prediction", CheckStage.PREDICTION);
        // POST stage — post-prediction movement checks
        map.put("Speed", CheckStage.POST);
        map.put("Fly", CheckStage.POST);
        map.put("Phase", CheckStage.POST);
        map.put("NoFall", CheckStage.POST);
        map.put("Jesus", CheckStage.POST);
        map.put("NoSlow", CheckStage.POST);
        map.put("Knockback", CheckStage.POST);
        map.put("Velocity", CheckStage.POST);
        // COMBAT stage — attack-event-driven
        map.put("Reach", CheckStage.COMBAT);
        map.put("KillAura", CheckStage.COMBAT);
        // PASSIVE stage — rate-limit / timing
        map.put("AutoClicker", CheckStage.PASSIVE);
        map.put("FastPlace", CheckStage.PASSIVE);
        map.put("FastBreak", CheckStage.PASSIVE);
        map.put("FastUse", CheckStage.PASSIVE);
        // PRE stage — BadPackets (packet-level validation)
        map.put("BadPacketsA", CheckStage.PRE);
        map.put("BadPacketsC", CheckStage.PRE);
        map.put("BadPacketsD", CheckStage.PRE);
        map.put("BadPacketsE", CheckStage.PRE);
        map.put("BadPacketsF", CheckStage.PRE);
        map.put("BadPacketsG", CheckStage.PRE);
        map.put("BadPacketsI", CheckStage.PRE);
        map.put("BadPacketsL", CheckStage.PRE);
        map.put("BadPacketsO", CheckStage.PRE);
        map.put("BadPacketsQ", CheckStage.PRE);
        map.put("CrashA", CheckStage.PRE);
        // POST stage — GroundSpoof
        map.put("GroundSpoof", CheckStage.POST);
        // PASSIVE stage — Scaffold checks
        map.put("AirLiquidPlace", CheckStage.PASSIVE);
        map.put("FarPlace", CheckStage.PASSIVE);
        map.put("RotationPlace", CheckStage.PASSIVE);
        map.put("MultiPlace", CheckStage.PASSIVE);
        map.put("PositionPlace", CheckStage.PASSIVE);
        STAGE_MAP = Collections.unmodifiableMap(map);
    }

    protected Check(LegacyAntiCheatPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Return the pipeline stage this check belongs to (FR-2).
     * Subclasses may override for non-standard stage assignments.
     */
    public CheckStage getStage() {
        CheckStage stage = STAGE_MAP.get(name);
        return stage != null ? stage : CheckStage.FALLBACK;
    }

    /**
     * Convenience: get the per-frame tolerance budget from PlayerData (FR-3).
     * Returns null if no budget has been computed for this frame yet.
     */
    protected ToleranceBudgetEngine.BudgetSnapshot getBudget(PlayerData data) {
        return data.getCurrentBudget();
    }

    protected boolean isEnabled() {
        return plugin.getConfig().getBoolean("checks." + name + ".enabled", true);
    }

    protected double getMaxViolation() {
        return plugin.getConfig().getDouble("checks." + name + ".max-vl", 10.0D);
    }

    protected boolean isExempt(Player player, PlayerData data) {
        return isExempt(player, data, true); // Default: ignore velocity grace for movement checks
    }

    protected boolean isExempt(Player player, PlayerData data, boolean ignoreVelocityGrace) {
        // Bot soft-compat: skip all checks for bot players
        String nameLower = player.getName().toLowerCase(java.util.Locale.ROOT);
        if (nameLower.startsWith("[bot]") || nameLower.startsWith("nodebuff") || nameLower.contains("gapple")) {
            return true;
        }

        long now = System.currentTimeMillis();
        int joinGrace = plugin.getConfig().getInt("exempt.join-grace-ms", 2500);
        int teleportGrace = plugin.getConfig().getInt("exempt.teleport-grace-ms", 1000);
        int velocityGrace = plugin.getConfig().getInt("exempt.velocity-grace-ms", 400);

        if (now - data.getJoinAt() < joinGrace) {
            return true;
        }
        if (now - data.getLastTeleportAt() < teleportGrace) {
            return true;
        }
        if (!ignoreVelocityGrace && now - data.getLastVelocityAt() < velocityGrace) {
            return true;
        }

        return false;
    }

    protected double increaseBuffer(PlayerData data, double amount) {
        return data.addBuffer(name, amount);
    }

    protected double slideAndAddScore(PlayerData data, double deviation, double weight) {
        double decay = plugin.getConfig().getDouble("heuristics.window-decay", 0.95D);
        data.scaleBuffer(name, decay);
        return increaseBuffer(data, Math.max(0.0D, deviation) * weight);
    }

    protected void coolDownScore(PlayerData data) {
        double decay = plugin.getConfig().getDouble("heuristics.window-decay", 0.95D);
        data.scaleBuffer(name, decay);
    }

    protected boolean isLagging(PlayerData data) {
        double jitterMs = data.getTransactionRttJitterNanos() / 1000000.0D;
        double jitterThreshold = plugin.getConfig().getDouble("adaptive-lag.jitter-threshold-ms", 50.0D);
        double tps = plugin.checks().getCurrentTps();
        double minTps = plugin.getConfig().getDouble("adaptive-lag.min-tps", 18.0D);
        boolean networkLag = jitterMs >= jitterThreshold || tps < minTps;
        return networkLag && data.getMovementStateSnapshot().isFullyAligned();
    }

    protected void logAdaptiveLagComparison(Player player, PlayerData data, String checkName, double baseLimit,
            double finalLimit, String note) {
        if (!plugin.getConfig().getBoolean("adaptive-lag.compare-log-enabled", false)) {
            return;
        }
        plugin.getLogger().info("[GLAC-LAG-COMPARE] player=" + player.getName()
                + " check=" + checkName
                + " pending=" + data.getPendingWorldChangesCount()
                + " base=" + String.format(Locale.ROOT, "%.4f", baseLimit)
                + " final=" + String.format(Locale.ROOT, "%.4f", finalLimit)
                + " note=" + note);
    }

    protected void recordEvidence(PlayerData data, double offset, String sourceOverride) {
        data.recordDetectionEvidence(new DetectionEvidence(
                System.currentTimeMillis(),
                name,
                offset,
                data.getBuffer(name),
                data.getViolation(name),
                data.getLastTransactionRttNanos() / 1000000.0D,
                sourceOverride == null ? data.getDetectionSource() : sourceOverride,
                data.getDetectionTick()));
    }

    protected void flag(Player player, PlayerData data, double amount, String detail) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        double vl = data.addViolation(name, amount);
        recordEvidence(data, amount, null);

        // ── FR-5 Phase E: Record to ViolationLedger ──────────────────
        ToleranceBudgetEngine.BudgetSnapshot budget = getBudget(data);
        String budgetTag = budget != null ? budget.getScenarioTag() : "no-budget";
        ViolationLedger ledger = plugin.ledger();
        if (ledger != null) {
            ViolationLedger.ViolationEntry entry = new ViolationLedger.ViolationEntry.Builder()
                    .check(name)
                    .player(player.getName())
                    .score(amount)
                    .buffer(data.getBuffer(name))
                    .vl(vl)
                    .rttMs(data.getLastTransactionRttNanos() / 1000000.0D)
                    .source(data.getDetectionSource())
                    .detail(detail)
                    .budgetTag(budgetTag)
                    .build();
            ledger.record(name, entry);
        }

        if (data.isDebugEnabled()) {
            String evidence = "[" + name + "] P:" + String.format(Locale.ROOT, "%.2f", amount)
                    + ", RTT:" + String.format(Locale.ROOT, "%.0fms", data.getLastTransactionRttNanos() / 1000000.0D)
                    + ", Tick:" + data.getMoveWindow()
                    + ", Buffer:" + String.format(Locale.ROOT, "%.2f", data.getBuffer(name))
                    + ", Budget:" + budgetTag
                    + ", Detail:" + detail;
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName() + " " + evidence);
        }
        plugin.alerts().alert(player, name, vl, detail + " [budget=" + budgetTag + "]");

        if (vl >= getMaxViolation() && plugin.getConfig().getBoolean("checks." + name + ".setback", true)
                && data.getLastSafeLocation() != null) {
            player.teleport(data.getLastSafeLocation());
        }

        runPunishments(player, data, vl);
    }

    private void runPunishments(Player player, PlayerData data, double vl) {
        // OP players are checked but never punished
        if (player.hasPermission("grimlegacy.bypass")) {
            return;
        }
        double punishVl = plugin.getConfig().getDouble("checks." + name + ".punish-vl", -1.0D);
        if (punishVl <= 0.0D || vl < punishVl || data.hasExecutedPunish(name)) {
            return;
        }

        List<String> commands = plugin.getConfig().getStringList("checks." + name + ".punish-commands");
        for (String command : commands) {
            String parsed = command.replace("%player%", player.getName())
                    .replace("%check%", name)
                    .replace("%vl%", String.format(Locale.ROOT, "%.2f", vl));
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsed);
        }
        data.markPunishExecuted(name);
    }
}
