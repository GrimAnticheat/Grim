package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.entity.FabricGrimEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

public class FabricPlatformPlayerFactory extends AbstractPlatformPlayerFactory<ServerPlayerEntity> {

    private final Function<ServerPlayerEntity, FabricPlatformPlayer> getPlayerFunction;

    public FabricPlatformPlayerFactory(Function<ServerPlayerEntity, FabricPlatformPlayer> playerSupplier,
                                       Function<Entity, FabricGrimEntity> getEntityFunction,
                                       Function<ServerPlayerEntity, FabricPlatformInventory> getInventoryFunction
    ) {
        this.getPlayerFunction = playerSupplier;
        FabricPlatformPlayer.init(getEntityFunction, getInventoryFunction);
    }

    @Override
    protected ServerPlayerEntity getNativePlayer(@NotNull UUID uuid) {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayer(uuid);
    }

    @Override
    protected FabricPlatformPlayer createPlatformPlayer(@NotNull ServerPlayerEntity nativePlayer) {
        return getPlayerFunction.apply(nativePlayer);
    }

    @Override
    protected boolean isNativePlayerType(@NotNull Object playerObject) {
        return playerObject instanceof ServerPlayerEntity;
    }

    @Override
    protected UUID getPlayerUUID(@NotNull ServerPlayerEntity nativePlayer) {
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

    @Override
    public void replaceNativePlayer(@NonNull UUID uuid, @NonNull ServerPlayerEntity serverPlayerEntity) {
        super.cache.getPlayer(uuid).replaceNativePlayer(serverPlayerEntity);
    }
}
