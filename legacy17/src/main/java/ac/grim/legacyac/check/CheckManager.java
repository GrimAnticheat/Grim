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
import ac.grim.legacyac.check.impl.VelocityCheck;
import ac.grim.legacyac.combat.EntityIdIndex;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.InternalPacketEvent;
import ac.grim.legacyac.network.frame.MovementFrame;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
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
    private final List<VelocityCheck> velocityChecks = new ArrayList<VelocityCheck>();
    private final List<JesusCheck> jesusChecks = new ArrayList<JesusCheck>();
    private final List<FastPlaceCheck> fastPlaceChecks = new ArrayList<FastPlaceCheck>();
    private final List<FastBreakCheck> fastBreakChecks = new ArrayList<FastBreakCheck>();
    private final List<FastUseCheck> fastUseChecks = new ArrayList<FastUseCheck>();
    private final List<InventoryMoveCheck> inventoryMoveChecks = new ArrayList<InventoryMoveCheck>();
    private final List<PredictionMovementCheck> predictionChecks = new ArrayList<PredictionMovementCheck>();
    private final List<NoSlowCheck> noSlowChecks = new ArrayList<NoSlowCheck>();
    private long lastTickAtNanos;
    private double currentTps = 20.0D;

    public CheckManager(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.entityIdIndex = new EntityIdIndex(plugin.getLogger());
        speedChecks.add(new SpeedCheck(plugin));
        flyChecks.add(new FlyCheck(plugin));
        phaseChecks.add(new PhaseCheck(plugin));
        reachChecks.add(new ReachCheck(plugin));
        autoClickerChecks.add(new AutoClickerCheck(plugin));
        noFallChecks.add(new NoFallCheck(plugin));
        killAuraChecks.add(new KillAuraCheck(plugin));
        timerChecks.add(new TimerCheck(plugin));
        velocityChecks.add(new VelocityCheck(plugin));
        jesusChecks.add(new JesusCheck(plugin));
        fastPlaceChecks.add(new FastPlaceCheck(plugin));
        fastBreakChecks.add(new FastBreakCheck(plugin));
        fastUseChecks.add(new FastUseCheck(plugin));
        inventoryMoveChecks.add(new InventoryMoveCheck(plugin));
        predictionChecks.add(new PredictionMovementCheck(plugin));
        noSlowChecks.add(new NoSlowCheck(plugin));

        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                entityIdIndex.put(entity);
            }
        }
    }

    public int getCheckCount() {
        return speedChecks.size() + flyChecks.size() + phaseChecks.size() + reachChecks.size() + autoClickerChecks.size() + noFallChecks.size()
            + killAuraChecks.size() + timerChecks.size() + velocityChecks.size() + jesusChecks.size() + fastPlaceChecks.size() + fastBreakChecks.size()
            + fastUseChecks.size() + inventoryMoveChecks.size() + predictionChecks.size() + noSlowChecks.size();
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
        MovementFrame frame = new MovementFrame(now, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch(), player.isOnGround(), true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
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
                from = new Location(player.getWorld(), data.getLastFrameX(), data.getLastFrameY(), data.getLastFrameZ(), data.getLastFrameYaw(), data.getLastFramePitch());
            } else {
                from = player.getLocation().clone();
            }
        }

        Location to = explicitTo;
        if (to == null) {
            to = new Location(player.getWorld(), frame.getX(), frame.getY(), frame.getZ(), frame.getYaw(), frame.getPitch());
        }

        data.setMovementFrame(frame.getX(), frame.getY(), frame.getZ(), frame.getYaw(), frame.getPitch(), frame.getTimestampNanos());
        executeMovementPipeline(player, data, frame, from, to);
    }

    private void executeMovementPipeline(Player player, PlayerData data, MovementFrame frame, Location from, Location to) {
        data.handleMove(player, from, to, frame.isOnGround());
        data.setDetectionContext(frame.getSource().name(), data.getMoveWindow());

        PlayerData.MovementStateSnapshot snapshot = data.getMovementStateSnapshot();
        if (!snapshot.isTeleportAligned()) {
            if (data.isDebugEnabled()) {
                plugin.getLogger().info("[GLAC-DEBUG] " + player.getName() + " checks SKIPPED: teleport-not-aligned pending=" + snapshot.getPendingChanges());
            }
            return;
        }

        runPreChecks(player, frame, data);
        runPredictionChecks(player, frame, to, data);
        runPostChecks(player, frame, from, to, data);

        if (frame.isOnGround() && data.getLastDeltaXZ() < 0.35D && Math.abs(data.getLastDeltaY()) < 0.02D) {
            data.setLastSafeLocation(to.clone());
        }
    }


    private void runPreChecks(Player player, MovementFrame frame, PlayerData data) {
        runTimingChecks(player, frame, data);
        for (InventoryMoveCheck check : inventoryMoveChecks) {
            check.onMovementFrame(player, frame, data);
        }
        for (NoSlowCheck check : noSlowChecks) {
            check.onMovementFrame(player, frame, data);
        }
    }

    private void runTimingChecks(Player player, MovementFrame frame, PlayerData data) {
        for (TimerCheck check : timerChecks) {
            check.onMovementFrame(player, frame, data);
        }
    }

    private void runPredictionChecks(Player player, MovementFrame frame, Location to, PlayerData data) {
        for (PredictionMovementCheck check : predictionChecks) {
            check.onMovementFrame(player, frame, to, data);
        }
    }

    private void runPostChecks(Player player, MovementFrame frame, Location from, Location to, PlayerData data) {
        for (SpeedCheck check : speedChecks) {
            check.onMovementFrame(player, frame, from, to, data);
        }
        for (FlyCheck check : flyChecks) {
            check.onMovementFrame(player, frame, to, data);
        }
        for (PhaseCheck check : phaseChecks) {
            check.onMovementFrame(player, frame, to, data);
        }
        for (VelocityCheck check : velocityChecks) {
            check.onMovementFrame(player, frame, data);
        }
        for (JesusCheck check : jesusChecks) {
            check.onMovementFrame(player, frame, data);
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

    public void onInternalPacketEvent(InternalPacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        PlayerData data = plugin.getPlayerData(player);

        if (event.getType() == InternalPacketEvent.Type.CLIENT_MOVEMENT) {
            data.setLastRawMovementPacketAt(event.getCreatedAtNanos());
            data.incrementRawMovementPacketCounter();
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
            if (event.isAttackAction() && event.getEntityId() != null) {
                onUseEntityAttackPacket(player, event.getEntityId().intValue());
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.SERVER_ENTITY_VELOCITY) {
            Integer entityId = event.getEntityId();
            if (entityId == null || entityId.intValue() != player.getEntityId()) {
                return;
            }
            Integer vx = event.getVelocityX();
            Integer vy = event.getVelocityY();
            Integer vz = event.getVelocityZ();
            if (vx == null || vy == null || vz == null) {
                return;
            }
            final double dx = vx.intValue() / 8000.0D;
            final double dy = vy.intValue() / 8000.0D;
            final double dz = vz.intValue() / 8000.0D;
            final Player scheduled = player;
            plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (!scheduled.isOnline()) {
                        return;
                    }
                    org.bukkit.util.Vector vector = new org.bukkit.util.Vector(dx, dy, dz);
                    org.bukkit.event.player.PlayerVelocityEvent bukkitEvent = new org.bukkit.event.player.PlayerVelocityEvent(scheduled, vector);
                    onVelocity(bukkitEvent);
                }
            });
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
        PlayerData targetData = plugin.getPlayerData(target);
        double[] targetBox = plugin.resolveEntityBox(target);
        Location targetLoc = target.getLocation();
        targetData.recordCurrentHitbox(targetLoc.getX(), targetLoc.getY(), targetLoc.getZ(), targetBox[0], targetBox[1]);
        final long backtrackWindow = plugin.getConfig().getLong("combat.backtrack-window-ms", 400L);
        final ReachCheck.AttackEvaluation reachEval;
        if (reachChecks.isEmpty()) {
            reachEval = new ReachCheck.AttackEvaluation(true, 0.0D, 0L);
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
    public void onClick(PlayerInteractEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        for (AutoClickerCheck check : autoClickerChecks) {
            check.onInteract(event, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        data.recordPendingBlockChange("place:" + event.getBlockPlaced().getType().name());
        for (FastPlaceCheck check : fastPlaceChecks) {
            check.onPlace(event, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        data.recordPendingBlockChange("break:" + event.getBlock().getType().name());
        for (FastBreakCheck check : fastBreakChecks) {
            check.onBreak(event, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        for (FastUseCheck check : fastUseChecks) {
            check.onConsume(event, data);
        }
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
        for (NoFallCheck check : noFallChecks) {
            check.onFallDamage(event, player, data);
        }
    }
}
