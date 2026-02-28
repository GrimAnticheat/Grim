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
import ac.grim.legacyac.check.impl.PhaseCheck;
import ac.grim.legacyac.check.impl.PredictionMovementCheck;
import ac.grim.legacyac.check.impl.ReachCheck;
import ac.grim.legacyac.check.impl.SpeedCheck;
import ac.grim.legacyac.check.impl.TimerCheck;
import ac.grim.legacyac.check.impl.VelocityCheck;
import ac.grim.legacyac.data.PlayerData;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public final class CheckManager implements Listener {
    private final LegacyAntiCheatPlugin plugin;
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

    public CheckManager(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
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
    }

    public int getCheckCount() {
        return speedChecks.size() + flyChecks.size() + phaseChecks.size() + reachChecks.size() + autoClickerChecks.size() + noFallChecks.size()
            + killAuraChecks.size() + timerChecks.size() + velocityChecks.size() + jesusChecks.size() + fastPlaceChecks.size() + fastBreakChecks.size()
            + fastUseChecks.size() + inventoryMoveChecks.size() + predictionChecks.size();
    }

    public void tick() {
        double decay = plugin.getConfig().getDouble("violation-decay-per-second", 0.08D);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.getPlayerData(player).decayViolations(decay);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
        data.setJoinAt(System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.removePlayerData(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerData(player);
        data.handleMove(event.getFrom(), to, player.isOnGround());

        for (SpeedCheck check : speedChecks) {
            check.onMove(event, data);
        }
        for (FlyCheck check : flyChecks) {
            check.onMove(event, data);
        }
        for (PhaseCheck check : phaseChecks) {
            check.onMove(event, data);
        }
        for (TimerCheck check : timerChecks) {
            check.onMove(event, data);
        }
        for (VelocityCheck check : velocityChecks) {
            check.onMove(event, data);
        }
        for (JesusCheck check : jesusChecks) {
            check.onMove(event, data);
        }
        for (InventoryMoveCheck check : inventoryMoveChecks) {
            check.onMove(event, data);
        }
        for (PredictionMovementCheck check : predictionChecks) {
            check.onMove(event, data);
        }

        if (player.isOnGround() && data.getLastDeltaXZ() < 0.35D && Math.abs(data.getLastDeltaY()) < 0.02D) {
            data.setLastSafeLocation(to.clone());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        PlayerData data = plugin.getPlayerData(attacker);
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
        for (FastPlaceCheck check : fastPlaceChecks) {
            check.onPlace(event, data);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer());
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
