package ac.grim.grimac.platform.fabric.mc1194.entity;

import ac.grim.grimac.platform.fabric.mc1171.entity.Fabric1170GrimEntity;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.platform.fabric.world.FabricPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class Fabric1194GrimEntity extends Fabric1170GrimEntity {

    public Fabric1194GrimEntity(Entity entity) {
        super(entity);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return FabricFutureUtil.supplySync(() -> {
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
}
