package ac.grim.grimac.platform.api.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.sender.Sender;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface PlatformPlayer extends GrimEntity {
    void kickPlayer(String textReason);

    boolean hasPermission(String s);

    boolean hasPermission(String s, boolean defaultIfUnset);

    boolean isSneaking();

    void setSneaking(boolean b);

    void sendMessage(String message);

    void sendMessage(Component message);

    boolean isOnline();

    String getName();

    void updateInventory();

    Vector3d getPosition();

    PlatformInventory getInventory();

    @Nullable GrimEntity getVehicle();

    GameMode getGameMode();

    void setGameMode(GameMode gameMode);

    boolean isExternalPlayer();

    void sendPluginMessage(String channelName, byte[] byteArray);

    Sender getSender();

    /*
     * Replaces native player reference in PlatformPlayer implementation with a new object
     * Vanilla MC replaces ServerPlayerEntity references on respawn and dimension change
     */
    default void replaceNativePlayer(Object nativePlayerObject) {}
}
