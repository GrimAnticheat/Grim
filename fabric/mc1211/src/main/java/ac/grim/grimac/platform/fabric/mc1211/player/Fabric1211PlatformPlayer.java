package ac.grim.grimac.platform.fabric.mc1211.player;

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

public class Fabric1211PlatformPlayer extends FabricPlatformPlayer {
    public Fabric1211PlatformPlayer(ServerPlayerEntity player) {
        super(player);
    }

    // Though the source code for the methods is the same, in 1.21.4 getCommandSource() is a ServerPlayerEntity method
    // so the compiled bytecode is not compatible with 1.21.1, where getCommandSource() is an Entity method
    @Override
    public boolean hasPermission(String permission) {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(fabricPlayer.getCommandSource()).hasPermission(permission);
    }

    @Override
    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(fabricPlayer.getCommandSource()).hasPermission(s, defaultIfUnset);
    }

    // In 1.21.4 these ServerPlayerEntity.sendMessage() does not override anything
    // in 1.21.1 it Overrides PlayerEntity -> LivingEntity -> Entity -> CommandOutput
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
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(fabricPlayer.getCommandSource());
    }
}
