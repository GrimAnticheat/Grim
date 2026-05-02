package ac.grim.grimac.manager.config.update;

import org.jetbrains.annotations.NotNull;

/**
 * Per-step migration context. The current file's old user data is exposed as
 * {@link #input()} (read-only); the new file's writeable view is
 * {@link #output()}. Sibling files reachable via {@link #otherFile(String)}
 * support both reads (against their current on-disk state) and writes
 * (queued and applied after every file's own migration chain finishes).
 *
 * <p>Cross-file ordering rule: writes pushed via {@code otherFile(...).put()}
 * land at the END, after every file's own migrations have been applied. The
 * recipient file's migrations DO NOT see queued cross-file writes — design a
 * "move config.yml's X to discord.yml's Y" migration as a single transform
 * on config.yml that reads its own input and writes both sides.
 */
public interface MigrationContext {

    /** OLD user file as parsed by snakeyaml, read-only. */
    @NotNull YamlMap input();

    /**
     * NEW file's writeable view. Starts as the bundled default with any
     * trivially-liftable user overrides already auto-applied (the diff path).
     * Migrations only touch what they explicitly need to.
     */
    @NotNull YamlMap output();

    /**
     * Read-or-write view of a sibling file. Reads see that file's current
     * on-disk state (parsed). Writes are queued against that file's apply
     * log and committed after all files have run their own migrations.
     *
     * @param fileName the sibling file's bare name, e.g. {@code "discord.yml"}.
     */
    @NotNull YamlMap otherFile(@NotNull String fileName);
}
