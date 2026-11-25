package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.fabric.inject.FabricEntityHandle;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(Entity.class)
@Implements(@Interface(iface = FabricEntityHandle.class, prefix = "grim$"))
abstract class FabricIntermediaryEntityMixin {

    public UUID grim$fabricEntityUuid() {
        return ((Entity) (Object) this).getUUID();
    }

    public boolean grim$fabricEjectPassengers() {
        Entity entity = (Entity) (Object) this;
        if (entity.isVehicle()) {
            entity.ejectPassengers();
            return true;
        }
        return false;
    }

    public Object grim$fabricWorld() {
        return ((Entity) (Object) this).level;
    }

    public double grim$fabricPosX() {
        return ((Entity) (Object) this).getX();
    }

    public double grim$fabricPosY() {
        return ((Entity) (Object) this).getY();
    }

    public double grim$fabricPosZ() {
        return ((Entity) (Object) this).getZ();
    }

    public float grim$fabricYaw(float partialTick) {
        return ((Entity) (Object) this).getViewYRot(partialTick);
    }

    public float grim$fabricPitch(float partialTick) {
        return ((Entity) (Object) this).getViewXRot(partialTick);
    }
}
