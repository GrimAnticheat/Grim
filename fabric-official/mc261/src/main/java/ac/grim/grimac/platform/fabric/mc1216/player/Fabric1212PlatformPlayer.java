package ac.grim.grimac.platform.fabric.mc1216.player;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1205.player.Fabric1202PlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.utils.math.Location;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

public class Fabric1212PlatformPlayer extends Fabric1202PlatformPlayer {
    public Fabric1212PlatformPlayer(ServerPlayer player) {
        super(player);
    }

    @Override
    public Sender getSender() {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().wrap(fabricPlayer.createCommandSourceStack());
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        PlatformWorld world = location.getWorld();
        if (world == null || !(world instanceof ServerLevel targetLevel)) {
            return CompletableFuture.completedFuture(false);
        }
        return FabricFutureUtil.supplySync(
                () -> {
                    fabricPlayer.teleportTo(
                            targetLevel,
                            location.getX(),
                            location.getY(),
                            location.getZ(),
                            EnumSet.noneOf(Relative.class),
                            location.getYaw(),
                            location.getPitch(),
                            true);
                    return true;
                });
    }
}
