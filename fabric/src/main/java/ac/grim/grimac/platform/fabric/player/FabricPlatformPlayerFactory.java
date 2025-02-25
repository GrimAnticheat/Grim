package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.UUID;

public class FabricPlatformPlayerFactory extends AbstractPlatformPlayerFactory<ServerPlayerEntity> {

    @Override
    protected ServerPlayerEntity getNativePlayer(UUID uuid) {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayer(uuid);
    }

    @Override
    protected PlatformPlayer createPlatformPlayer(ServerPlayerEntity nativePlayer) {
        return new FabricPlatformPlayer(nativePlayer);
    }

    @Override
    protected boolean isNativePlayerType(Object playerObject) {
        return playerObject instanceof ServerPlayerEntity;
    }

    @Override
    protected UUID getPlayerUUID(ServerPlayerEntity nativePlayer) {
        return nativePlayer.getUuid();
    }

    @Override
    protected Class<ServerPlayerEntity> getNativePlayerClass() {
        return ServerPlayerEntity.class;
    }

    @Override
    protected Collection<ServerPlayerEntity> getNativeOnlinePlayers() {
        // Get the list of online players from the server
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayerList();
    }
}
