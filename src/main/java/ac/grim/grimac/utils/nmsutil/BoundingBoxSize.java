package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySizeable;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

/**
 * Yeah, I know this is a bad class
 * I just can't figure out how to PR it to PacketEvents due to babies, slimes, and other irregularities
 * <p>
 * I could PR a ton of classes in order to accomplish it but then no one would use it
 * (And even if they did they would likely be breaking my license...)
 */
public class BoundingBoxSize {
    public static double getWidth(PacketEntity packetEntity) {
        // Turtles are the only baby animal that don't follow the * 0.5 rule
        if (packetEntity.type == EntityTypes.TURTLE && packetEntity.isBaby) return 0.36;
        return getWidthMinusBaby(packetEntity) * (packetEntity.isBaby ? 0.5 : 1);
    }

    private static double getWidthMinusBaby(PacketEntity packetEntity) {
        if (EntityTypes.AXOLOTL.equals(packetEntity.type) || EntityTypes.PANDA.equals(packetEntity.type)) {
            return 1.3;
        } else if (EntityTypes.BAT.equals(packetEntity.type) || EntityTypes.PARROT.equals(packetEntity.type) || EntityTypes.COD.equals(packetEntity.type) || EntityTypes.EVOKER_FANGS.equals(packetEntity.type) || EntityTypes.TROPICAL_FISH.equals(packetEntity.type)) {
            return 0.5;
        } else if (EntityTypes.BEE.equals(packetEntity.type) || EntityTypes.PUFFERFISH.equals(packetEntity.type) || EntityTypes.SALMON.equals(packetEntity.type) || EntityTypes.SNOW_GOLEM.equals(packetEntity.type) || EntityTypes.WITHER_SKELETON.equals(packetEntity.type) || EntityTypes.CAVE_SPIDER.equals(packetEntity.type)) {
            return 0.7;
        } else if (EntityTypes.WITHER_SKULL.equals(packetEntity.type) || EntityTypes.SHULKER_BULLET.equals(packetEntity.type)) {
            return 0.3125;
        } else if (EntityTypes.BLAZE.equals(packetEntity.type) || EntityTypes.OCELOT.equals(packetEntity.type) || EntityTypes.STRAY.equals(packetEntity.type) || EntityTypes.HOGLIN.equals(packetEntity.type) || EntityTypes.SKELETON_HORSE.equals(packetEntity.type) || EntityTypes.MULE.equals(packetEntity.type) || EntityTypes.ZOMBIE_HORSE.equals(packetEntity.type) || EntityTypes.HORSE.equals(packetEntity.type) || EntityTypes.ZOGLIN.equals(packetEntity.type)) {
            return 1.39648;
        } else if (EntityTypes.BOAT.equals(packetEntity.type)) {
            return 1.375;
        } else if (EntityTypes.CHICKEN.equals(packetEntity.type) || EntityTypes.ENDERMITE.equals(packetEntity.type) || EntityTypes.RABBIT.equals(packetEntity.type) || EntityTypes.SILVERFISH.equals(packetEntity.type) || EntityTypes.VEX.equals(packetEntity.type)) {
            return 0.4;
        } else if (EntityTypes.STRIDER.equals(packetEntity.type) || EntityTypes.COW.equals(packetEntity.type) || EntityTypes.SHEEP.equals(packetEntity.type) || EntityTypes.MOOSHROOM.equals(packetEntity.type) || EntityTypes.PIG.equals(packetEntity.type) || EntityTypes.LLAMA.equals(packetEntity.type) || EntityTypes.DOLPHIN.equals(packetEntity.type) || EntityTypes.WITHER.equals(packetEntity.type) || EntityTypes.TRADER_LLAMA.equals(packetEntity.type)) {
            return 0.9;
        } else if (EntityTypes.PHANTOM.equals(packetEntity.type)) {
            if (packetEntity instanceof PacketEntitySizeable) {
                return 0.9 + ((PacketEntitySizeable) packetEntity).size * 0.2;
            }

            return 1.5;
        } else if (EntityTypes.DONKEY.equals(packetEntity.type)) {
            return 1.5;
        } else if (EntityTypes.ELDER_GUARDIAN.equals(packetEntity.type)) {
            return 1.9975;
        } else if (EntityTypes.END_CRYSTAL.equals(packetEntity.type)) {
            return 2.0;
        } else if (EntityTypes.ENDER_DRAGON.equals(packetEntity.type)) {
            return 16.0;
        } else if (EntityTypes.FIREBALL.equals(packetEntity.type)) {
            return 1;
        } else if (EntityTypes.GHAST.equals(packetEntity.type)) {
            return 4.0;
        } else if (EntityTypes.GIANT.equals(packetEntity.type)) {
            return 3.6;
        } else if (EntityTypes.GUARDIAN.equals(packetEntity.type)) {
            return 0.85;
        } else if (EntityTypes.IRON_GOLEM.equals(packetEntity.type)) {
            return 1.4;
        } else if (EntityTypes.MAGMA_CUBE.equals(packetEntity.type)) {
            if (packetEntity instanceof PacketEntitySizeable) {
                return 0.51000005 * ((PacketEntitySizeable) packetEntity).size;
            }

            return 0.98;
        } else if (EntityTypes.isTypeInstanceOf(packetEntity.type, EntityTypes.MINECART_ABSTRACT)) {
            return 0.98;
        } else if (EntityTypes.PLAYER.equals(packetEntity.type)) {
            return 0.6;
        } else if (EntityTypes.POLAR_BEAR.equals(packetEntity.type)) {
            return 1.4;
        } else if (EntityTypes.RAVAGER.equals(packetEntity.type)) {
            return 1.95;
        } else if (EntityTypes.SHULKER.equals(packetEntity.type)) {
            return 1.0;
        } else if (EntityTypes.SLIME.equals(packetEntity.type)) {
            if (packetEntity instanceof PacketEntitySizeable) {
                return 0.51000005 * ((PacketEntitySizeable) packetEntity).size;
            }

            return 0.3125;
        } else if (EntityTypes.SMALL_FIREBALL.equals(packetEntity.type)) {
            return 0.3125;
        } else if (EntityTypes.SPIDER.equals(packetEntity.type)) {
            return 1.4;
        } else if (EntityTypes.SQUID.equals(packetEntity.type)) {
            return 0.8;
        } else if (EntityTypes.TURTLE.equals(packetEntity.type)) {
            return 1.2;
        }
        return 0.6;
    }

    public static double getHeight(PacketEntity packetEntity) {
        // Turtles are the only baby animal that don't follow the * 0.5 rule
        if (packetEntity.type == EntityTypes.TURTLE && packetEntity.isBaby) return 0.12;
        return getHeightMinusBaby(packetEntity) * (packetEntity.isBaby ? 0.5 : 1);
    }

    public static double getMyRidingOffset(PacketEntity packetEntity) {
        if (EntityTypes.PIGLIN.equals(packetEntity.type) || EntityTypes.ZOMBIFIED_PIGLIN.equals(packetEntity.type) || EntityTypes.ZOMBIE.equals(packetEntity.type)) {
            return packetEntity.isBaby ? -0.05 : -0.45;
        } else if (EntityTypes.SKELETON.equals(packetEntity.type)) {
            return -0.6;
        } else if (EntityTypes.ENDERMITE.equals(packetEntity.type) || EntityTypes.SILVERFISH.equals(packetEntity.type)) {
            return 0.1;
        } else if (EntityTypes.EVOKER.equals(packetEntity.type) || EntityTypes.ILLUSIONER.equals(packetEntity.type) || EntityTypes.PILLAGER.equals(packetEntity.type) || EntityTypes.RAVAGER.equals(packetEntity.type) || EntityTypes.VINDICATOR.equals(packetEntity.type) || EntityTypes.WITCH.equals(packetEntity.type)) {
            return -0.45;
        } else if (EntityTypes.PLAYER.equals(packetEntity.type)) {
            return -0.35;
        }

        if (EntityTypes.isTypeInstanceOf(packetEntity.type, EntityTypes.ABSTRACT_ANIMAL)) {
            return 0.14;
        }

        return 0;
    }

    public static double getPassengerRidingOffset(PacketEntity packetEntity) {

        if (packetEntity instanceof PacketEntityHorse)
            return (getHeight(packetEntity) * 0.75) - 0.25;

        if (EntityTypes.isTypeInstanceOf(packetEntity.type, EntityTypes.MINECART_ABSTRACT)) {
            return 0;
        } else if (EntityTypes.BOAT.equals(packetEntity.type)) {
            return -0.1;
        } else if (EntityTypes.HOGLIN.equals(packetEntity.type) || EntityTypes.ZOGLIN.equals(packetEntity.type)) {
            return getHeight(packetEntity) - (packetEntity.isBaby ? 0.2 : 0.15);
        } else if (EntityTypes.LLAMA.equals(packetEntity.type)) {
            return getHeight(packetEntity) * 0.67;
        } else if (EntityTypes.PIGLIN.equals(packetEntity.type)) {
            return getHeight(packetEntity) * 0.92;
        } else if (EntityTypes.RAVAGER.equals(packetEntity.type)) {
            return 2.1;
        } else if (EntityTypes.SKELETON.equals(packetEntity.type)) {
            return (getHeight(packetEntity) * 0.75) - 0.1875;
        } else if (EntityTypes.SPIDER.equals(packetEntity.type)) {
            return getHeight(packetEntity) * 0.5;
        } else if (EntityTypes.STRIDER.equals(packetEntity.type)) {// depends on animation position, good luck getting it exactly, this is the best you can do though
            return getHeight(packetEntity) - 0.19;
        }
        return getHeight(packetEntity) * 0.75;
    }

    private static double getHeightMinusBaby(PacketEntity packetEntity) {
        if (EntityTypes.AXOLOTL.equals(packetEntity.type) || EntityTypes.BEE.equals(packetEntity.type) || EntityTypes.DOLPHIN.equals(packetEntity.type)) {
            return 0.6;
        } else if (EntityTypes.BAT.equals(packetEntity.type) || EntityTypes.PARROT.equals(packetEntity.type) || EntityTypes.PIG.equals(packetEntity.type) || EntityTypes.EVOKER_FANGS.equals(packetEntity.type) || EntityTypes.SQUID.equals(packetEntity.type) || EntityTypes.VEX.equals(packetEntity.type)) {
            return 0.8;
        } else if (EntityTypes.SPIDER.equals(packetEntity.type)) {
            return 0.9;
        } else if (EntityTypes.WITHER_SKULL.equals(packetEntity.type) || EntityTypes.SHULKER_BULLET.equals(packetEntity.type)) {
            return 0.3125;
        } else if (EntityTypes.BLAZE.equals(packetEntity.type)) {
            return 1.8;
        } else if (EntityTypes.BOAT.equals(packetEntity.type)) {
            return 0.5625;
        } else if (EntityTypes.CAT.equals(packetEntity.type)) {
            return 0.7;
        } else if (EntityTypes.CAVE_SPIDER.equals(packetEntity.type)) {
            return 0.5;
        } else if (EntityTypes.CHICKEN.equals(packetEntity.type)) {
            return 0.7;
        } else if (EntityTypes.HOGLIN.equals(packetEntity.type) || EntityTypes.ZOGLIN.equals(packetEntity.type) || EntityTypes.COD.equals(packetEntity.type)) {
            return 1.4;
        } else if (EntityTypes.COW.equals(packetEntity.type)) {
            return 1.7;
        } else if (EntityTypes.STRIDER.equals(packetEntity.type)) {
            return 1.7;
        } else if (EntityTypes.CREEPER.equals(packetEntity.type)) {
            return 1.7;
        } else if (EntityTypes.DONKEY.equals(packetEntity.type)) {
            return 1.39648;
        } else if (EntityTypes.ELDER_GUARDIAN.equals(packetEntity.type)) {
            return 1.9975;
        } else if (EntityTypes.ENDERMAN.equals(packetEntity.type)) {
            return 2.9;
        } else if (EntityTypes.ENDERMITE.equals(packetEntity.type)) {
            return 0.3;
        } else if (EntityTypes.END_CRYSTAL.equals(packetEntity.type)) {
            return 2.0;
        } else if (EntityTypes.ENDER_DRAGON.equals(packetEntity.type)) {
            return 8.0;
        } else if (EntityTypes.FIREBALL.equals(packetEntity.type)) {
            return 1;
        } else if (EntityTypes.FOX.equals(packetEntity.type)) {
            return 0.7;
        } else if (EntityTypes.GHAST.equals(packetEntity.type)) {
            return 4.0;
        } else if (EntityTypes.GIANT.equals(packetEntity.type)) {
            return 12.0;
        } else if (EntityTypes.GUARDIAN.equals(packetEntity.type)) {
            return 0.85;
        } else if (EntityTypes.HORSE.equals(packetEntity.type)) {
            return 1.6;
        } else if (EntityTypes.IRON_GOLEM.equals(packetEntity.type)) {
            return 2.7;
        } else if (EntityTypes.LLAMA.equals(packetEntity.type) || EntityTypes.TRADER_LLAMA.equals(packetEntity.type)) {
            return 1.87;
        } else if (EntityTypes.TROPICAL_FISH.equals(packetEntity.type)) {
            return 0.4;
        } else if (EntityTypes.MAGMA_CUBE.equals(packetEntity.type)) {
            if (packetEntity instanceof PacketEntitySizeable) {
                return 0.51000005 * ((PacketEntitySizeable) packetEntity).size;
            }

            return 0.7;
        } else if (EntityTypes.isTypeInstanceOf(packetEntity.type, EntityTypes.MINECART_ABSTRACT)) {
            return 0.7;
        } else if (EntityTypes.MULE.equals(packetEntity.type)) {
            return 1.6;
        } else if (EntityTypes.MOOSHROOM.equals(packetEntity.type)) {
            return 1.4;
        } else if (EntityTypes.OCELOT.equals(packetEntity.type)) {
            return 0.7;
        } else if (EntityTypes.PANDA.equals(packetEntity.type)) {
            return 1.25;
        } else if (EntityTypes.PHANTOM.equals(packetEntity.type)) {
            if (packetEntity instanceof PacketEntitySizeable) {
                return 0.5 + ((PacketEntitySizeable) packetEntity).size * 0.1;
            }

            return 1.8;
        } else if (EntityTypes.PLAYER.equals(packetEntity.type)) {
            return 1.8;
        } else if (EntityTypes.POLAR_BEAR.equals(packetEntity.type)) {
            return 1.4;
        } else if (EntityTypes.PUFFERFISH.equals(packetEntity.type)) {
            return 0.7;
        } else if (EntityTypes.RABBIT.equals(packetEntity.type)) {
            return 0.5;
        } else if (EntityTypes.RAVAGER.equals(packetEntity.type)) {
            return 2.2;
        } else if (EntityTypes.SALMON.equals(packetEntity.type)) {
            return 0.4;
        } else if (EntityTypes.SHEEP.equals(packetEntity.type)) {
            return 1.3;
        } else if (EntityTypes.SHULKER.equals(packetEntity.type)) { // Could maybe guess peek size, although seems useless
            return 1.0;
        } else if (EntityTypes.SILVERFISH.equals(packetEntity.type)) {
            return 0.3;
        } else if (EntityTypes.SKELETON.equals(packetEntity.type)) {
            return 1.99;
        } else if (EntityTypes.SKELETON_HORSE.equals(packetEntity.type)) {
            return 1.6;
        } else if (EntityTypes.SLIME.equals(packetEntity.type)) {
            if (packetEntity instanceof PacketEntitySizeable) {
                return 0.51000005 * ((PacketEntitySizeable) packetEntity).size;
            }

            return 0.3125;
        } else if (EntityTypes.SMALL_FIREBALL.equals(packetEntity.type)) {
            return 0.3125;
        } else if (EntityTypes.SNOW_GOLEM.equals(packetEntity.type)) {
            return 1.9;
        } else if (EntityTypes.STRAY.equals(packetEntity.type)) {
            return 1.99;
        } else if (EntityTypes.TURTLE.equals(packetEntity.type)) {
            return 0.4;
        } else if (EntityTypes.WITHER.equals(packetEntity.type)) {
            return 3.5;
        } else if (EntityTypes.WITHER_SKELETON.equals(packetEntity.type)) {
            return 2.4;
        } else if (EntityTypes.WOLF.equals(packetEntity.type)) {
            return 0.85;
        } else if (EntityTypes.ZOMBIE_HORSE.equals(packetEntity.type)) {
            return 1.6;
        }
        return 1.95;
    }
}
