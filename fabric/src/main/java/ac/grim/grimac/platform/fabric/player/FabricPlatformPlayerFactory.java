package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

public class FabricPlatformPlayerFactory extends AbstractPlatformPlayerFactory<ServerPlayerEntity> {

    private final Function<ServerPlayerEntity, AbstractFabricPlatformPlayer> getPlayerFunction;

    public FabricPlatformPlayerFactory(Function<ServerPlayerEntity, AbstractFabricPlatformPlayer> playerSupplier,
                                       Function<Entity, GrimEntity> getEntityFunction,
                                       Function<ServerPlayerEntity, AbstractFabricPlatformInventory> getInventoryFunction
    ) {
        this.getPlayerFunction = playerSupplier;
        AbstractFabricPlatformPlayer.init(getEntityFunction, getInventoryFunction);
    }

    @Override
    protected ServerPlayerEntity getNativePlayer(@NotNull UUID uuid) {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayer(uuid);
    }

    @Override
    protected AbstractFabricPlatformPlayer createPlatformPlayer(@NotNull ServerPlayerEntity nativePlayer) {
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
