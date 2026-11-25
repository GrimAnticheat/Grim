package ac.grim.grimac.platform.fabric.inject;

import java.util.UUID;

public interface FabricEntityHandle {
    UUID fabricEntityUuid();

    boolean fabricEjectPassengers();

    Object fabricWorld();

    double fabricPosX();

    double fabricPosY();

    double fabricPosZ();

    float fabricYaw(float partialTick);

    float fabricPitch(float partialTick);
}
