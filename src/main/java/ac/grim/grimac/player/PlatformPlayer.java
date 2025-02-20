package ac.grim.grimac.player;

import ac.grim.grimac.entity.GrimEntity;
import ac.grim.grimac.world.PlatformWorld;
import com.github.retrooper.packetevents.util.Vector3d;
import net.kyori.adventure.text.Component;


public interface PlatformPlayer extends GrimEntity {
    void kickPlayer(String textReason);

    boolean hasPermission(String s);

    void setSneaking(boolean b);

    boolean isSneaking();

    void sendMessage(String message);

    void sendMessage(Component message);

    boolean isOnline();

    PlatformWorld getWorld();

    String getName();

    void updateInventory();

    Vector3d getPosition();

    PlatformInventory getInventory();

    GrimEntity getVehicle();

    GameMode getGameMode();

    void setGameMode(GameMode gameMode);

    boolean isExternalPlayer();
}
