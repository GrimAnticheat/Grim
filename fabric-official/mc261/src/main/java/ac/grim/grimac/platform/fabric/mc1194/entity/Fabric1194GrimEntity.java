package ac.grim.grimac.platform.fabric.mc1194.entity;

import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.fabric.mc1171.entity.Fabric1170GrimEntity;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.utils.anticheat.LogUtil;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;

public class Fabric1194GrimEntity extends Fabric1170GrimEntity {

    public Fabric1194GrimEntity(Entity entity) {
        super(entity);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return FabricFutureUtil.supplySync(() -> {
            if (!(entity.level() instanceof ServerLevel)) {
                return false;
            }
            PlatformWorld world = location.getWorld();
            if (world == null || !(world instanceof ServerLevel targetLevel)) {
                LogUtil.info("teleportAsync skipped: location world missing or not a ServerLevel");
                return false;
            }
            entity.teleportTo(
                    targetLevel,
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    EnumSet.noneOf(Relative.class),
                    location.getYaw(),
                    location.getPitch(),
                    false
            );
            return true;
        });
    }
}
