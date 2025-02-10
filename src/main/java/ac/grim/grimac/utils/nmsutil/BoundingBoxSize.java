package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySizeable;
import ac.grim.grimac.utils.data.packetentity.PacketEntityTrackXRot;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;

/**
 * Yeah, I know this is a bad class
 * I just can't figure out how to PR it to PacketEvents due to babies, slimes, and other irregularities
 * <p>
 * I could PR a ton of classes in order to accomplish it but then no one would use it
 * (And even if they did they would likely be breaking my license...)
 */
public final class BoundingBoxSize {

    public static float getWidth(GrimPlayer player, PacketEntity packetEntity) {
        // Turtles are the only baby animal that don't follow the * 0.5 rule
        if (packetEntity.getType() == EntityTypes.TURTLE && packetEntity.isBaby) return 0.36f;
        return getWidthMinusBaby(player, packetEntity) * (packetEntity.isBaby ? 0.5f : 1f);
    }

    private static float getWidthMinusBaby(GrimPlayer player, PacketEntity packetEntity) {
        final EntityType type = packetEntity.getType();
        if (EntityTypes.AXOLOTL.equals(type)) {
            return 0.75f;
        } else if (EntityTypes.PANDA.equals(type)) {
            return 1.3f;
        } else if (EntityTypes.BAT.equals(type) || EntityTypes.PARROT.equals(type) || EntityTypes.COD.equals(type) || EntityTypes.EVOKER_FANGS.equals(type) || EntityTypes.TROPICAL_FISH.equals(type) || EntityTypes.FROG.equals(type)) {
            return 0.5f;
        } else if (EntityTypes.ARMADILLO.equals(type) || EntityTypes.BEE.equals(type) || EntityTypes.PUFFERFISH.equals(type) || EntityTypes.SALMON.equals(type) || EntityTypes.SNOW_GOLEM.equals(type) || EntityTypes.CAVE_SPIDER.equals(type)) {
            return 0.7f;
        } else if (EntityTypes.WITHER_SKELETON.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 0.7f : 0.72f;
        } else if (EntityTypes.WITHER_SKULL.equals(type) || EntityTypes.SHULKER_BULLET.equals(type)) {
            return 0.3125f;
        } else if (EntityTypes.HOGLIN.equals(type) || EntityTypes.ZOGLIN.equals(type)) {
            return 1.3964844f;
        } else if (EntityTypes.SKELETON_HORSE.equals(type) || EntityTypes.ZOMBIE_HORSE.equals(type) || EntityTypes.HORSE.equals(type) ||EntityTypes.DONKEY.equals(type) || EntityTypes.MULE.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 1.3964844f : 1.4f;
        } else if (EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 1.375f : 1.5f;
        } else if (EntityTypes.CHICKEN.equals(type) || EntityTypes.ENDERMITE.equals(type) || EntityTypes.SILVERFISH.equals(type) || EntityTypes.VEX.equals(type) || EntityTypes.TADPOLE.equals(type)) {
            return 0.4f;
        } else if (EntityTypes.RABBIT.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 0.4f : 0.6f;
        } else if (EntityTypes.CREAKING.equals(type) || EntityTypes.STRIDER.equals(type) || EntityTypes.COW.equals(type) || EntityTypes.SHEEP.equals(type) || EntityTypes.MOOSHROOM.equals(type) || EntityTypes.PIG.equals(type) || EntityTypes.LLAMA.equals(type) || EntityTypes.DOLPHIN.equals(type) || EntityTypes.WITHER.equals(type) || EntityTypes.TRADER_LLAMA.equals(type) || EntityTypes.WARDEN.equals(type) || EntityTypes.GOAT.equals(type)) {
            return 0.9f;
        } else if (EntityTypes.PHANTOM.equals(type)) {
            if (packetEntity instanceof PacketEntitySizeable sizeable) {
                return 0.9f + sizeable.size * 0.2f;
            }

            return 1.5f;
        } else if (EntityTypes.ELDER_GUARDIAN.equals(type)) { // TODO: 2.35 * guardian?
            return 1.9975f;
        } else if (EntityTypes.END_CRYSTAL.equals(type)) {
            return 2f;
        } else if (EntityTypes.ENDER_DRAGON.equals(type)) {
            return 16f;
        } else if (EntityTypes.FIREBALL.equals(type)) {
            return 1f;
        } else if (EntityTypes.GHAST.equals(type)) {
            return 4f;
        } else if (EntityTypes.GIANT.equals(type)) {
            return 3.6f;
        } else if (EntityTypes.GUARDIAN.equals(type)) {
            return 0.85f;
        } else if (EntityTypes.IRON_GOLEM.equals(type)) {
            return 1.4f;
        } else if (EntityTypes.MAGMA_CUBE.equals(type)) {
            if (packetEntity instanceof PacketEntitySizeable sizeable) {
                float size = sizeable.size;
                return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)
                        ? 0.52f * size : player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                        ? 2.04f * (0.255f * size)
                        : 0.51000005f * size;
            }

            return 0.98f;
        } else if (EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT)) {
            return 0.98f;
        } else if (EntityTypes.PLAYER.equals(type)) {
            return 0.6f;
        } else if (EntityTypes.POLAR_BEAR.equals(type)) {
            return 1.4f;
        } else if (EntityTypes.RAVAGER.equals(type)) {
            return 1.95f;
        } else if (EntityTypes.SHULKER.equals(type)) {
            return 1f;
        } else if (EntityTypes.SLIME.equals(type)) {
            if (packetEntity instanceof PacketEntitySizeable sizeable) {
                float size = sizeable.size;
                return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)
                        ? 0.52f * size : player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                        ? 2.04f * (0.255f * size) : 0.51000005f * size;
            }

            return 0.3125f;
        } else if (EntityTypes.SMALL_FIREBALL.equals(type)) {
            return 0.3125f;
        } else if (EntityTypes.SPIDER.equals(type)) {
            return 1.4f;
        } else if (EntityTypes.SQUID.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 0.8f : 0.95f;
        } else if (EntityTypes.TURTLE.equals(type)) {
            return 1.2f;
        } else if (EntityTypes.ALLAY.equals(type)) {
            return 0.35f;
        } else if (EntityTypes.SNIFFER.equals(type)) {
            return 1.9f;
        } else if (EntityTypes.CAMEL.equals(type)) {
            return 1.7f;
        } else if (EntityTypes.WIND_CHARGE.equals(type)) {
            return 0.3125f;
        }
        return 0.6f;
    }

    public static Vector3d getRidingOffsetFromVehicle(PacketEntity entity, GrimPlayer player) {
        SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
        double x = (box.maxX + box.minX) / 2d;
        double y = box.minY;
        double z = (box.maxZ + box.minZ) / 2d;

        if (entity instanceof PacketEntityTrackXRot xRotEntity) {
            // Horses desync here, and we can't do anything about it without interpolating animations.
            // Mojang just has to fix it.  I'm not attempting to fix it.
            // Striders also do the same with animations, causing a desync.
            // At least the only people using buckets are people in boats for villager transportation
            // and people trying to false the anticheat.
            if (EntityTypes.isTypeInstanceOf(entity.getType(), EntityTypes.BOAT)) {
                float f = 0f;
                float f1 = (float) (getPassengerRidingOffset(player, entity) - 0.35f); // hardcoded player offset

                if (!entity.passengers.isEmpty()) {
                    int i = entity.passengers.indexOf(player.compensatedEntities.self);

                    if (i == 0) {
                        f = 0.2f;
                    } else if (i == 1) {
                        f = -0.6f;
                    }
                }

                Vector3d vec3 = new Vector3d(f, 0d, 0d);
                vec3 = yRot(GrimMath.radians(-xRotEntity.interpYaw) - ((float) Math.PI / 2f), vec3);
                return new Vector3d(x + vec3.x, y + (double) f1, z + vec3.z);
            } else if (entity.getType() == EntityTypes.LLAMA) {
                float f = player.trigHandler.cos(GrimMath.radians(xRotEntity.interpYaw));
                float f1 = player.trigHandler.sin(GrimMath.radians(xRotEntity.interpYaw));
                return new Vector3d(x + (double) (0.3f * f1), y + getPassengerRidingOffset(player, entity) - 0.35f, z + (double) (0.3f * f));
            } else if (entity.getType() == EntityTypes.CHICKEN) {
                float f = player.trigHandler.sin(GrimMath.radians(xRotEntity.interpYaw));
                float f1 = player.trigHandler.cos(GrimMath.radians(xRotEntity.interpYaw));
                y = y + (getHeight(player, entity) * 0.5f);
                return new Vector3d(x + (double) (0.1f * f), y - 0.35f, z - (double) (0.1f * f1));
            }
        }

        return new Vector3d(x, y + getPassengerRidingOffset(player, entity) - 0.35f, z);
    }

    private static Vector3d yRot(float yaw, Vector3d start) {
        double cos = (float) Math.cos(yaw);
        double sin = (float) Math.sin(yaw);
        return new Vector3d(
                start.x * cos + start.z * sin,
                start.y,
                start.z * cos - start.x * sin
        );
    }

    public static float getHeight(GrimPlayer player, PacketEntity packetEntity) {
        // Turtles are the only baby animal that don't follow the * 0.5 rule
        if (packetEntity.getType() == EntityTypes.TURTLE && packetEntity.isBaby) return 0.12f;
        return getHeightMinusBaby(player, packetEntity) * (packetEntity.isBaby ? 0.5f : 1f);
    }

    public static double getMyRidingOffset(PacketEntity packetEntity) {
        final EntityType type = packetEntity.getType();
        if (EntityTypes.PIGLIN.equals(type) || EntityTypes.ZOMBIFIED_PIGLIN.equals(type) || EntityTypes.ZOMBIE.equals(type)) {
            return packetEntity.isBaby ? -0.05 : -0.45;
        } else if (EntityTypes.SKELETON.equals(type)) {
            return -0.6;
        } else if (EntityTypes.ENDERMITE.equals(type) || EntityTypes.SILVERFISH.equals(type)) {
            return 0.1;
        } else if (EntityTypes.EVOKER.equals(type) || EntityTypes.ILLUSIONER.equals(type) || EntityTypes.PILLAGER.equals(type) || EntityTypes.RAVAGER.equals(type) || EntityTypes.VINDICATOR.equals(type) || EntityTypes.WITCH.equals(type)) {
            return -0.45;
        } else if (EntityTypes.PLAYER.equals(type)) {
            return -0.35;
        }

        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_ANIMAL)) {
            return 0.14;
        }

        return 0;
    }

    public static double getPassengerRidingOffset(GrimPlayer player, PacketEntity packetEntity) {
        if (packetEntity instanceof PacketEntityHorse)
            return (getHeight(player, packetEntity) * 0.75) - 0.25;

        final EntityType type = packetEntity.getType();
        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT)) {
            return 0;
        } else if (EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT)) {
            return -0.1;
        } else if (EntityTypes.HOGLIN.equals(type) || EntityTypes.ZOGLIN.equals(type)) {
            return getHeight(player, packetEntity) - (packetEntity.isBaby ? 0.2 : 0.15);
        } else if (EntityTypes.LLAMA.equals(type)) {
            return getHeight(player, packetEntity) * 0.67;
        } else if (EntityTypes.PIGLIN.equals(type)) {
            return getHeight(player, packetEntity) * 0.92;
        } else if (EntityTypes.RAVAGER.equals(type)) {
            return 2.1;
        } else if (EntityTypes.SKELETON.equals(type)) {
            return (getHeight(player, packetEntity) * 0.75) - 0.1875;
        } else if (EntityTypes.SPIDER.equals(type)) {
            return getHeight(player, packetEntity) * 0.5;
        } else if (EntityTypes.STRIDER.equals(type)) {// depends on animation position, good luck getting it exactly, this is the best you can do though
            return getHeight(player, packetEntity) - 0.19;
        }
        return getHeight(player, packetEntity) * 0.75;
    }

    private static float getHeightMinusBaby(GrimPlayer player, PacketEntity packetEntity) {
        final EntityType type = packetEntity.getType();
        if (EntityTypes.ARMADILLO.equals(type)) {
            return 0.65f;
        } else if (EntityTypes.AXOLOTL.equals(type)) {
            return 0.42f;
        } else if (EntityTypes.BEE.equals(type) || EntityTypes.DOLPHIN.equals(type) || EntityTypes.ALLAY.equals(type)) {
            return 0.6f;
        } else if (EntityTypes.EVOKER_FANGS.equals(type) || EntityTypes.VEX.equals(type)) {
            return 0.8f;
        } else if (EntityTypes.SQUID.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 0.8f : 0.95f;
        } else if (EntityTypes.PARROT.equals(type) || EntityTypes.BAT.equals(type) || EntityTypes.PIG.equals(type) || EntityTypes.SPIDER.equals(type)) {
            return 0.9f;
        } else if (EntityTypes.WITHER_SKULL.equals(type) || EntityTypes.SHULKER_BULLET.equals(type)) {
            return 0.3125f;
        } else if (EntityTypes.BLAZE.equals(type)) {
            return 1.8f;
        } else if (EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT)) {
            // WHY DOES VIAVERSION OFFSET BOATS? THIS MAKES IT HARD TO SUPPORT, EVEN IF WE INTERPOLATE RIGHT.
            // I gave up and just exempted boats from the reach check and gave up with interpolation for collisions
            return 0.5625f;
        } else if (EntityTypes.CAT.equals(type)) {
            return 0.7f;
        } else if (EntityTypes.CAVE_SPIDER.equals(type)) {
            return 0.5f;
        } else if (EntityTypes.FROG.equals(type)) {
            return 0.55f;
        } else if (EntityTypes.CHICKEN.equals(type)) {
            return 0.7f;
        } else if (EntityTypes.HOGLIN.equals(type) || EntityTypes.ZOGLIN.equals(type)) {
            return 1.4f;
        } else if (EntityTypes.COW.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 1.4f : 1.3f;
        } else if (EntityTypes.STRIDER.equals(type)) {
            return 1.7f;
        } else if (EntityTypes.CREEPER.equals(type)) {
            return 1.7f;
        } else if (EntityTypes.DONKEY.equals(type)) {
            return 1.5f;
        } else if (EntityTypes.ELDER_GUARDIAN.equals(type)) {
            return 1.9975f;
        } else if (EntityTypes.ENDERMAN.equals(type) || EntityTypes.WARDEN.equals(type)) {
            return 2.9f;
        } else if (EntityTypes.ENDERMITE.equals(type) || EntityTypes.COD.equals(type)) {
            return 0.3f;
        } else if (EntityTypes.END_CRYSTAL.equals(type)) {
            return 2f;
        } else if (EntityTypes.ENDER_DRAGON.equals(type)) {
            return 8f;
        } else if (EntityTypes.FIREBALL.equals(type)) {
            return 1f;
        } else if (EntityTypes.FOX.equals(type)) {
            return 0.7f;
        } else if (EntityTypes.GHAST.equals(type)) {
            return 4f;
        } else if (EntityTypes.GIANT.equals(type)) {
            return 12f;
        } else if (EntityTypes.GUARDIAN.equals(type)) {
            return 0.85f;
        } else if (EntityTypes.HORSE.equals(type)) {
            return 1.6f;
        } else if (EntityTypes.IRON_GOLEM.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 2.7f : 2.9f;
        } else if (EntityTypes.CREAKING.equals(type)) {
            return 2.7f;
        } else if (EntityTypes.LLAMA.equals(type) || EntityTypes.TRADER_LLAMA.equals(type)) {
            return 1.87f;
        } else if (EntityTypes.TROPICAL_FISH.equals(type)) {
            return 0.4f;
        } else if (EntityTypes.MAGMA_CUBE.equals(type)) {
            if (packetEntity instanceof PacketEntitySizeable sizeable) {
                float size = sizeable.size;
                return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)
                        ? 0.52f * size : player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                        ? 2.04f * (0.255f * size)
                        : 0.51000005f * size;
            }

            return 0.7f;
        } else if (EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT)) {
            return 0.7f;
        } else if (EntityTypes.MULE.equals(type)) {
            return 1.6f;
        } else if (EntityTypes.MOOSHROOM.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 1.4f : 1.3f;
        } else if (EntityTypes.OCELOT.equals(type)) {
            return 0.7f;
        } else if (EntityTypes.PANDA.equals(type)) {
            return 1.25f;
        } else if (EntityTypes.PHANTOM.equals(type)) {
            if (packetEntity instanceof PacketEntitySizeable sizeable) {
                return 0.5f + sizeable.size * 0.1f;
            }

            return 1.8f;
        } else if (EntityTypes.PLAYER.equals(type)) {
            return 1.8f;
        } else if (EntityTypes.POLAR_BEAR.equals(type)) {
            return 1.4f;
        } else if (EntityTypes.PUFFERFISH.equals(type)) {
            return 0.7f;
        } else if (EntityTypes.RABBIT.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 0.5f : 0.7f;
        } else if (EntityTypes.RAVAGER.equals(type)) {
            return 2.2f;
        } else if (EntityTypes.SALMON.equals(type)) {
            return 0.4f;
        } else if (EntityTypes.SHEEP.equals(type) || EntityTypes.GOAT.equals(type)) {
            return 1.3f;
        } else if (EntityTypes.SHULKER.equals(type)) { // Could maybe guess peek size, although seems useless
            return 2f;
        } else if (EntityTypes.SILVERFISH.equals(type)) {
            return 0.3f;
        } else if (EntityTypes.SKELETON.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 1.99f : 1.95f;
        } else if (EntityTypes.SKELETON_HORSE.equals(type)) {
            return 1.6f;
        } else if (EntityTypes.SLIME.equals(type)) {
            if (packetEntity instanceof PacketEntitySizeable sizeable) {
                float size = sizeable.size;
                return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)
                        ? 0.52f * size : player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                        ? 2.04f * (0.255f * size)
                        : 0.51000005f * size;
            }

            return 0.3125f;
        } else if (EntityTypes.SMALL_FIREBALL.equals(type)) {
            return 0.3125f;
        } else if (EntityTypes.SNOW_GOLEM.equals(type)) {
            return 1.9f;
        } else if (EntityTypes.STRAY.equals(type)) {
            return 1.99f;
        } else if (EntityTypes.TURTLE.equals(type)) {
            return 0.4f;
        } else if (EntityTypes.WITHER.equals(type)) {
            return 3.5f;
        } else if (EntityTypes.WITHER_SKELETON.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 2.4f : 2.535f;
        } else if (EntityTypes.WOLF.equals(type)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 0.85f : 0.8f;
        } else if (EntityTypes.ZOMBIE_HORSE.equals(type)) {
            return 1.6f;
        } else if (EntityTypes.TADPOLE.equals(type)) {
            return 0.3f;
        } else if (EntityTypes.SNIFFER.equals(type)) {
            return 1.75f;
        } else if (EntityTypes.CAMEL.equals(type)) {
            return 2.375f;
        } else if (EntityTypes.BREEZE.equals(type)) {
            return 1.77f;
        } else if (EntityTypes.BOGGED.equals(type)) {
            return 1.99f;
        } else if (EntityTypes.WIND_CHARGE.equals(type)) {
            return 0.3125f;
        }
        return 1.95f;
    }
}
