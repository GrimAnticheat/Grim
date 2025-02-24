package ac.grim.grimac.platform.bukkit.player;

import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.platform.bukkit.entity.BukkitGrimEntity;
import ac.grim.grimac.platform.bukkit.utils.anticheat.MultiLibUtil;
import ac.grim.grimac.platform.bukkit.utils.convert.BukkitConversionUtils;
import ac.grim.grimac.platform.bukkit.utils.reflection.PaperUtils;
import ac.grim.grimac.platform.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.sender.Sender;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitPlatformPlayer extends BukkitGrimEntity implements PlatformPlayer {
    private static final BukkitAudiences audiences = BukkitAudiences.create(GrimACBukkitLoaderPlugin.PLUGIN);

    @Getter
    private final Player bukkitPlayer;
    private final PlatformInventory inventory;
    private BukkitPlatformWorld bukkitPlatformWorld;

    public BukkitPlatformPlayer(Player bukkitPlayer) {
        super(bukkitPlayer);
        this.bukkitPlayer = bukkitPlayer;
        this.inventory = new BukkitPlatformInventory(bukkitPlayer);
    }

    @Override
    public void kickPlayer(String textReason) {
        bukkitPlayer.kickPlayer(textReason);
    }

    @Override
    public boolean hasPermission(String s) {
        return bukkitPlayer.hasPermission(s);
    }

    @Override
    public void setSneaking(boolean isSneaking) {
        bukkitPlayer.setSneaking(isSneaking);
    }

    @Override
    public boolean isSneaking() {
        return bukkitPlayer.isSneaking();
    }

    @Override
    public void sendMessage(String message) {
        bukkitPlayer.sendMessage(message);
    }

    @Override
    public void sendMessage(Component message) {
        audiences.player(bukkitPlayer).sendMessage(message);
    }

    @Override
    public boolean isOnline() {
        return bukkitPlayer.isOnline();
    }

    // TODO replace with PlayerWorldChangeEvent listener instead of checking for equality for better performance
    @Override
    public PlatformWorld getWorld() {
        if (bukkitPlatformWorld == null || !bukkitPlatformWorld.getBukkitWorld().equals(bukkitPlayer.getWorld())) {
            bukkitPlatformWorld = new BukkitPlatformWorld(bukkitPlayer.getWorld());
        }

        return bukkitPlatformWorld;
    }

    @Override
    public String getName() {
        return bukkitPlayer.getName();
    }

    @Override
    public void updateInventory() {
        bukkitPlayer.updateInventory();
    }

    @Override
    public Vector3d getPosition() {
        org.bukkit.Location location = this.bukkitPlayer.getLocation();
        return new Vector3d(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public PlatformInventory getInventory() {
        return inventory;
    }

    @Override
    public GrimEntity getVehicle() {
        return new BukkitGrimEntity(bukkitPlayer.getVehicle());
    }

    @Override
    public GameMode getGameMode() {
        return BukkitConversionUtils.fromBukkitGameMode(bukkitPlayer.getGameMode());
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        bukkitPlayer.setGameMode(BukkitConversionUtils.toBukkitGameMode(gameMode));
    }

    public World getBukkitWorld() {
        return bukkitPlayer.getWorld();
    }

    @Override
    public UUID getUniqueId() {
        return bukkitPlayer.getUniqueId();
    }

    @Override
    public boolean eject() {
        return bukkitPlayer.eject();
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        org.bukkit.Location bLoc = BukkitConversionUtils.toBukkitLocation(location);
        return PaperUtils.teleportAsync(this.bukkitPlayer, bLoc);
    }

    @Override
    public boolean isExternalPlayer() {
        return MultiLibUtil.isExternalPlayer(this.bukkitPlayer);
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] byteArray) {
        this.bukkitPlayer.sendPluginMessage(GrimACBukkitLoaderPlugin.PLUGIN, channelName, byteArray);
    }

    @Override
    public Sender getSender() {
        return GrimACBukkitLoaderPlugin.PLUGIN.getBukkitSenderFactory().map(this.bukkitPlayer);
    }
}
