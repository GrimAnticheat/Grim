package ac.grim.bukkit.player;

import ac.grim.bukkit.entity.BukkitGrimEntity;
import ac.grim.bukkit.utils.anticheat.MultiLibUtil;
import ac.grim.bukkit.utils.convert.ConvertLocation;
import ac.grim.bukkit.utils.reflection.PaperUtils;
import ac.grim.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitPlatformPlayer implements PlatformPlayer {

    @Getter
    private final Player bukkitPlayer;
    private final PlatformInventory inventory;
    private BukkitPlatformWorld bukkitPlatformWorld;

    public BukkitPlatformPlayer(Player bukkitPlayer) {
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
        bukkitPlayer.sendMessage(message);
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
        return new Vector3d(this.bukkitPlayer.getX(), this.bukkitPlayer.getY(), this.bukkitPlayer.getZ());
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
        GameMode gameMode;
        switch (bukkitPlayer.getGameMode()) {
            case CREATIVE:
                gameMode = GameMode.CREATIVE;
                break;
            case SURVIVAL:
                gameMode = GameMode.SURVIVAL;
                break;
            case ADVENTURE:
                gameMode = GameMode.ADVENTURE;
                break;
            case SPECTATOR:
                gameMode = GameMode.SPECTATOR;
                break;
            default:
                throw new IllegalStateException();
        }
        return gameMode;
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        org.bukkit.GameMode bukkitGameMode;
        switch (gameMode) {
            case CREATIVE:
                bukkitGameMode = org.bukkit.GameMode.CREATIVE;
                break;
            case SURVIVAL:
                bukkitGameMode = org.bukkit.GameMode.SURVIVAL;
                break;
            case ADVENTURE:
                bukkitGameMode = org.bukkit.GameMode.ADVENTURE;
                break;
            case SPECTATOR:
                bukkitGameMode = org.bukkit.GameMode.SPECTATOR;
                break;
            default:
                throw new IllegalStateException();
        }
        bukkitPlayer.setGameMode(bukkitGameMode);
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
        org.bukkit.Location bLoc = ConvertLocation.toBukkit(location);
        return PaperUtils.teleportAsync(this.bukkitPlayer, bLoc);
    }

    @Override
    public boolean isExternalPlayer() {
        return MultiLibUtil.isExternalPlayer(this.bukkitPlayer);
    }
}
