package ac.grim.grimac.manager.config.update;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cross-version YAML config updater.
 *
 * <h2>What it does</h2>
 * <ol>
 *   <li>Reads the user's current on-disk config (if it exists).</li>
 *   <li>Writes the bundled default in place, after backing up the old file
 *       to {@code <name>.v<oldVersion>.bak}.</li>
 *   <li>Auto-lifts any user-customised value whose key path also exists in
 *       the new bundled default (the "diff path").</li>
 *   <li>Runs version-gated {@link Migration} steps in order. Each step gets
 *       a {@link MigrationContext} with typed map views over the OLD user
 *       file ({@code input()}) and the NEW file ({@code output()}).</li>
 *   <li>Stamps the file with the latest {@link Spec#latestVersion} +
 *       {@link Spec#flavor} so subsequent boots are no-ops.</li>
 * </ol>
 *
 * <h2>Cross-file writes</h2>
 * Use {@link #updateAll(java.util.Map)} when migrations need to push values
 * between sibling files (e.g. config.yml → discord.yml). The single-file
 * {@link #update(File, Spec)} entry point disables {@code otherFile()}
 * writes — only reads remain valid.
 *
 * <h2>What this preserves</h2>
 * The bundled default's structure + comments. User-edited comments in the
 * OLD file are dropped (the bundled default's commentary always wins);
 * user-edited values are auto-lifted unless a migration overrides them.
 */
public final class ConfigUpdater {

    /** V2 vs V3 plugin lineage marker — fail-fast on flavor mismatch. */
    public enum ConfigFlavor {
        V2, V3;

        public static @Nullable ConfigFlavor parse(@Nullable Object raw) {
            if (raw == null) return null;
            String s = raw.toString().trim().toUpperCase();
            try { return valueOf(s); } catch (IllegalArgumentException e) { return null; }
        }
    }

    /** A single migration step — runs when {@code oldVersion < toVersion}. */
    @FunctionalInterface
    public interface Migration {
        void apply(@NotNull MigrationContext ctx);
    }

    /** Configuration for one logical file. Build with {@link Spec#builder}. */
    public static final class Spec {
        /**
         * Resource directory holding the per-language bundled defaults
         * ({@code <dir>/en.yml}, {@code <dir>/zh.yml}, …). Must end with
         * {@code /}. The updater resolves the active language at update
         * time and falls back to {@code en.yml} if the language file isn't
         * bundled — same shape Configuralize uses for runtime loading.
         */
        public final String resourceDirectory;
        public final int latestVersion;
        public final @NotNull ConfigFlavor flavor;
        /** Keyed by the version this migration upgrades TO. */
        public final @NotNull Map<Integer, Migration> migrations;

        Spec(@NotNull String resourceDirectory, int latestVersion,
             @NotNull ConfigFlavor flavor, @NotNull Map<Integer, Migration> migrations) {
            Objects.requireNonNull(resourceDirectory, "resourceDirectory");
            this.resourceDirectory = resourceDirectory.endsWith("/")
                    ? resourceDirectory : resourceDirectory + "/";
            this.latestVersion = latestVersion;
            this.flavor = Objects.requireNonNull(flavor, "flavor");
            this.migrations = Map.copyOf(migrations);
        }

        public static @NotNull Builder builder(@NotNull String resourceDirectory,
                                               int latestVersion,
                                               @NotNull ConfigFlavor flavor) {
            return new Builder(resourceDirectory, latestVersion, flavor);
        }

        public static final class Builder {
            private final String resourceDirectory;
            private final int latestVersion;
            private final ConfigFlavor flavor;
            private final Map<Integer, Migration> migrations = new LinkedHashMap<>();

            private Builder(String resourceDirectory, int latestVersion, ConfigFlavor flavor) {
                this.resourceDirectory = resourceDirectory;
                this.latestVersion = latestVersion;
                this.flavor = flavor;
            }

            /** Register a migration that runs when upgrading TO {@code toVersion}. */
            public @NotNull Builder migration(int toVersion, @NotNull Migration m) {
                migrations.put(toVersion, m);
                return this;
            }

            public @NotNull Spec build() {
                return new Spec(resourceDirectory, latestVersion, flavor, migrations);
            }
        }
    }

    public static final class Result {
        public final boolean migrated;
        public final int oldVersion;
        public final int newVersion;
        public final @Nullable String warning;

        Result(boolean migrated, int oldVersion, int newVersion, @Nullable String warning) {
            this.migrated = migrated;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
            this.warning = warning;
        }
    }

    private final Class<?> resourceAnchor;
    private final Logger logger;

    public ConfigUpdater(@NotNull Class<?> resourceAnchor, @NotNull Logger logger) {
        this.resourceAnchor = Objects.requireNonNull(resourceAnchor, "resourceAnchor");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Single-file update. Migrations may read sibling files via
     * {@link MigrationContext#otherFile(String)} but writes to siblings are
     * dropped with a warning — use {@link #updateAll(Map, String)} for
     * cross-file write support.
     */
    public @NotNull Result update(@NotNull File configFile, @NotNull Spec spec) throws IOException {
        Map<File, Spec> single = new LinkedHashMap<>();
        single.put(configFile, spec);
        Map<File, Result> results = updateAll(single, "en");
        return results.get(configFile);
    }

    /**
     * Multi-file update. Migrations on file A may push values into file B
     * via {@code ctx.otherFile("B.yml").put(...)}; queued cross-file writes
     * apply at the end (after every file's own migration chain has run).
     *
     * @param langCode lowercase 2-letter language code (e.g. {@code "en"},
     *                 {@code "zh"}). Each Spec's bundled default is loaded
     *                 from {@code <resourceDirectory>/<langCode>.yml} with
     *                 fall-back to {@code en.yml}. Pass the same code
     *                 Configuralize resolved at runtime so the migrated
     *                 on-disk file's comments end up in the matching
     *                 language.
     */
    public @NotNull Map<File, Result> updateAll(@NotNull Map<File, Spec> specs,
                                                @NotNull String langCode) throws IOException {
        // Pending cross-file writes keyed by sibling file's bare name. Each
        // file's writes are accumulated in submission order; after every
        // per-file update finishes, we flush these against the on-disk file
        // through a fresh ConfigPatcher pass (no migrations re-run).
        Map<String, List<WriteLog.Entry>> pendingCrossFile = new LinkedHashMap<>();
        // For sibling-file READS — load each on-disk parsed view lazily.
        Map<String, Map<String, Object>> siblingReadViews = new LinkedHashMap<>();
        Map<File, Result> results = new LinkedHashMap<>();
        // Map of bare filename -> File so otherFile() reads/writes can be
        // resolved against the actual on-disk path.
        Map<String, File> filesByName = new LinkedHashMap<>();
        for (File f : specs.keySet()) filesByName.put(f.getName(), f);

        for (Map.Entry<File, Spec> entry : specs.entrySet()) {
            File configFile = entry.getKey();
            Spec spec = entry.getValue();
            results.put(configFile,
                    updateSingle(configFile, spec, langCode, filesByName, siblingReadViews, pendingCrossFile));
        }

        // Flush queued cross-file writes against each sibling file.
        for (Map.Entry<String, List<WriteLog.Entry>> q : pendingCrossFile.entrySet()) {
            File sibling = filesByName.get(q.getKey());
            if (sibling == null || !sibling.exists()) {
                logger.warning("[grim-config-updater] queued cross-file write to '"
                        + q.getKey() + "' has no resolvable file in this batch — dropping "
                        + q.getValue().size() + " op(s)");
                continue;
            }
            try {
                applyEntries(sibling, q.getValue());
            } catch (IOException e) {
                logger.log(Level.WARNING, "[grim-config-updater] failed to flush "
                        + "cross-file writes into " + sibling.getName(), e);
            }
        }

        return results;
    }

    private @NotNull Result updateSingle(@NotNull File configFile,
                                         @NotNull Spec spec,
                                         @NotNull String langCode,
                                         @NotNull Map<String, File> filesByName,
                                         @NotNull Map<String, Map<String, Object>> siblingReadViews,
                                         @NotNull Map<String, List<WriteLog.Entry>> pendingCrossFile)
            throws IOException {
        if (!configFile.exists()) {
            // Fresh install — let the loader (Configuralize) copy the
            // bundled default in place from the resource. No-op here.
            return new Result(false, -1, spec.latestVersion, null);
        }

        Map<String, Object> oldData = readYaml(configFile.toPath());
        if (oldData == null) {
            return new Result(false, -1, spec.latestVersion,
                    "couldn't parse " + configFile.getName());
        }

        ConfigFlavor onDiskFlavor = ConfigFlavor.parse(oldData.get("config-flavor"));
        if (onDiskFlavor != null && onDiskFlavor != spec.flavor) {
            String warning = configFile.getName() + " has config-flavor=" + onDiskFlavor
                    + " but this build is " + spec.flavor + "; refusing to migrate. "
                    + "Move/rename the file and let the bundled default be written fresh.";
            logger.log(Level.SEVERE, "[grim-config-updater] " + warning);
            return new Result(false, -1, spec.latestVersion, warning);
        }

        int oldVersion = parseVersion(oldData.get("config-version"));
        if (oldVersion >= spec.latestVersion && onDiskFlavor != null) {
            return new Result(false, oldVersion, spec.latestVersion, null);
        }

        Path backup = configFile.toPath().resolveSibling(
                configFile.getName() + ".v" + oldVersion + ".bak");
        if (!Files.exists(backup)) {
            Files.copy(configFile.toPath(), backup);
        }

        String resolvedResource = resolveBundledDefaultPath(spec, langCode);
        try (InputStream defaultStream = openResource(resolvedResource)) {
            Files.copy(defaultStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, Object> newData = readYaml(configFile.toPath());
        if (newData == null) {
            String warning = "bundled default " + resolvedResource
                    + " is unparseable; backup at " + backup;
            logger.log(Level.SEVERE, "[grim-config-updater] " + warning);
            return new Result(false, oldVersion, spec.latestVersion, warning);
        }

        WriteLog ownLog = WriteLog.active();
        YamlMap inputView = new YamlMapImpl(oldData, WriteLog.noop());
        YamlMap outputView = new YamlMapImpl(newData, ownLog);

        // Auto-lift trivially-matching user values into outputView. The diff
        // walk records each lift in ownLog so it goes through the same
        // patcher pass as migration writes.
        autoLift("", oldData, newData, outputView);

        // Run each version-gated migration in order.
        for (int v = oldVersion + 1; v <= spec.latestVersion; v++) {
            Migration m = spec.migrations.get(v);
            if (m == null) continue;
            MigrationContext ctx = new ContextImpl(
                    inputView, outputView, filesByName, siblingReadViews, pendingCrossFile, logger);
            try {
                m.apply(ctx);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "[grim-config-updater] migration to v" + v
                        + " failed for " + configFile.getName()
                        + " — partial state may have been applied", e);
            }
        }

        // Always stamp version + flavor.
        outputView.put("config-version", spec.latestVersion);
        outputView.put("config-flavor", spec.flavor.name());

        applyEntries(configFile, ownLog.finalState().entrySet().stream()
                .map(e -> new WriteLog.Entry(
                        e.getValue() == null ? WriteLog.Op.REMOVE : WriteLog.Op.PUT,
                        e.getKey(), e.getValue()))
                .toList());

        logger.info("[grim-config-updater] " + configFile.getName()
                + " migrated v" + oldVersion + " → v" + spec.latestVersion
                + " (flavor " + spec.flavor + ", backup at " + backup.getFileName() + ")");
        return new Result(true, oldVersion, spec.latestVersion, null);
    }

    @SuppressWarnings("unchecked")
    private static void autoLift(String pathPrefix,
                                 Map<String, Object> oldData,
                                 Map<String, Object> newData,
                                 YamlMap output) {
        for (Map.Entry<String, Object> entry : oldData.entrySet()) {
            String key = entry.getKey();
            if (!newData.containsKey(key)) continue;
            String currentPath = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;
            Object oldValue = entry.getValue();
            Object newValue = newData.get(key);
            if (oldValue instanceof Map && newValue instanceof Map) {
                autoLift(currentPath,
                        (Map<String, Object>) oldValue,
                        (Map<String, Object>) newValue,
                        output);
            } else if (!Objects.equals(oldValue, newValue)) {
                output.put(currentPath, oldValue);
            }
        }
    }

    private void applyEntries(File configFile, List<WriteLog.Entry> entries) throws IOException {
        if (entries.isEmpty()) return;
        ConfigPatcher patcher = new ConfigPatcher(configFile);
        List<ConfigPatcher.PendingChange> pending = new ArrayList<>();
        for (WriteLog.Entry e : entries) {
            if (e.op == WriteLog.Op.REMOVE) {
                logger.log(Level.FINE, "[grim-config-updater] REMOVE op for '"
                        + e.path + "' on " + configFile.getName()
                        + " is not yet supported; expected the bundled default to drop the key");
                continue;
            }
            ConfigPatcher.NodePosition pos = patcher.getNodePosition(e.path);
            if (pos == null) {
                logger.log(Level.FINE, "[grim-config-updater] no slot for migrated path '"
                        + e.path + "' in " + configFile.getName() + "; dropping");
                continue;
            }
            pending.add(new ConfigPatcher.PendingChange(pos, e.value));
        }
        Collections.sort(pending);
        for (ConfigPatcher.PendingChange c : pending) {
            patcher.applyChange(c);
        }
        patcher.save();
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, Object> readYaml(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length == 0) return null;
            Object loaded = new Yaml().load(new String(bytes, StandardCharsets.UTF_8));
            if (loaded instanceof Map) return (Map<String, Object>) loaded;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private InputStream openResource(String resourcePath) throws IOException {
        InputStream in = resourceAnchor.getResourceAsStream(resourcePath);
        if (in == null) throw new IOException("bundled default not found at " + resourcePath);
        return in;
    }

    /**
     * Pick the bundled default to copy in for this update. Tries the
     * caller-supplied language first ({@code <dir>/<langCode>.yml}), then
     * falls back to {@code <dir>/en.yml}. Caller is expected to pass the
     * same language Configuralize resolved (after its own all-or-nothing
     * fallback check) so the migrated file's comments stay aligned with
     * what's read back at runtime.
     */
    private @NotNull String resolveBundledDefaultPath(@NotNull Spec spec, @NotNull String langCode) {
        String lang = langCode.toLowerCase();
        if (!lang.isEmpty() && !lang.equals("en")) {
            String langPath = spec.resourceDirectory + lang + ".yml";
            if (resourceAnchor.getResource(langPath) != null) return langPath;
        }
        return spec.resourceDirectory + "en.yml";
    }

    private static int parseVersion(@Nullable Object raw) {
        if (raw == null) return 0;
        if (raw instanceof Number n) return n.intValue();
        try { return Integer.parseInt(raw.toString().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    /**
     * Concrete {@link MigrationContext}. {@link #otherFile(String)} reads
     * always work; writes only land if the sibling is part of the same
     * {@link #updateAll(Map)} batch (else the queued ops get dropped with a
     * warning at flush time).
     */
    private static final class ContextImpl implements MigrationContext {
        private final YamlMap input;
        private final YamlMap output;
        private final Map<String, File> filesByName;
        private final Map<String, Map<String, Object>> siblingReadViews;
        private final Map<String, List<WriteLog.Entry>> pendingCrossFile;
        private final Logger logger;

        ContextImpl(YamlMap input,
                    YamlMap output,
                    Map<String, File> filesByName,
                    Map<String, Map<String, Object>> siblingReadViews,
                    Map<String, List<WriteLog.Entry>> pendingCrossFile,
                    Logger logger) {
            this.input = input;
            this.output = output;
            this.filesByName = filesByName;
            this.siblingReadViews = siblingReadViews;
            this.pendingCrossFile = pendingCrossFile;
            this.logger = logger;
        }

        @Override public @NotNull YamlMap input() { return input; }
        @Override public @NotNull YamlMap output() { return output; }

        @Override
        public @NotNull YamlMap otherFile(@NotNull String fileName) {
            File f = filesByName.get(fileName);
            Map<String, Object> view = siblingReadViews.computeIfAbsent(fileName, n -> {
                if (f == null || !f.exists()) return new LinkedHashMap<>();
                Map<String, Object> parsed = readYaml(f.toPath());
                return parsed == null ? new LinkedHashMap<>() : parsed;
            });
            // Writes go into a per-file pending log that the updater flushes
            // at the end of updateAll(). For single-file update() calls the
            // log still accumulates but never gets flushed (sibling not in
            // the batch); writes get dropped with a warning at flush time.
            WriteLog log = WriteLog.sibling(fileName, pendingCrossFile, logger);
            return new YamlMapImpl(view, log);
        }
    }
}
