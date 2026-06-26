package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.AbstractGrimACFabricEntryPoint;
import ac.grim.grimac.platform.fabric.FabricPlatformServices;
import ac.grim.grimac.platform.fabric.inject.FabricEntityHandle;
import ac.grim.grimac.platform.fabric.inject.FabricServerPlayerHandle;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class FabricPlatformPlayerFactory extends AbstractPlatformPlayerFactory<FabricServerPlayerHandle> {

    private final Map<UUID, OfflinePlatformPlayer> offlinePlatformPlayerCache = new HashMap<>();
    private final Function<FabricServerPlayerHandle, AbstractFabricPlatformPlayer<?>> getPlayerFunction;
    private final Function<FabricEntityHandle, GrimEntity> getEntityFunction;
    private final Function<AbstractFabricPlatformPlayer<?>, AbstractFabricPlatformInventory> getPlayerInventoryFunction;

    @SuppressWarnings("unchecked")
    public <P extends FabricServerPlayerHandle, E extends FabricEntityHandle> FabricPlatformPlayerFactory(
            Function<P, AbstractFabricPlatformPlayer<?>> getPlayerFunction,
            Function<E, GrimEntity> getEntityFunction,
            Function<AbstractFabricPlatformPlayer<?>, AbstractFabricPlatformInventory> getPlayerInventoryFunction
    ) {
        this.getPlayerFunction = player -> getPlayerFunction.apply((P) player);
        this.getEntityFunction = entity -> getEntityFunction.apply((E) entity);
        this.getPlayerInventoryFunction = Objects.requireNonNull(getPlayerInventoryFunction);
    }

    @Override
    protected FabricServerPlayerHandle getNativePlayer(@NotNull UUID uuid) {
        return AbstractGrimACFabricEntryPoint.server().playerByUuid(uuid);
    }

    @Override
    protected FabricServerPlayerHandle getNativePlayer(@NotNull String name) {
        return AbstractGrimACFabricEntryPoint.server().playerByName(name);
    }

    @Override
    protected PlatformPlayer createPlatformPlayer(@NotNull FabricServerPlayerHandle nativePlayer) {
        return getPlayerFunction.apply(nativePlayer);
    }

    @Override
    protected UUID getPlayerUUID(@NotNull FabricServerPlayerHandle nativePlayer) {
        return nativePlayer.uuid();
    }

    @Override
    protected Collection<FabricServerPlayerHandle> getNativeOnlinePlayers() {
        return AbstractGrimACFabricEntryPoint.server().onlinePlayers();
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
            FabricOfflineProfile profile = AbstractGrimACFabricEntryPoint.server().usesAuthentication()
                    ? FabricPlatformServices.profileByName(name)
                    : null;
            result = profile != null
                    ? getOfflinePlayer(profile)
                    : getOfflinePlayer(new FabricOfflineProfile(
                            UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)),
                            name
                    ));
        } else {
            this.offlinePlatformPlayerCache.remove(result.getUniqueId());
        }

        return result;
    }

    @Override
    public Collection<OfflinePlatformPlayer> getOfflinePlayers() {
        Collection<OfflinePlatformPlayer> players = new HashSet<>();
        for (UUID uuid : AbstractGrimACFabricEntryPoint.server().savedPlayerUuids()) {
            players.add(this.getOfflineFromUUID(uuid));
        }

        players.addAll(this.getOnlinePlayers());

        return players;
    }

    public OfflinePlatformPlayer getOfflinePlayer(FabricOfflineProfile profile) {
        OfflinePlatformPlayer player = new FabricOfflinePlatformPlayer(profile.uuid(), profile.name());
        this.offlinePlatformPlayerCache.put(profile.uuid(), player);
        return player;
    }

    @Override
    public void replaceNativePlayer(@NotNull UUID uuid, @NotNull FabricServerPlayerHandle serverPlayerEntity) {
        super.cache.getPlayer(uuid).replaceNativePlayer(serverPlayerEntity);
    }

    public AbstractFabricPlatformInventory getPlatformInventory(AbstractFabricPlatformPlayer<?> serverPlayerEntity) {
        return getPlayerInventoryFunction.apply(serverPlayerEntity);
    }

    public GrimEntity getPlatformEntity(Object entity) {
        return getEntityFunction.apply((FabricEntityHandle) entity);
    }
}
