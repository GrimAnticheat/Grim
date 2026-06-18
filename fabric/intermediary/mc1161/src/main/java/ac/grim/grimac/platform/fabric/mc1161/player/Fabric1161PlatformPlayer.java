package ac.grim.grimac.platform.fabric.mc1161.player;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricIntermediaryLoaderPlugin;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.convert.FabricIntermediaryConversionUtil;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.utils.math.Location;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class Fabric1161PlatformPlayer extends AbstractFabricPlatformPlayer<ServerPlayer> {
    public Fabric1161PlatformPlayer(ServerPlayer player) {
        super(player);
    }

    @Override
    public Sender getSender() {
        return GrimACFabricIntermediaryLoaderPlugin.LOADER.getFabricSenderFactory().wrap(serverPlayer().createCommandSourceStack());
    }

    @Override
    public void kickPlayer(String textReason) {
        serverPlayer().connection.disconnect((Component) GrimACFabricIntermediaryLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(textReason));
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        serverPlayer().setGameMode(FabricIntermediaryConversionUtil.toFabricGameMode(gameMode));
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return FabricFutureUtil.supplySync(() -> {
            serverPlayer().teleportTo(
                    (ServerLevel) location.getWorld(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch()
            );
            return true;
        });
    }
}
