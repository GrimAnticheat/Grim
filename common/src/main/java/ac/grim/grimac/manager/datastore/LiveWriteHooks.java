package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Plugin-side facade that fans out player-lifecycle and flag events into the
 * data store. Datastore-disabled or init-failed paths get {@link #NOOP}, so
 * callers don't null-check.
 *
 * <p>The "from" convenience methods build all the per-call args (UUID, name,
 * timestamp, {@link SessionTracker.ClientMeta}) inside the impl. NOOP skips
 * the work entirely. Granular methods stay platform-neutral for tests +
 * tooling.
 */
public interface LiveWriteHooks {

    /** Player joined. Upsert identity (firstSeen=min / lastSeen=max) and open/extend a session. */
    void onJoin(@NotNull UUID uuid, @Nullable String name, long now, @NotNull SessionTracker.ClientMeta meta);

    /** Player disconnected. Flush the closing heartbeat on the session row + clear the in-memory tracker entry. */
    void onQuit(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta);

    /**
     * Brand packet arrived. Re-issues a session upsert with the now-known
     * {@code clientBrand} field — the brand-channel plugin message can arrive
     * after PlayerJoinEvent, so the initial onJoin upsert may have left
     * client_brand null.
     */
    void observeBrand(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta);

    /** Record a single flag with the full check object — pulls stable_key + description from the check. */
    void recordFlag(
            @NotNull UUID playerUuid,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose,
            long now,
            @NotNull SessionTracker.ClientMeta meta);

    /** Convenience for {@code PacketPlayerJoinQuit.onUserLogin}. Builds meta from the User + GrimPlayer internally. */
    void onJoinFromUserLogin(@NotNull PlatformPlayer player, @NotNull User user, long now);

    /** Convenience for {@code PlayerDataManager.onDisconnect}. Builds meta internally; tolerates null grimPlayer (early-disconnect race). */
    void onQuitFromUserDisconnect(@NotNull User user, @Nullable GrimPlayer grimPlayer, long now);

    /** Convenience for {@code ClientBrand.onPacketReceive}. Pulls UUID + builds meta from the GrimPlayer internally. */
    void observeBrandFromCheck(@NotNull GrimPlayer grimPlayer);

    /**
     * Convenience for {@code PunishmentManager.recordV1Flag}. Wraps the call
     * in a try/catch so call sites don't need their own — datastore failures
     * are warned once and swallowed (the legacy violation write already ran).
     */
    void recordFlagFromCheck(
            @NotNull GrimPlayer player,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose);

    /**
     * Build the {@link SessionTracker.ClientMeta} stamped on session upserts
     * for {@code user}. Reads from the passed-in {@link GrimPlayer} (when
     * non-null) for the captured client brand. Public so tests and other
     * callers can mint a meta with the same shape the impl uses.
     */
    static @NotNull SessionTracker.ClientMeta clientMetaFor(
            @NotNull User user, @Nullable GrimPlayer gp) {
        String grimVersion = GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        int clientVersionPvn = user.getClientVersion().getProtocolVersion();
        String serverVersion = GrimAPI.INSTANCE.getPlatformServer().getPlatformImplementationString();
        String brand = null;
        if (gp != null) {
            ClientBrand check = gp.checkManager.getPacketCheck(ClientBrand.class);
            if (check != null && check.isHasBrand()) brand = check.getBrand();
        }
        return new SessionTracker.ClientMeta(grimVersion, brand, clientVersionPvn, serverVersion);
    }

    /**
     * No-op hooks returned by {@code DataStoreLifecycle.liveWriteHooks()}
     * when the v1 datastore is disabled or its init failed. Every method is
     * empty — the convenience overloads' arg-building is skipped too.
     */
    LiveWriteHooks NOOP = new LiveWriteHooks() {
        @Override public void onJoin(@NotNull UUID u, @Nullable String n, long t, @NotNull SessionTracker.ClientMeta m) {}
        @Override public void onQuit(@NotNull UUID u, long t, @NotNull SessionTracker.ClientMeta m) {}
        @Override public void observeBrand(@NotNull UUID u, long t, @NotNull SessionTracker.ClientMeta m) {}
        @Override public void recordFlag(@NotNull UUID u, @NotNull AbstractCheck c, double v, @Nullable String vb, long t, @NotNull SessionTracker.ClientMeta m) {}
        @Override public void onJoinFromUserLogin(@NotNull PlatformPlayer p, @NotNull User u, long t) {}
        @Override public void onQuitFromUserDisconnect(@NotNull User u, @Nullable GrimPlayer g, long t) {}
        @Override public void observeBrandFromCheck(@NotNull GrimPlayer g) {}
        @Override public void recordFlagFromCheck(@NotNull GrimPlayer p, @NotNull AbstractCheck c, double v, @Nullable String vb) {}
    };
}
