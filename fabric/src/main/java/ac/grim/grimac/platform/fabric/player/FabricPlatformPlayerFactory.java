package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import com.mojang.authlib.GameProfile;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@RequiredArgsConstructor
public class FabricPlatformPlayerFactory extends AbstractPlatformPlayerFactory<ServerPlayer> {

    private final Map<UUID, OfflinePlatformPlayer> offlinePlatformPlayerCache = new HashMap<>();
    private final Function<ServerPlayer, AbstractFabricPlatformPlayer> getPlayerFunction;
    private final Function<Entity, GrimEntity> getEntityFunction;
    private final Function<AbstractFabricPlatformPlayer, AbstractFabricPlatformInventory> getPlayerInventoryFunction;

    @Override
    protected ServerPlayer getNativePlayer(@NotNull UUID uuid) {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerList().getPlayer(uuid);
    }

    @Override
    protected ServerPlayer getNativePlayer(@NotNull String name) {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerList().getPlayerByName(name);
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
    public OfflinePlatformPlayer getOfflineFromUUID(@NotNull UUID uuid) {
        OfflinePlatformPlayer result = this.getFromUUID(uuid);
        if (result == null) {
            result = this.offlinePlatformPlayerCache.get(uuid);
            if (result == null) {
                result = new FabricOfflinePlatformPlayer(uuid, "");
                this.offlinePlatformPlayerCache.put(uuid, result);
            }
        } else {
            this.offlinePlatformPlayerCache.remove(uuid);
        }

        return result;
    }

    @Override
    public OfflinePlatformPlayer getOfflineFromName(@NotNull String name) {
        OfflinePlatformPlayer result = this.getFromName(name);
        if (result == null) {
            GameProfile profile = null;
            // Only fetch an online UUID in online mode
            // TODO (cross-platform) add a config option for "offline-mode" servers with online-mode behind a proxy
            if (GrimACFabricLoaderPlugin.FABRIC_SERVER.usesAuthentication()) {
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

    @Override
    public Collection<OfflinePlatformPlayer> getOfflinePlayers() {
        PlayerDataStorage storage = GrimACFabricLoaderPlugin.FABRIC_SERVER.playerDataStorage;
        String[] files = storage.playerDir.list((dir, name) -> name.endsWith(".dat"));
        Set<OfflinePlatformPlayer> players = new HashSet<>();

        for (String file : files) {
            try {
                players.add(this.getOfflineFromUUID(UUID.fromString(file.substring(0, file.length() - 4))));
            } catch (IllegalArgumentException ex) {
                // ignore invalid fires in directory
            }
        }

        players.addAll(this.getOnlinePlayers());

        return players;
    }

    public OfflinePlatformPlayer getOfflinePlayer(GameProfile profile) {
        OfflinePlatformPlayer player = new FabricOfflinePlatformPlayer(profile.getId(), profile.getName());
        this.offlinePlatformPlayerCache.put(profile.getId(), player);
        return player;
    }

    @Override
    public void replaceNativePlayer(@NotNull UUID uuid, @NotNull ServerPlayer serverPlayerEntity) {
        super.cache.getPlayer(uuid).replaceNativePlayer(serverPlayerEntity);
    }

    public AbstractFabricPlatformInventory getPlatformInventory(AbstractFabricPlatformPlayer serverPlayerEntity) {
        return getPlayerInventoryFunction.apply(serverPlayerEntity);
    }

    public GrimEntity getPlatformEntity(Entity entity) {
        return getEntityFunction.apply(entity);
    }
}
