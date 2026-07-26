package ac.grim.grimac.platform.minestom.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.BlockTranslator;
import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.minestom.sender.MinestomSenderFactory;
import ac.grim.grimac.platform.minestom.world.MinestomPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Grim {@link PlatformPlayer} over a Minestom {@link Player}. Wraps position, inventory,
 * game-mode, packet/message sending, etc.
 * <p>
 * TODO Phase 4: {@link #hasPermission} returns {@code false} (no bypass, no alerts) until the
 * monorepo's group system is bridged. TODO Phase 3: {@link #getVehicle} returns {@code null}
 * (vehicle-mount checks) pending a generic {@code MinestomGrimEntity} wrapper.
 */
public final class MinestomPlatformPlayer implements PlatformPlayer {

    private static final MinestomSenderFactory SENDER_FACTORY = new MinestomSenderFactory();

    private final Player player;

    public MinestomPlatformPlayer(Player player) {
        this.player = player;
    }

    public Player getMinestomPlayer() {
        return player;
    }

    // --- GrimIdentity / OfflinePlatformPlayer ---

    @Override
    public UUID getUniqueId() {
        return player.getUuid();
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public boolean isOnline() {
        return player.isOnline();
    }

    // --- GrimEntity ---

    @Override
    public Object getNative() {
        return player;
    }

    @Override
    public boolean isDead() {
        return player.isDead();
    }

    @Override
    public PlatformWorld getWorld() {
        return new MinestomPlatformWorld(player.getInstance());
    }

    @Override
    public Location getLocation() {
        Pos pos = player.getPosition();
        return new Location(getWorld(), pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch());
    }

    @Override
    public double distanceSquared(double x, double y, double z) {
        Pos pos = player.getPosition();
        double dx = pos.x() - x;
        double dy = pos.y() - y;
        double dz = pos.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public boolean eject() {
        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);
        return !passengers.isEmpty();
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return player.teleport(new Pos(location.x(), location.y(), location.z()))
                .thenApply(ignored -> Boolean.TRUE);
    }

    // --- PlatformPlayer ---

    @Override
    public void kickPlayer(String textReason) {
        player.kick(textReason);
    }

    @Override
    public boolean isSneaking() {
        return player.isSneaking();
    }

    @Override
    public void setSneaking(boolean b) {
        player.setSneaking(b);
    }

    @Override
    public boolean hasPermission(String s) {
        return false; // TODO Phase 4: bridge to the monorepo group system
    }

    @Override
    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return defaultIfUnset;
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    @Override
    public void sendMessage(Component message) {
        player.sendMessage(message);
    }

    @Override
    public void updateInventory() {
        // Minestom keeps the client inventory synced automatically; nothing to force here.
    }

    @Override
    public Vector3d getPosition() {
        Pos pos = player.getPosition();
        return new Vector3d(pos.x(), pos.y(), pos.z());
    }

    @Override
    public PlatformInventory getInventory() {
        return new MinestomPlatformInventory(player);
    }

    @Override
    public @Nullable GrimEntity getVehicle() {
        return null; // TODO Phase 3: wrap player.getVehicle() in a MinestomGrimEntity
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.valueOf(player.getGameMode().name());
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        player.setGameMode(net.minestom.server.entity.GameMode.valueOf(gameMode.name()));
    }

    @Override
    public boolean isExternalPlayer() {
        return false;
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] byteArray) {
        player.sendPluginMessage(channelName, byteArray);
    }

    @Override
    public Sender getSender() {
        return SENDER_FACTORY.wrap(player);
    }

    @Override
    public BlockTranslator getBlockTranslator() {
        return MinestomBlockTranslator.INSTANCE;
    }
}
