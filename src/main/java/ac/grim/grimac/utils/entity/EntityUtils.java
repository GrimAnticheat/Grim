package ac.grim.grimac.utils.entity;

import ac.grim.grimac.utils.enums.EntityType;
import io.github.retrooper.packetevents.packetwrappers.api.helper.WrappedPacketEntityAbstraction;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.utilities.DisguiseUtilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Locale;

public class EntityUtils {
    private static final boolean hasLibDisguise;
    private static final int selfDisguiseEntityID;

    static {
        hasLibDisguise = Bukkit.getServer().getPluginManager().isPluginEnabled("LibsDisguises");
        selfDisguiseEntityID = hasLibDisguise ? DisguiseAPI.getSelfDisguiseId() : 0;
    }

    public static Entity getEntity(WrappedPacketEntityAbstraction packet) {
        if (hasLibDisguise && packet.getEntityId() == selfDisguiseEntityID)
            return null; // Faster
        return packet.getEntity();
    }

    public static boolean isSelfDisguise(int entityID) {
        return hasLibDisguise && selfDisguiseEntityID == entityID;
    }

    public static boolean isSelfDisguise(WrappedPacketEntityAbstraction packet) {
        return hasLibDisguise && selfDisguiseEntityID == packet.getEntityId();
    }

    public static boolean isDisguisedEntity(Player player, int entityID) {
        return hasLibDisguise && selfDisguiseEntityID == entityID ||
                DisguiseUtilities.getDisguise(player, entityID) != null;
    }

    public static EntityType getEntityType(Player player, Entity entity) {
        if (player == entity) return EntityType.PLAYER;
        org.bukkit.entity.EntityType bukkitType = entity.getType();
        if (hasLibDisguise) {
            Disguise disguise = DisguiseAPI.getDisguise(player, entity);
            if (disguise != null) {
                bukkitType = disguise.getType().getEntityType();
            }
        }
        return EntityType.valueOf(bukkitType.toString().toUpperCase(Locale.ROOT));
    }
}
