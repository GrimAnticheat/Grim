package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.fabric.inject.FabricMinecraftServerHandle;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * INTERMEDIARY mappings (Minecraft 1.16.1 .. 1.21.11). Supplies the
 * {@link FabricMinecraftServerHandle} body on {@code MinecraftServer}, mirroring the
 * {@code ServerPlayerMixin} -> {@code FabricServerPlayerHandle} pattern: bare interface
 * name, {@code grim$}-prefixed body that Mixin strips and grafts onto
 * {@code MinecraftServer}. The official (26.x) copy is identical
 * (getPlayerList().getPlayer(UUID) is mapping-stable).
 */
@Mixin(MinecraftServer.class)
@Implements(@Interface(iface = FabricMinecraftServerHandle.class, prefix = "grim$"))
abstract class ServerMixin {

    public boolean grim$isPlayerOnline(UUID uuid) {
        return ((MinecraftServer) (Object) this).getPlayerList().getPlayer(uuid) != null;
    }
}
