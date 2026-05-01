package ac.grim.grimac.manager.config.update;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Centralised registry of {@link ConfigUpdater.Spec} instances for every
 * Grim config file. Each yml file owns:
 *
 * <ul>
 *   <li>a {@code config-flavor} marker (V2 / V3) that the updater rejects on
 *       a mismatch — a wrong-flavor file fails fast instead of silently
 *       flat-merging into the wrong key set;</li>
 *   <li>a {@code config-version} integer bumped on every breaking shape
 *       change to the bundled default;</li>
 *   <li>an optional in-version {@link ConfigUpdater.Migration} chain that
 *       lifts values forward across each version step. Multiple steps
 *       stack — v3 → v8 runs each registered v4/v5/v6/v7/v8 migration.</li>
 * </ul>
 *
 * <p>Resource paths name the directory ({@code "/database/"}) — the updater
 * picks the language-appropriate {@code en.yml} / {@code zh.yml} / … from
 * inside it at update time, falling back to {@code en.yml} when the active
 * locale isn't bundled. Same shape Configuralize uses for runtime loading.
 *
 * <p>Migrations write through the {@link MigrationContext} API: typed
 * dotted-path reads from {@code input()}, writes to {@code output()},
 * cross-file pushes via {@code otherFile(name)}. The updater applies
 * recorded write ops via the line-mapped patcher so the bundled default's
 * comments survive the rewrite.
 *
 * <p>{@code punishments.yml} is intentionally absent — open-ended user-
 * defined data (operator-authored punishment groups), no schema versioning.
 */
public final class GrimConfigSpecs {

    private GrimConfigSpecs() {}

    /**
     * Spec for the main {@code config.yml}.
     *
     * <p>v9 → v10: migrates Grim 2.x's {@code history:} block out of
     * config.yml and into the new {@code database.yml} + the matching
     * {@code databases/<id>.yml}. Pre-V2 operators had all DB settings
     * inline in config.yml; V2 splits the datastore config into its own
     * file. The bundled default at v10 has no {@code history:} block, so
     * the auto-lift naturally drops it from the new file; the migration
     * only needs to ferry the values over.
     */
    public static @NotNull ConfigUpdater.Spec mainConfig() {
        return ConfigUpdater.Spec.builder("/config/", 10, ConfigUpdater.ConfigFlavor.V2)
                .migration(10, ctx -> {
                    String typeRaw = ctx.input().getString("history.database.type");
                    String type = typeRaw == null ? null : typeRaw.trim().toUpperCase(Locale.ROOT);
                    String backendId = backendIdFor(type);
                    if (backendId != null) {
                        // Route every relational category to the operator's
                        // chosen backend so the new layout matches the old
                        // single-DB shape. blob is intentionally absent —
                        // none of the SQL backends declare support for it
                        // (would fail capability validation); leave it on
                        // whatever the bundled default routes blob to.
                        for (String cat : new String[]{"violation", "session", "player-identity", "setting"}) {
                            ctx.otherFile("database.yml").put("database.routing." + cat, backendId);
                        }
                    }
                    // Carry the rendering settings over — these moved out of
                    // history: into database.* with the same names.
                    Integer entriesPerPage = ctx.input().getInt("history.entries-per-page");
                    if (entriesPerPage != null) {
                        ctx.otherFile("database.yml").put("database.history.entries-per-page", entriesPerPage);
                    }
                    String serverName = ctx.input().getString("history.server-name");
                    if (serverName != null) {
                        ctx.otherFile("database.yml").put("database.server-name", serverName);
                    }
                    if (backendId != null && !backendId.equals("sqlite")) {
                        // Connection details only matter for networked backends.
                        // SQLite settings (file path) stay at their defaults.
                        String target = "databases/" + backendId + ".yml";
                        Object host = ctx.input().get("history.database.host");
                        Object port = ctx.input().get("history.database.port");
                        Object db = ctx.input().get("history.database.database");
                        Object user = ctx.input().get("history.database.username");
                        Object pass = ctx.input().get("history.database.password");
                        if (host != null) ctx.otherFile(target).put(backendId + ".host", host);
                        if (port != null) ctx.otherFile(target).put(backendId + ".port", port);
                        if (db != null) ctx.otherFile(target).put(backendId + ".database", db);
                        if (user != null) ctx.otherFile(target).put(backendId + ".user", user);
                        if (pass != null) ctx.otherFile(target).put(backendId + ".password", pass);
                    }
                })
                .build();
    }

    private static @Nullable String backendIdFor(@Nullable String legacyType) {
        if (legacyType == null) return null;
        return switch (legacyType) {
            case "SQLITE" -> "sqlite";
            case "MYSQL" -> "mysql";
            case "POSTGRESQL", "POSTGRES" -> "postgres";
            case "NOOP", "NONE", "DISABLED" -> null;
            default -> null;
        };
    }

    public static @NotNull ConfigUpdater.Spec discord() {
        return ConfigUpdater.Spec.builder("/discord/", 1, ConfigUpdater.ConfigFlavor.V2)
                .build();
    }

    public static @NotNull ConfigUpdater.Spec messages() {
        return ConfigUpdater.Spec.builder("/messages/", 1, ConfigUpdater.ConfigFlavor.V2)
                .build();
    }

    /**
     * The datastore router config (top-level {@code database:} wrapper).
     * No own-file migrations — operators arriving from Grim 2.x have no
     * existing database.yml on disk; the legacy lift happens on the
     * config.yml side via cross-file writes (see {@link #mainConfig}).
     */
    public static @NotNull ConfigUpdater.Spec database() {
        return ConfigUpdater.Spec.builder("/database/", 1, ConfigUpdater.ConfigFlavor.V2)
                .build();
    }

    /**
     * Per-backend file at {@code databases/<id>/en.yml}. Each backend's
     * settings live under a top-level {@code <id>:} wrapper so all
     * per-backend files load through the shared ConfigManager without
     * key collisions across backends.
     */
    public static @NotNull ConfigUpdater.Spec backend(@NotNull String backendId) {
        return ConfigUpdater.Spec.builder("/databases/" + backendId + "/", 1,
                        ConfigUpdater.ConfigFlavor.V2)
                .build();
    }
}
