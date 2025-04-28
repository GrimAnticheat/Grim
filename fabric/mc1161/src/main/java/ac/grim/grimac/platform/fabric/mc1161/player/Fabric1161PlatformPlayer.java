package ac.grim.grimac.platform.fabric.mc1161.player;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.platform.fabric.world.FabricPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.concurrent.CompletableFuture;

public class Fabric1161PlatformPlayer extends AbstractFabricPlatformPlayer {
    public Fabric1161PlatformPlayer(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    public boolean hasPermission(String permission) {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(entity.getCommandSource()).hasPermission(permission);
    }

    @Override
    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(entity.getCommandSource()).hasPermission(s, defaultIfUnset);
    }

    @Override
    public Sender getSender() {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(entity.getCommandSource());
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return FabricFutureUtil.supplySync(() -> {
            if (fabricPlayer.getEntityWorld() instanceof ServerWorld) {
                fabricPlayer.teleport(
                        ((FabricPlatformWorld) location.getWorld()).getFabricWorld(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        location.getYaw(),
                        location.getPitch()
                );
                return true;
            }
            return false;
        });
    }
}
