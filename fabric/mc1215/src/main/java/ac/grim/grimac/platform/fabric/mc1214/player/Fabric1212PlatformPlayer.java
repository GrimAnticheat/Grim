package ac.grim.grimac.platform.fabric.mc1214.player;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1205.player.Fabric1202PlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.platform.fabric.world.FabricPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class Fabric1212PlatformPlayer extends Fabric1202PlatformPlayer {
    public Fabric1212PlatformPlayer(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    public boolean hasPermission(String permission) {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(fabricPlayer.getCommandSource()).hasPermission(permission);
    }

    @Override
    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(fabricPlayer.getCommandSource()).hasPermission(s, defaultIfUnset);
    }

    @Override
    public Sender getSender() {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(fabricPlayer.getCommandSource());
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
                        EnumSet.noneOf(PositionFlag.class), // todo change to match paper? Do they do this?
                        location.getYaw(),
                        location.getPitch(),
                        true
                );
                return true;
            }
            return false;
        });
    }
}
