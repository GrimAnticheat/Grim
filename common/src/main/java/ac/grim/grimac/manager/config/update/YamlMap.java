package ac.grim.grimac.manager.config.update;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A typed, dotted-path view over a parsed YAML document. Migrations read from
 * one of these (input / sibling-file) and write to another (output) without
 * touching raw text. Implementations live in {@link YamlMapImpl}.
 *
 * <p>Dotted paths walk nested maps: {@code get("a.b.c")} reads
 * {@code root["a"]["b"]["c"]}. Intermediate maps are auto-created on
 * {@link #put} so a migration can synthesise a new nested key without
 * preallocating the structure.
 *
 * <p>Reads return {@code null} when the path doesn't exist or the value is the
 * wrong type for the typed accessor (e.g. {@link #getInt} on a string returns
 * null rather than throwing). Migrations should null-check; the bundled
 * default supplies the floor value when no user override exists.
 */
public interface YamlMap {

    @Nullable Object get(@NotNull String dottedPath);
    @Nullable String getString(@NotNull String dottedPath);
    @Nullable Integer getInt(@NotNull String dottedPath);
    @Nullable Long getLong(@NotNull String dottedPath);
    @Nullable Double getDouble(@NotNull String dottedPath);
    @Nullable Boolean getBool(@NotNull String dottedPath);
    @Nullable List<?> getList(@NotNull String dottedPath);
    @Nullable Map<String, Object> getMap(@NotNull String dottedPath);

    boolean has(@NotNull String dottedPath);

    /**
     * Set a value at the given dotted path. Records a write op that the
     * updater will later apply via the line-mapped patcher (preserving the
     * bundled default's comments). Returns {@code this} for chaining.
     */
    @NotNull YamlMap put(@NotNull String dottedPath, @NotNull Object value);

    /** Remove a key. Records a write op. Returns {@code this} for chaining. */
    @NotNull YamlMap remove(@NotNull String dottedPath);

    /**
     * Convenience for the move case: copy the value at {@code oldPath} to
     * {@code newPath} and remove {@code oldPath}. No-op if {@code oldPath}
     * doesn't exist.
     */
    @NotNull YamlMap rename(@NotNull String oldPath, @NotNull String newPath);
}
