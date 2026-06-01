package ac.grim.grimac.platform.fabric.inject;

import java.util.UUID;

/**
 * A Loom-injected handle interface grafted onto the NMS
 * {@code net.minecraft.server.MinecraftServer} via {@code loom:injected_interfaces} in
 * each Fabric aggregator's {@code fabric.mod.json}. Bodies are supplied per mapping family
 * by {@code @Mixin(MinecraftServer.class)
 * @Implements(@Interface(iface = FabricMinecraftServerHandle.class, prefix = "grim$"))}
 * (see {@code ServerMixin} in fabric-official and fabric-intermediary). Same NMS-free
 * contract and bare-name/collision rules as {@link FabricServerPlayerHandle}.
 *
 * <p>This lets the shared {@code FabricOfflinePlatformPlayer} (now a single copy in
 * fabric-common) ask the server whether a UUID is online without referencing the NMS
 * {@code MinecraftServer} or the per-version {@code GrimACFabricLoaderPlugin}.
 *
 * <p>COLLISION: {@code isPlayerOnline} is checked collision-free against the full
 * {@code MinecraftServer} hierarchy (incl. {@code ReentrantBlockableEventLoop} /
 * {@code BlockableEventLoop} / {@code CommandSource}) on 26.1.2 official mappings and on the
 * layered 1.16.1 / 1.21.11 jars.
 */
public interface FabricMinecraftServerHandle {

    /**
     * Whether a player with this UUID is currently connected
     * (was {@code getPlayerList().getPlayer(uuid) != null}). Version-stable.
     */
    boolean isPlayerOnline(UUID uuid);
}
