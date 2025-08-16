package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class FabricPlatformPlayerFactory extends AbstractPlatformPlayerFactory<ServerPlayerEntity> {

    private final Map<UUID, OfflinePlatformPlayer> offlinePlatformPlayerCache = new HashMap<>();
    private final Function<ServerPlayerEntity, AbstractFabricPlatformPlayer> getPlayerFunction;
    private final Function<Entity, GrimEntity> getEntityFunction;
    private final Function<ServerPlayerEntity, AbstractFabricPlatformInventory> getPlayerInventoryFunction;

    public FabricPlatformPlayerFactory(Function<ServerPlayerEntity, AbstractFabricPlatformPlayer> playerSupplier,
                                       Function<Entity, GrimEntity> getEntityFunction,
                                       Function<ServerPlayerEntity, AbstractFabricPlatformInventory> getInventoryFunction
    ) {
        this.getPlayerFunction = playerSupplier;
        this.getEntityFunction = getEntityFunction;
        this.getPlayerInventoryFunction = getInventoryFunction;
    }

    @Override
    protected ServerPlayerEntity getNativePlayer(@NotNull UUID uuid) {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayer(uuid);
    }

    @Override
    protected ServerPlayerEntity getNativePlayer(@NonNull String name) {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayer(name);
    }

    @Override
    protected AbstractFabricPlatformPlayer createPlatformPlayer(@NotNull ServerPlayerEntity nativePlayer) {
        return getPlayerFunction.apply(nativePlayer);
    }

    @Override
    protected UUID getPlayerUUID(@NotNull ServerPlayerEntity nativePlayer) {
        return nativePlayer.getUuid();
    }

    @Override
    protected Collection<ServerPlayerEntity> getNativeOnlinePlayers() {
        // Get the list of online players from the server
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayerList();
    }

    @Override
    public OfflinePlatformPlayer getOfflineFromUUID(@NotNull UUID uuid) {
        return null;
    }

    @Override
    public OfflinePlatformPlayer getOfflineFromName(@NotNull String name) {
        OfflinePlatformPlayer result = this.getFromName(name);
        if (result == null) {
            GameProfile profile = null;
            // Only fetch an online UUID in online mode
            // TODO (cross-platform) add a config option for "offline-mode" servers with online-mode behind a proxy
            if (GrimACFabricLoaderPlugin.FABRIC_SERVER.isOnlineMode()) {
                // THIS CAN BLOCK THE CALLING THREAD!
                profile = GrimACFabricLoaderPlugin.LOADER.getPlatformServer().getProfileByName(name);
            }

            result = this.getOfflinePlayer(profile != null
                    // Use the GameProfile even when we get a UUID so we ensure we still have a name
                    ? profile
                    // Make an OfflinePlayer using an offline mode UUID since the name has no profile
                    : new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)), name)
            );
        } else {
            this.offlinePlatformPlayerCache.remove(result.getUniqueId());
        }

        return result;
    }

    public OfflinePlatformPlayer getOfflinePlayer(GameProfile profile) {
        OfflinePlatformPlayer player = new FabricOfflinePlatformPlayer(profile.getId(), profile.getName());
        this.offlinePlatformPlayerCache.put(profile.getId(), player);
        return player;
    }

    @Override
    public void replaceNativePlayer(@NonNull UUID uuid, @NonNull ServerPlayerEntity serverPlayerEntity) {
        super.cache.getPlayer(uuid).replaceNativePlayer(serverPlayerEntity);
    }

    public AbstractFabricPlatformInventory getPlatformInventory(ServerPlayerEntity serverPlayerEntity) {
        return getPlayerInventoryFunction.apply(serverPlayerEntity);
    }

    public GrimEntity getPlatformEntity(Entity entity) {
        return getEntityFunction.apply(entity);
    }
}
