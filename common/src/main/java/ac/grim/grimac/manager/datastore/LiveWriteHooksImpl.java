package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.model.VerboseFormat;
import ac.grim.grimac.internal.storage.checks.CheckRegistry;
import ac.grim.grimac.internal.storage.checks.StableKeyMapping;
import ac.grim.grimac.internal.storage.identity.PlayerIdentityService;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete {@link LiveWriteHooks}. Caches display-name → checkId locally so
 * the hot path skips the {@code synchronized intern(...)} call on
 * {@link CheckRegistry}.
 */
public final class LiveWriteHooksImpl implements LiveWriteHooks {

    private final DataStore store;
    private final PlayerIdentityService identityService;
    private final CheckRegistry checkRegistry;
    private final SessionTracker sessionTracker;
    /** display name (lowercased) → checkId. Populated lazily via intern. */
    private final Map<String, Integer> checkIdCache = new ConcurrentHashMap<>();
    /** display-name-lowercase of checks we've already warned about. Prevents log spam. */
    private final Set<String> missingStableKeyLogged = ConcurrentHashMap.newKeySet();

    public LiveWriteHooksImpl(
            @NotNull DataStore store,
            @NotNull PlayerIdentityService identityService,
            @NotNull CheckRegistry checkRegistry,
            @NotNull SessionTracker sessionTracker) {
        this.store = store;
        this.identityService = identityService;
        this.checkRegistry = checkRegistry;
        this.sessionTracker = sessionTracker;
    }

    @Override
    public void onJoin(
            @NotNull UUID uuid,
            @Nullable String name,
            long now,
            @NotNull SessionTracker.ClientMeta meta) {
        identityService.observe(uuid, name, now);
        sessionTracker.observeActivity(uuid, now, meta);
    }

    @Override
    public void onQuit(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta) {
        sessionTracker.close(uuid, now, meta);
    }

    @Override
    public void observeBrand(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta) {
        sessionTracker.observeActivity(uuid, now, meta);
    }

    @Override
    public void recordFlag(
            @NotNull UUID playerUuid,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose,
            long now,
            @NotNull SessionTracker.ClientMeta meta) {
        UUID sessionId = sessionTracker.currentSessionId(playerUuid);
        if (sessionId == null) {
            // Two real cases: a check fired on a packet between LOGIN_SUCCESS
            // and PlayerJoinEvent (no session yet), or a test harness called
            // recordFlag directly. Synthesise a session so the violation has
            // somewhere to land; PlayerJoinEvent's onJoin will extend it.
            sessionId = sessionTracker.observeActivity(playerUuid, now, meta);
        }
        final int checkId = resolveCheckId(check);
        final UUID sid = sessionId;
        store.submit(Categories.VIOLATION, e -> e
                .sessionId(sid)
                .playerUuid(playerUuid)
                .checkId(checkId)
                .vl(vl)
                .occurredEpochMs(now)
                .verbose(verbose)
                .verboseFormat(VerboseFormat.TEXT));
    }

    @Override
    public void onJoinFromUserLogin(@NotNull PlatformPlayer player, @NotNull User user, long now) {
        GrimPlayer gp = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(user);
        onJoin(player.getUniqueId(), player.getName(), now, LiveWriteHooks.clientMetaFor(user, gp));
    }

    @Override
    public void onQuitFromUserDisconnect(@NotNull User user, @Nullable GrimPlayer grimPlayer, long now) {
        UUID uuid = user.getUUID();
        if (uuid == null) return; // disconnected pre-LOGIN_SUCCESS — no session to close
        onQuit(uuid, now, LiveWriteHooks.clientMetaFor(user, grimPlayer));
    }

    @Override
    public void observeBrandFromCheck(@NotNull GrimPlayer grimPlayer) {
        UUID uuid = grimPlayer.user.getUUID();
        if (uuid == null) return;
        observeBrand(uuid, System.currentTimeMillis(), LiveWriteHooks.clientMetaFor(grimPlayer.user, grimPlayer));
    }

    @Override
    public void recordFlagFromCheck(
            @NotNull GrimPlayer player,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose) {
        try {
            recordFlag(player.uuid, check, vl, verbose, System.currentTimeMillis(), SessionTracker.ClientMeta.empty());
        } catch (RuntimeException e) {
            // Don't let a datastore issue break the alert path; the legacy
            // write already ran when we got here. One warn, then swallow.
            LogUtil.warn("v1 datastore recordFlag failed: " + e.getMessage());
        }
    }

    private int resolveCheckId(@NotNull AbstractCheck check) {
        String display = check.getCheckName();
        String key = display.toLowerCase(Locale.ROOT);
        Integer cached = checkIdCache.get(key);
        if (cached != null) return cached;

        String declaredStable = check.getStableKey();
        String stable;
        if (declaredStable != null && !declaredStable.isEmpty()) {
            stable = declaredStable;
        } else {
            // Check hasn't adopted the stable-key contract yet. Fall back to
            // the legacy map, and warn exactly once per display so the
            // missing declaration surfaces without spamming the log.
            if (missingStableKeyLogged.add(key)) {
                LogUtil.warn("[grim-history] check " + display
                        + " has no stableKey declared; falling back to StableKeyMapping. "
                        + "Populate the @CheckData.stableKey / CheckInfo.stableKey field.");
            }
            stable = StableKeyMapping.stableKeyFor(display)
                    .orElse(StableKeyMapping.legacyFallback(display));
        }
        String description = check.getDescription();
        String introducedVersion = safePluginVersion();
        int id = checkRegistry.intern(stable, display, description, introducedVersion);
        checkIdCache.put(key, id);
        return id;
    }

    private static @Nullable String safePluginVersion() {
        try {
            var plugin = GrimAPI.INSTANCE.getGrimPlugin();
            if (plugin == null || plugin.getDescription() == null) return null;
            return plugin.getDescription().getVersion();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
