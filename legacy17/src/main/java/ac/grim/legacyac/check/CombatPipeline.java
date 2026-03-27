package ac.grim.legacyac.check;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.impl.KillAuraCheck;
import ac.grim.legacyac.check.impl.ReachCheck;
import ac.grim.legacyac.combat.EntityIdIndex;
import ac.grim.legacyac.data.FrameContextSnapshot;
import ac.grim.legacyac.data.PlayerData;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

final class CombatPipeline {
    private final LegacyAntiCheatPlugin plugin;
    private final EntityIdIndex entityIdIndex;
    private final List<ReachCheck> reachChecks;
    private final List<KillAuraCheck> killAuraChecks;

    CombatPipeline(LegacyAntiCheatPlugin plugin, EntityIdIndex entityIdIndex,
            List<ReachCheck> reachChecks, List<KillAuraCheck> killAuraChecks) {
        this.plugin = plugin;
        this.entityIdIndex = entityIdIndex;
        this.reachChecks = reachChecks;
        this.killAuraChecks = killAuraChecks;
    }

    void onUseEntityAttackPacket(final Player attacker, final int targetEntityId,
            final PlayerData.QueuedAttackSnapshot snapshot) {
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
                plugin.getLogger().info("[GLAC-DEBUG] " + attacker.getName()
                        + " attack packet blocked: teleport-sync-pending");
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

        java.util.List<ac.grim.legacyac.combat.HitboxFrame> attackFrames = targetData.getHitboxHistorySnapshot(400L);
        if (!attackFrames.isEmpty()) {
            FrameContextSnapshot attackerFrameContext = attackerData.getCurrentFrameContext();
            if (attackerFrameContext != null) {
                attackerData.setCurrentFrameContext(attackerFrameContext.withTargetHitboxSnapshot(
                        FrameContextSnapshot.HitboxSnapshot.fromFrame(attackFrames.get(0))));
            }
        }

        final long backtrackWindow = resolveCombatBacktrackWindow(attackerData);
        final ReachCheck.AttackEvaluation reachEval;
        if (reachChecks.isEmpty()) {
            reachEval = new ReachCheck.AttackEvaluation(true, 0.0D, 0L, false, true,
                    ReachCheck.ReachEvidenceType.NONE);
        } else {
            reachEval = reachChecks.get(0).onUseEntityAttack(attacker, target, attackerData, backtrackWindow, snapshot);
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
                    check.onUseEntityAttack(attacker, target, attackerData, reachEval);
                }
            }
        });
    }

    void onAttackFallback(EntityDamageByEntityEvent event) {
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

    private long resolveCombatBacktrackWindow(PlayerData attackerData) {
        long configuredMax = plugin.getConfig().getLong("combat.backtrack-window-ms", 400L);
        double oneWayDelay = Math.max(0.0D, attackerData.getLastTransactionRttNanos() / 2000000.0D);
        double jitterGrace = Math.min(80.0D, attackerData.getTransactionRttJitterNanos() / 1000000.0D);
        long dynamicWindow = Math.round(oneWayDelay + jitterGrace + 40.0D);
        if (dynamicWindow < 75L) {
            dynamicWindow = 75L;
        }
        return Math.min(configuredMax, dynamicWindow);
    }
}
