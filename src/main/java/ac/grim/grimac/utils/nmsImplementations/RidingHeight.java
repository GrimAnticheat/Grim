package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import org.bukkit.entity.Entity;

public class RidingHeight {
    public static double getEntityRidingHeight(PacketEntity entity) {
        Entity bukkitEntity = entity.entity;

        switch (bukkitEntity.getType().toString().toUpperCase()) {
            case "DONKEY":
            case "HORSE":
            case "LLAMA":
            case "MULE":
            case "SKELETON_HORSE":
            case "ZOMBIE_HORSE":
            case "TRADER_LLAMA":
                return -0.25;
            case "MINECART":
                return 0;
            case "BOAT":
                return -0.1;

        }

        return -1;
    }
}
