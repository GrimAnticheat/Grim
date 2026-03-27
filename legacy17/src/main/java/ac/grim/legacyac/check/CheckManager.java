package ac.grim.legacyac.check;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.impl.AutoClickerCheck;
import ac.grim.legacyac.check.impl.FastBreakCheck;
import ac.grim.legacyac.check.impl.FastPlaceCheck;
import ac.grim.legacyac.check.impl.FastUseCheck;
import ac.grim.legacyac.check.impl.FlyCheck;
import ac.grim.legacyac.check.impl.GroundSpoofCheck;
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
import ac.grim.legacyac.combat.EntityIdIndex;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.InternalPacketEvent;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
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
    private final ToleranceBudgetEngine.ConfigProvider budgetConfigProvider;
    private final CombatPipeline combatPipeline;
    private final MovementPipeline movementPipeline;
    private final PacketIntakeCoordinator packetIntakeCoordinator;
    private long lastTickAtNanos;
    private double currentTps = 20.0D;

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

        this.combatPipeline = new CombatPipeline(plugin, entityIdIndex, reachChecks, killAuraChecks);
        this.movementPipeline = new MovementPipeline(
                plugin,
                budgetConfigProvider,
                new DoubleSupplier() {
                    @Override
                    public double getAsDouble() {
                        return currentTps;
                    }
                },
                combatPipeline,
                timerChecks,
                inventoryMoveChecks,
                airLiquidPlaceChecks,
                farPlaceChecks,
                rotationPlaceChecks,
                multiPlaceChecks,
                positionPlaceChecks,
                duplicateRotPlaceChecks,
                fabricatedPlaceChecks,
                airLiquidBreakChecks,
                farBreakChecks,
                rotationBreakChecks,
                multiBreakChecks,
                predictionChecks,
                aimProcessorChecks,
                aimModulo360Checks,
                aimDuplicateLookChecks,
                noSlowChecks,
                speedChecks,
                flyChecks,
                phaseChecks,
                jesusChecks,
                velocityChecks,
                groundSpoofChecks);
        this.packetIntakeCoordinator = new PacketIntakeCoordinator(
                plugin,
                velocityChecks,
                badPacketsAChecks,
                badPacketsCChecks,
                badPacketsDChecks,
                badPacketsEChecks,
                badPacketsFChecks,
                badPacketsGChecks,
                badPacketsIChecks,
                badPacketsLChecks,
                badPacketsQChecks,
                crashAChecks);

        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                entityIdIndex.put(entity);
            }
        }
    }

    public int getCheckCount() {
        return speedChecks.size() + flyChecks.size() + phaseChecks.size() + reachChecks.size()
                + autoClickerChecks.size() + noFallChecks.size()
                + killAuraChecks.size() + timerChecks.size() + jesusChecks.size()
                + fastPlaceChecks.size() + fastBreakChecks.size()
                + fastUseChecks.size() + inventoryMoveChecks.size() + predictionChecks.size() + noSlowChecks.size()
                + velocityChecks.size() + groundSpoofChecks.size()
                + aimProcessorChecks.size() + aimModulo360Checks.size() + aimDuplicateLookChecks.size()
                + duplicateRotPlaceChecks.size() + fabricatedPlaceChecks.size()
                + airLiquidBreakChecks.size() + farBreakChecks.size() + rotationBreakChecks.size()
                + multiBreakChecks.size()
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

    public String describeMovementExecutionPath() {
        boolean packetFirst = plugin.getConfig().getBoolean("pipeline.packet-first", true);
        boolean packetActive = plugin.isPacketPipelineActive();
        boolean bukkitFallback = plugin.getConfig().getBoolean("pipeline.bukkit-fallback", true);
        long staleThresholdNanos = plugin.getConfig().getLong("pipeline.bukkit-fallback-stale-nanos", 150000000L);

        StringBuilder sb = new StringBuilder();
        if (packetFirst && packetActive) {
            sb.append("packet-first -> MovementPipeline");
            sb.append(bukkitFallback ? " -> stale Bukkit fallback enabled" : " -> no Bukkit fallback");
        } else {
            sb.append("Bukkit move event -> MovementPipeline");
        }
        sb.append(" [stale=").append(staleThresholdNanos).append("ns");
        sb.append(", legacyFallback=")
                .append(plugin.getConfig().getBoolean("pipeline.legacy-onmove-fallback", true));
        sb.append(", minimalPost=")
                .append(plugin.getConfig().getBoolean("pipeline.minimal-post-on-prediction-miss", true));
        sb.append(']');
        return sb.toString();
    }

    public String describeMovementPipelineTopology() {
        return "CheckManager -> PacketIntakeCoordinator -> MovementPipeline -> CombatPipeline";
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
        movementPipeline.consumeMovementFrame(player, frame, event.getFrom(), to);
    }

    public void onMovementFrame(Player player, MovementFrame frame) {
        movementPipeline.consumeMovementFrame(player, frame, null, null);
    }

    public void onInternalPacketEvent(InternalPacketEvent event) {
        packetIntakeCoordinator.onInternalPacketEvent(event);
    }

    public void onUseEntityAttackPacket(Player attacker, int targetEntityId) {
        combatPipeline.onUseEntityAttackPacket(attacker, targetEntityId, null);
    }

    public void onUseEntityAttackPacket(Player attacker, int targetEntityId, PlayerData.QueuedAttackSnapshot snapshot) {
        combatPipeline.onUseEntityAttackPacket(attacker, targetEntityId, snapshot);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (plugin.isCombatPacketPipelineActive()) {
            return;
        }
        combatPipeline.onAttackFallback(event);
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
        data.recordPlacedBlock(event.getBlockPlaced().getX(), event.getBlockPlaced().getY(), event.getBlockPlaced().getZ());
        if (!plugin.isWorldPacketPipelineActive()) {
            data.queueCompensatedBlockChange(event.getPlayer(), event.getBlockPlaced().getX(),
                    event.getBlockPlaced().getY(), event.getBlockPlaced().getZ(),
                    event.getBlockPlaced().getType(), event.getBlockPlaced().getData(), "event:block-place");
        }
        for (FastPlaceCheck check : fastPlaceChecks) {
            check.onPlace(event, data);
        }
        if (plugin.isPlacePacketPipelineActive()) {
            return;
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
        if (!plugin.isWorldPacketPipelineActive()) {
            data.queueCompensatedBlockChange(event.getPlayer(), event.getBlock().getX(), event.getBlock().getY(),
                    event.getBlock().getZ(), Material.AIR, (byte) 0, "event:block-break");
        }
        for (FastBreakCheck check : fastBreakChecks) {
            check.onBreak(event, data);
        }
        if (plugin.isPlacePacketPipelineActive()) {
            return;
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
        org.bukkit.util.Vector vel = event.getVelocity();
        double xzMagnitude = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
        data.setLastVelocityXZ(xzMagnitude);
        if (plugin.isCombatPacketPipelineActive()) {
            return;
        }
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
