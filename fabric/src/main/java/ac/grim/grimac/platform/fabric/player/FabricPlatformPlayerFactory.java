package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor
public class FabricPlatformPlayerFactory extends AbstractPlatformPlayerFactory<ServerPlayer> {

    private final Function<ServerPlayer, AbstractFabricPlatformPlayer> getPlayerFunction;
    private final Function<Entity, GrimEntity> getEntityFunction;
    private final Function<ServerPlayer, AbstractFabricPlatformInventory> getPlayerInventoryFunction;

    @Override
    protected ServerPlayer getNativePlayer(@NotNull UUID uuid) {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerList().getPlayer(uuid);
    }

    @Override
    protected AbstractFabricPlatformPlayer createPlatformPlayer(@NotNull ServerPlayer nativePlayer) {
        return getPlayerFunction.apply(nativePlayer);
    }

    @Override
    protected UUID getPlayerUUID(@NotNull ServerPlayer nativePlayer) {
        return nativePlayer.getUUID();
    }

    @Override
    protected Collection<ServerPlayer> getNativeOnlinePlayers() {
        // Get the list of online players from the server
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerList().getPlayers();
    }

    @Override
    public void replaceNativePlayer(@NotNull UUID uuid, @NotNull ServerPlayer serverPlayerEntity) {
        super.cache.getPlayer(uuid).replaceNativePlayer(serverPlayerEntity);
    }

    public AbstractFabricPlatformInventory getPlatformInventory(ServerPlayer serverPlayerEntity) {
        return getPlayerInventoryFunction.apply(serverPlayerEntity);
    }

    public GrimEntity getPlatformEntity(Entity entity) {
        return getEntityFunction.apply(entity);
    }
}
