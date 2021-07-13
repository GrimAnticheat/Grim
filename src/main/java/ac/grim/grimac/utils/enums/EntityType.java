package ac.grim.grimac.utils.enums;

import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;

public enum EntityType {
    AREA_EFFECT_CLOUD,
    ARMOR_STAND,
    ARROW,
    AXOLOTL,
    BAT,
    BEE,
    BLAZE,
    BOAT,
    CAT,
    CAVE_SPIDER,
    CHICKEN,
    COD,
    COW,
    CREEPER,
    DOLPHIN,
    DONKEY,
    DRAGON_FIREBALL,
    DROPPED_ITEM,
    DROWNED,
    EGG,
    ELDER_GUARDIAN,
    ENDERMAN,
    ENDERMITE,
    ENDER_CRYSTAL,
    ENDER_DRAGON,
    ENDER_PEARL,
    ENDER_SIGNAL,
    EVOKER,
    EVOKER_FANGS,
    EXPERIENCE_ORB,
    FALLING_BLOCK,
    FIREBALL,
    FIREWORK,
    FISHING_HOOK,
    FOX,
    GHAST,
    GIANT,
    GLOW_ITEM_FRAME,
    GLOW_SQUID,
    GOAT,
    GUARDIAN,
    HOGLIN,
    HORSE,
    HUSK,
    ILLUSIONER,
    IRON_GOLEM,
    ITEM_FRAME,
    LEASH_HITCH,
    LIGHTNING,
    LLAMA,
    LLAMA_SPIT,
    MAGMA_CUBE,
    MARKER,
    MINECART,
    MINECART_CHEST,
    MINECART_COMMAND,
    MINECART_FURNACE,
    MINECART_HOPPER,
    MINECART_MOB_SPAWNER,
    MINECART_TNT,
    MULE,
    MUSHROOM_COW,
    OCELOT,
    PAINTING,
    PANDA,
    PARROT,
    PHANTOM,
    PIG,
    PIGLIN,
    PIGLIN_BRUTE,
    PILLAGER,
    PLAYER,
    POLAR_BEAR,
    PRIMED_TNT,
    PUFFERFISH,
    RABBIT,
    RAVAGER,
    SALMON,
    SHEEP,
    SHULKER,
    SHULKER_BULLET,
    SILVERFISH,
    SKELETON,
    SKELETON_HORSE,
    SLIME,
    SMALL_FIREBALL,
    SNOWBALL,
    SNOWMAN,
    SPECTRAL_ARROW,
    SPIDER,
    SPLASH_POTION,
    SQUID,
    STRAY,
    STRIDER,
    THROWN_EXP_BOTTLE,
    TRADER_LLAMA,
    TRIDENT,
    TROPICAL_FISH,
    TURTLE,
    VEX,
    VILLAGER,
    VINDICATOR,
    WANDERING_TRADER,
    WITCH,
    WITHER,
    WITHER_SKELETON,
    WITHER_SKULL,
    WOLF,
    ZOGLIN,
    ZOMBIE,
    ZOMBIE_HORSE,
    ZOMBIE_VILLAGER,
    ZOMBIFIED_PIGLIN;

    public static boolean isHorse(EntityType type) {
        switch (type) {
            case DONKEY:
            case HORSE:
            case LLAMA:
            case MULE:
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
            case TRADER_LLAMA:
                return true;
            default:
                return false;
        }
    }

    public static boolean isMinecart(EntityType type) {
        switch (type) {
            case MINECART:
            case MINECART_CHEST:
            case MINECART_COMMAND:
            case MINECART_FURNACE:
            case MINECART_HOPPER:
            case MINECART_MOB_SPAWNER:
            case MINECART_TNT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isLivingEntity(org.bukkit.entity.EntityType type) {
        return (type.getEntityClass() != null && LivingEntity.class.isAssignableFrom(type.getEntityClass()));
    }

    public static boolean isAgeableEntity(org.bukkit.entity.EntityType type) {
        return (type.getEntityClass() != null && Ageable.class.isAssignableFrom(type.getEntityClass()));
    }

    public static boolean isAnimal(org.bukkit.entity.EntityType type) {
        return (type.getEntityClass() != null && Animals.class.isAssignableFrom(type.getEntityClass()));
    }

    public static boolean isSize(org.bukkit.entity.EntityType type) {
        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13) && type == org.bukkit.entity.EntityType.PHANTOM)
            return true;

        return type == org.bukkit.entity.EntityType.SLIME || type == org.bukkit.entity.EntityType.MAGMA_CUBE;
    }
}
