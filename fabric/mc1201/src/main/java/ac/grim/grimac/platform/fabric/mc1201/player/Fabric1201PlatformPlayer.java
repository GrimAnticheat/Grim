package ac.grim.grimac.platform.fabric.mc1201.player;

import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import ac.grim.grimac.platform.fabric.world.FabricPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import net.kyori.adventure.text.Component;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class Fabric1201PlatformPlayer extends FabricPlatformPlayer implements PlatformPlayer {

    public Fabric1201PlatformPlayer(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    public boolean hasPermission(String permission) {
        return GrimACFabricLoaderPlugin.PLUGIN.getFabricSenderFactory().map(fabricPlayer.getCommandSource()).hasPermission(permission);
    }

    @Override
    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return GrimACFabricLoaderPlugin.PLUGIN.getFabricSenderFactory().map(fabricPlayer.getCommandSource()).hasPermission(s, defaultIfUnset);
    }

    @Override
    public void sendMessage(String message) {
        fabricPlayer.sendMessage(Text.literal(message));
    }

    @Override
    public void sendMessage(Component message) {
        fabricPlayer.sendMessage(FabricConversionUtil.toNativeText(message));
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return CompletableFuture.supplyAsync(() -> {
            if (entity.getWorld() instanceof ServerWorld) {
                entity.teleport(
                        ((FabricPlatformWorld) location.getWorld()).getFabricWorld(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        EnumSet.noneOf(PositionFlag.class), // todo change to match paper? Do they do this?
                        location.getYaw(),
                        location.getPitch()

                );
                return true;
            }
            return false;
        });
    }

    @Override
    public Sender getSender() {
        return GrimACFabricLoaderPlugin.PLUGIN.getFabricSenderFactory().map(fabricPlayer.getCommandSource());
    }

}
