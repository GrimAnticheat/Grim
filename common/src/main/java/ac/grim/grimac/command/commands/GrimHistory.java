package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.history.HistoryService;
import ac.grim.grimac.api.storage.history.SessionDetail;
import ac.grim.grimac.api.storage.history.SessionSummary;
import ac.grim.grimac.api.storage.history.ViolationEntry;
import ac.grim.grimac.api.storage.model.PlayerIdentity;
import ac.grim.grimac.api.storage.query.Cursor;
import ac.grim.grimac.api.storage.query.Page;
import ac.grim.grimac.api.storage.query.Queries;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.command.render.HistoryComponentRenderer;
import ac.grim.grimac.manager.datastore.DataStoreLifecycle;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * {@code /grim history} UI entry.
 * <p>
 * Command tree (sibling shapes disambiguated by the literal that follows
 * {@code <target>}):
 * <pre>
 *   /grim history &lt;target&gt;
 *       → session list, page 1
 *   /grim history &lt;target&gt; page &lt;P&gt;
 *       → session list, page P
 *   /grim history &lt;target&gt; session &lt;N | "latest"&gt; [-d] [-v]
 *       → session detail for "Session N" (global chronological, Session 1 = oldest).
 *         {@code latest} / {@code last} / {@code l} are aliases for the most
 *         recent session.
 *   /grim history &lt;target&gt; session &lt;N | "latest"&gt; page &lt;P&gt; [-d] [-v]
 *       → session detail, violation page P.
 * </pre>
 * The {@code session} literal leaves the slot after {@code <target>} open for
 * future top-level subcommands (e.g. {@code violations}, {@code summary}) —
 * adding one is a matter of registering another sibling builder with its own
 * literal. Cloud will disambiguate on the literal token without ambiguity.
 * <p>
 * Flags: {@code --detailed}/{@code -d} shows raw violations one-per-row instead
 * of time-bucketed groups; {@code --verbose}/{@code -v} inlines verbose text
 * (full verbose always on hover). {@code --name <regex>}, {@code --match
 * <regex>} and {@code --grep <regex>} narrow the displayed violations
 * (display name, verbose text, or either) and apply to both list and detail
 * views — combining flags ANDs them.
 * <p>
 * Autocompletion on {@code <session>} and {@code <page>} is constrained to the
 * actual valid range for the player in context (computed via
 * {@code countSessions} / violations count), so tab-complete never offers
 * numbers that'd error out.
 * <p>
 * Runs synchronously on the command thread — keeps RCON callers from losing
 * the reply channel mid-command.
 */
public class GrimHistory implements BuildableCommand {

    private static final int MAX_SUGGESTIONS = 30;
    private static final int MAX_PLAYER_SUGGESTIONS = 25;
    private static final String LATEST_ALIAS = "latest";

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        SuggestionProvider<Sender> listPageNumberSuggestions = listPageSuggestions();
        SuggestionProvider<Sender> sessionOrdinalSuggestions = sessionSuggestions();
        SuggestionProvider<Sender> violationPageSuggestions = violationPageSuggestions();
        SuggestionProvider<Sender> targetSuggestions = targetSuggestions(adapter);

        // List, page 1
        commandManager.command(
                applyFilterFlags(commandManager,
                        commandManager.commandBuilder("grim", "grimac")
                                .literal("history", "hist")
                                .permission("grim.history")
                                .required("target", StringParser.stringParser(), targetSuggestions))
                        .handler(this::handleListPage1)
        );
        // List, page N
        commandManager.command(
                applyFilterFlags(commandManager,
                        commandManager.commandBuilder("grim", "grimac")
                                .literal("history", "hist")
                                .permission("grim.history")
                                .required("target", StringParser.stringParser(), targetSuggestions)
                                .literal("page")
                                .required("page_number", IntegerParser.integerParser(1), listPageNumberSuggestions))
                        .handler(this::handleListPageN)
        );
        // Detail (default violation page). The `session` literal lives at the
        // same tree slot as `page` above; Cloud picks the branch by exact
        // match. The session-ordinal arg is a String parser so it can accept
        // the "latest" / "last" / "l" aliases alongside plain integers — the
        // handler resolves them via resolveSessionOrdinal().
        commandManager.command(
                applyFilterFlags(commandManager,
                        commandManager.commandBuilder("grim", "grimac")
                                .literal("history", "hist")
                                .permission("grim.history")
                                .required("target", StringParser.stringParser(), targetSuggestions)
                                .literal("session")
                                .required("session", StringParser.stringParser(), sessionOrdinalSuggestions)
                                .flag(commandManager.flagBuilder("detailed").withAliases("d")
                                        .withDescription(Description.of("Show each violation as its own row instead of time-bucketed groups.")))
                                .flag(commandManager.flagBuilder("verbose").withAliases("v")
                                        .withDescription(Description.of("Include the raw verbose text inline on each line (also always available on hover)."))))
                        .handler(this::handleDetailDefaultPage)
        );
        // Detail, violation page N
        commandManager.command(
                applyFilterFlags(commandManager,
                        commandManager.commandBuilder("grim", "grimac")
                                .literal("history", "hist")
                                .permission("grim.history")
                                .required("target", StringParser.stringParser(), targetSuggestions)
                                .literal("session")
                                .required("session", StringParser.stringParser(), sessionOrdinalSuggestions)
                                .literal("page")
                                .required("page_number", IntegerParser.integerParser(1), violationPageSuggestions)
                                .flag(commandManager.flagBuilder("detailed").withAliases("d")
                                        .withDescription(Description.of("Show each violation as its own row instead of time-bucketed groups.")))
                                .flag(commandManager.flagBuilder("verbose").withAliases("v")
                                        .withDescription(Description.of("Include the raw verbose text inline on each line (also always available on hover)."))))
                        .handler(this::handleDetailPageN)
        );
    }

    /**
     * Attach the three regex-filter flags ({@code --name}, {@code --match},
     * {@code --grep}) to a command builder. Shared across all four branches
     * — declarative cloud builders need the flags repeated per branch, but
     * the bodies are identical, so we centralise.
     */
    private static Command.Builder<Sender> applyFilterFlags(
            CommandManager<Sender> commandManager,
            Command.Builder<Sender> b) {
        return b
                .flag(commandManager.flagBuilder("name")
                        .withComponent(StringParser.stringParser())
                        .withDescription(Description.of("Filter to violations whose check display name matches this regex.")))
                .flag(commandManager.flagBuilder("match")
                        .withComponent(StringParser.stringParser())
                        .withDescription(Description.of("Filter to violations whose verbose text matches this regex.")))
                .flag(commandManager.flagBuilder("grep")
                        .withComponent(StringParser.stringParser())
                        .withDescription(Description.of("Filter to violations whose display name OR verbose text matches this regex.")));
    }

    private void handleListPage1(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        Predicate<ViolationEntry> filter = parseFilterFromContext(sender, context);
        if (filter == FILTER_ERROR) return;
        runWithPrelude(sender, target, (uuid, displayName, lifecycle, history) ->
                renderList(sender, lifecycle, history, uuid, displayName, 1, filter));
    }

    private void handleListPageN(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        int page = context.<Integer>get("page_number");
        Predicate<ViolationEntry> filter = parseFilterFromContext(sender, context);
        if (filter == FILTER_ERROR) return;
        runWithPrelude(sender, target, (uuid, displayName, lifecycle, history) ->
                renderList(sender, lifecycle, history, uuid, displayName, Math.max(1, page), filter));
    }

    private void handleDetailDefaultPage(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        String sessionRaw = context.get("session");
        boolean detailed = context.flags().hasFlag("detailed");
        boolean verbose = context.flags().hasFlag("verbose");
        Predicate<ViolationEntry> filter = parseFilterFromContext(sender, context);
        if (filter == FILTER_ERROR) return;
        runWithPrelude(sender, target, (uuid, displayName, lifecycle, history) -> {
            Integer ordinal = resolveSessionOrdinal(sessionRaw, uuid, history);
            if (ordinal == null) {
                sender.sendMessage(message("grim-history-session-not-found",
                        "%prefix% &cSession &f%ordinal%&c not found for &f%player%&c.",
                        Map.of("player", displayName, "ordinal", sessionRaw)));
                return;
            }
            renderDetail(sender, lifecycle, history, uuid, displayName,
                    ordinal, detailed, verbose, /*pageArg*/ null, filter);
        });
    }

    private void handleDetailPageN(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        String sessionRaw = context.get("session");
        int page = context.<Integer>get("page_number");
        boolean detailed = context.flags().hasFlag("detailed");
        boolean verbose = context.flags().hasFlag("verbose");
        Predicate<ViolationEntry> filter = parseFilterFromContext(sender, context);
        if (filter == FILTER_ERROR) return;
        runWithPrelude(sender, target, (uuid, displayName, lifecycle, history) -> {
            Integer ordinal = resolveSessionOrdinal(sessionRaw, uuid, history);
            if (ordinal == null) {
                sender.sendMessage(message("grim-history-session-not-found",
                        "%prefix% &cSession &f%ordinal%&c not found for &f%player%&c.",
                        Map.of("player", displayName, "ordinal", sessionRaw)));
                return;
            }
            renderDetail(sender, lifecycle, history, uuid, displayName,
                    ordinal, detailed, verbose, Math.max(1, page), filter);
        });
    }

    /**
     * Sentinel returned by {@link #parseFilterFromContext(Sender, CommandContext)}
     * when the operator supplied an invalid regex. Handlers compare the
     * return value against this with {@code ==} to distinguish "no filter"
     * (null) from "user error already messaged" (this sentinel) before
     * dispatching to {@code runWithPrelude} — saves a separate boolean
     * field on each handler.
     */
    private static final Predicate<ViolationEntry> FILTER_ERROR = v -> false;

    /**
     * Build a {@link ViolationEntry} predicate from the three filter flags.
     *
     * <ul>
     *   <li>{@code --name <regex>} — matches the violation's display name.</li>
     *   <li>{@code --match <regex>} — matches the verbose string. Rows with
     *       {@code null} verbose drop when this flag is set.</li>
     *   <li>{@code --grep <regex>} — grep-style: matches if EITHER display
     *       name OR verbose hits.</li>
     * </ul>
     *
     * <p>Combining flags ANDs them — each flag is an independent narrowing
     * step. All three use {@link Pattern#CASE_INSENSITIVE} so the operator
     * doesn't have to think about casing.
     *
     * <p>Returns {@code null} when no filter flag is set so callers can
     * short-circuit; returns {@link #FILTER_ERROR} after sending an error
     * message to the sender if a regex fails to compile.
     */
    private static @Nullable Predicate<ViolationEntry> parseFilterFromContext(Sender sender, CommandContext<Sender> ctx) {
        Pattern namePat;
        Pattern verbosePat;
        Pattern grepPat;
        try {
            namePat = compileFlag(ctx, "name");
            verbosePat = compileFlag(ctx, "match");
            grepPat = compileFlag(ctx, "grep");
        } catch (BadRegexException e) {
            sender.sendMessage(MessageUtil.miniMessage("<red>Invalid regex for --" + e.flag + ": " + e.detail));
            return FILTER_ERROR;
        }
        if (namePat == null && verbosePat == null && grepPat == null) return null;
        final Pattern n = namePat;
        final Pattern m = verbosePat;
        final Pattern g = grepPat;
        return v -> {
            if (n != null && !n.matcher(v.displayName()).find()) return false;
            if (m != null) {
                String vb = v.verbose();
                if (vb == null || !m.matcher(vb).find()) return false;
            }
            if (g != null) {
                if (!g.matcher(v.displayName()).find()) {
                    String vb = v.verbose();
                    if (vb == null || !g.matcher(vb).find()) return false;
                }
            }
            return true;
        };
    }

    private static @Nullable Pattern compileFlag(CommandContext<Sender> ctx, String flag) {
        String raw = ctx.flags().<String>get(flag);
        if (raw == null) return null;
        try {
            return Pattern.compile(raw, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            throw new BadRegexException(flag, e.getDescription());
        }
    }

    private static final class BadRegexException extends RuntimeException {
        final String flag;
        final String detail;
        BadRegexException(String flag, String detail) {
            super(detail);
            this.flag = flag;
            this.detail = detail;
        }
    }

    /**
     * Translates the {@code session} argument — either a positive integer, the
     * literal "latest" / "last" / "l", or {@code null}-ish — into a global session
     * ordinal. Returns {@code null} when the target has no sessions or the input
     * is unparseable.
     */
    private static @Nullable Integer resolveSessionOrdinal(String raw, UUID uuid, HistoryService history)
            throws Exception {
        if (raw == null) return null;
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.equals(LATEST_ALIAS) || trimmed.equals("last") || trimmed.equals("l")) {
            long total = history.countSessions(uuid).toCompletableFuture().get(5, TimeUnit.SECONDS);
            return total >= 1 ? (int) Math.min(Integer.MAX_VALUE, total) : null;
        }
        try {
            int n = Integer.parseInt(trimmed);
            return n >= 1 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FunctionalInterface
    private interface HistoryAction {
        void run(UUID uuid, String displayName, DataStoreLifecycle lifecycle, HistoryService history) throws Exception;
    }

    private void runWithPrelude(Sender sender, String target, HistoryAction action) {
        DataStoreLifecycle lifecycle = GrimAPI.INSTANCE.getDataStoreLifecycle();
        // enabled=false when database.yml turns the feature off; loaded=false
        // when start() caught an init failure. Distinguish in the message so
        // the operator knows whether to flip config or check the log.
        if (!lifecycle.isEnabled()) {
            sender.sendMessage(message("grim-history-disabled",
                    "%prefix% &cHistory subsystem is disabled!", Map.of()));
            return;
        }
        if (!lifecycle.isLoaded()) {
            sender.sendMessage(message("grim-history-load-failure",
                    "%prefix% &cHistory subsystem failed to load! Check server console for errors.", Map.of()));
            return;
        }

        HistoryService history = lifecycle.historyService();

        try {
            OfflinePlatformPlayer targetPlayer =
                    GrimAPI.INSTANCE.getPlatformPlayerFactory().getOfflineFromName(target);
            // getOfflineFromName returns null when the platform doesn't know
            // the name (never logged in, typo). getUniqueId() comes back null
            // on some platforms when the offline player exists only as a
            // name-cache miss — treat both as "unknown player".
            if (targetPlayer == null || targetPlayer.getUniqueId() == null) {
                sender.sendMessage(message("grim-history-unknown-player",
                        "%prefix% &cUnknown player: &f%player%", Map.of("player", target)));
                return;
            }
            action.run(targetPlayer.getUniqueId(), target, lifecycle, history);
        } catch (Exception e) {
            // Some exception types carry a null message; fall back to the
            // class name so operators still see *something* useful.
            sender.sendMessage(message("grim-history-failed",
                    "%prefix% &cFailed to load history: &7%error%",
                    Map.of("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())));
        }
    }

    private void renderList(Sender sender, DataStoreLifecycle lifecycle, HistoryService history,
                            UUID uuid, String displayName, int page,
                            @Nullable Predicate<ViolationEntry> filter) throws Exception {
        int entriesPerPage = lifecycle.config().history().entriesPerPage();
        long totalSessions = history.countSessions(uuid).toCompletableFuture().get(5, TimeUnit.SECONDS);
        int maxPages = Math.max(1, (int) ((totalSessions + entriesPerPage - 1) / Math.max(1, entriesPerPage)));
        if (page > maxPages) page = maxPages;

        Cursor cursor = advanceToPage(history, uuid, entriesPerPage, page);
        Page<SessionSummary> result = history
                .listSessions(uuid, cursor, entriesPerPage)
                .toCompletableFuture().get(10, TimeUnit.SECONDS);

        // Filter active: keep only sessions with at least one matching
        // violation. Costs N+1 detail fetches per visible page — acceptable
        // at the page-size limit. Unfiltered path keeps the original single
        // listSessions query.
        if (filter != null) result = filterSessionsByDetail(history, uuid, result, filter);

        UUID ongoingSessionId = ongoingSessionIdFor(lifecycle, uuid);
        List<Component> components = HistoryComponentRenderer.renderSessionList(
                sender, uuid, displayName, page, maxPages, result, ongoingSessionId);
        for (Component c : components) sender.sendMessage(c);
    }

    private static Page<SessionSummary> filterSessionsByDetail(HistoryService history, UUID uuid,
                                                               Page<SessionSummary> result,
                                                               Predicate<ViolationEntry> filter) {
        List<SessionSummary> kept = new ArrayList<>();
        for (SessionSummary s : result.items()) {
            try {
                SessionDetail d = history.getSessionDetail(uuid, s.sessionId())
                        .toCompletableFuture().get(2, TimeUnit.SECONDS);
                if (d == null) continue;
                if (d.violations().stream().anyMatch(filter)) kept.add(s);
            } catch (Exception ignored) {
                // Skip sessions whose detail fetch fails — under-show beats
                // blocking the whole page on one slow row.
            }
        }
        return new Page<>(kept, result.nextCursor());
    }

    private void renderDetail(Sender sender, DataStoreLifecycle lifecycle, HistoryService history,
                              UUID uuid, String displayName, int sessionOrdinal,
                              boolean detailed, boolean verbose, @Nullable Integer violationPage,
                              @Nullable Predicate<ViolationEntry> filter) throws Exception {
        SessionDetail detail = null;
        if (history instanceof ac.grim.grimac.internal.storage.history.HistoryServiceImpl impl) {
            detail = impl.getSessionDetailByOrdinal(uuid, sessionOrdinal)
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
        if (detail == null) {
            sender.sendMessage(message("grim-history-session-not-found",
                    "%prefix% &cSession &f%ordinal%&c not found for &f%player%&c.",
                    Map.of(
                            "player", displayName,
                            "ordinal", Integer.toString(sessionOrdinal))));
            return;
        }
        if (filter != null) detail = HistoryComponentRenderer.applyFilter(detail, filter);
        int pageSize = lifecycle.config().history().entriesPerPage();
        UUID ongoingSessionId = ongoingSessionIdFor(lifecycle, uuid);
        boolean isOngoing = ongoingSessionId != null && ongoingSessionId.equals(detail.sessionId());
        List<Component> components = HistoryComponentRenderer.renderSessionDetail(
                sender, displayName, detail, detailed, verbose,
                violationPage, pageSize, isOngoing);
        for (Component c : components) sender.sendMessage(c);
    }

    private Cursor advanceToPage(HistoryService history, UUID uuid, int pageSize, int page) throws Exception {
        if (page <= 1) return null;
        Cursor cursor = null;
        for (int i = 1; i < page; i++) {
            Page<SessionSummary> r = history.listSessions(uuid, cursor, pageSize)
                    .toCompletableFuture().get(5, TimeUnit.SECONDS);
            cursor = r.nextCursor();
            if (cursor == null) break;
        }
        return cursor;
    }

    private static @Nullable UUID ongoingSessionIdFor(DataStoreLifecycle lifecycle, UUID player) {
        return lifecycle.sessionTracker().currentSessionId(player);
    }

    // ---- suggestion providers ----

    /**
     * Merged online + offline player suggestions for the {@code <target>} argument.
     * <p>
     * Empty prefix: online players only (skips the datastore to avoid
     * enumerating every historical player). Non-empty prefix: online players
     * first, then offline matches from the datastore sorted by {@code last_seen}
     * descending, merged case-insensitively. Combined result is capped at
     * {@link #MAX_PLAYER_SUGGESTIONS}.
     * <p>
     * Permission-gated by the surrounding {@code grim.history} permission on
     * the command itself; Cloud never invokes suggestions for a sender that
     * can't execute the command.
     */
    private static SuggestionProvider<Sender> targetSuggestions(CloudCommandAdapter adapter) {
        SuggestionProvider<Sender> onlineProvider = adapter.onlinePlayerSuggestions();
        return SuggestionProvider.blocking((ctx, in) -> {
            String partial = in.remainingInput();
            String partialLower = partial == null ? "" : partial.toLowerCase(Locale.ROOT);

            List<Suggestion> onlineSuggestions;
            try {
                Iterable<? extends Suggestion> onlineIt = onlineProvider.suggestionsFuture(ctx, in)
                        .toCompletableFuture().get(500, TimeUnit.MILLISECONDS);
                onlineSuggestions = new ArrayList<>();
                onlineIt.forEach(onlineSuggestions::add);
            } catch (Exception e) {
                onlineSuggestions = List.of();
            }

            if (partialLower.isEmpty()) return onlineSuggestions;

            Set<String> seen = new HashSet<>();
            List<Suggestion> out = new ArrayList<>();
            for (Suggestion s : onlineSuggestions) {
                if (seen.add(s.suggestion().toLowerCase(Locale.ROOT))) {
                    out.add(s);
                    if (out.size() >= MAX_PLAYER_SUGGESTIONS) return out;
                }
            }

            DataStoreLifecycle dsl = GrimAPI.INSTANCE.getDataStoreLifecycle();
            if (dsl == null || !dsl.isLoaded() || dsl.dataStore() == null) return out;
            try {
                Page<PlayerIdentity> page = dsl.dataStore().query(
                                Categories.PLAYER_IDENTITY,
                                Queries.listPlayersByNamePrefix(partialLower, MAX_PLAYER_SUGGESTIONS))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS);
                for (PlayerIdentity id : page.items()) {
                    if (id.currentName() == null) continue;
                    if (seen.add(id.currentName().toLowerCase(Locale.ROOT))) {
                        out.add(Suggestion.suggestion(id.currentName()));
                        if (out.size() >= MAX_PLAYER_SUGGESTIONS) return out;
                    }
                }
            } catch (Exception e) {
                // Datastore unavailable or timed out — online-only fallback already populated.
            }
            return out;
        });
    }

    /** Suggest 1..maxPages for the list-pagination page number. */
    private static SuggestionProvider<Sender> listPageSuggestions() {
        return SuggestionProvider.blocking((ctx, in) -> {
            UUID uuid = resolveTargetUuid(ctx);
            if (uuid == null) return List.of();
            DataStoreLifecycle dsl = GrimAPI.INSTANCE.getDataStoreLifecycle();
            if (dsl == null || !dsl.isLoaded() || dsl.historyService() == null) return List.of();
            try {
                long total = dsl.historyService().countSessions(uuid)
                        .toCompletableFuture().get(1, TimeUnit.SECONDS);
                int entriesPerPage = dsl.config().history().entriesPerPage();
                int maxPages = Math.max(1, (int) ((total + entriesPerPage - 1) / Math.max(1, entriesPerPage)));
                return rangeSuggestions(1, Math.min(maxPages, MAX_SUGGESTIONS));
            } catch (Exception e) {
                return List.of();
            }
        });
    }

    /** Suggest 1..totalSessions (capped) plus "latest". */
    private static SuggestionProvider<Sender> sessionSuggestions() {
        return SuggestionProvider.blocking((ctx, in) -> {
            UUID uuid = resolveTargetUuid(ctx);
            if (uuid == null) return List.of(Suggestion.suggestion(LATEST_ALIAS));
            DataStoreLifecycle dsl = GrimAPI.INSTANCE.getDataStoreLifecycle();
            if (dsl == null || !dsl.isLoaded() || dsl.historyService() == null) {
                return List.of(Suggestion.suggestion(LATEST_ALIAS));
            }
            try {
                long total = dsl.historyService().countSessions(uuid)
                        .toCompletableFuture().get(1, TimeUnit.SECONDS);
                int max = (int) Math.min(total, MAX_SUGGESTIONS);
                List<Suggestion> out = new ArrayList<>(max + 1);
                out.add(Suggestion.suggestion(LATEST_ALIAS));
                for (int i = 1; i <= max; i++) out.add(Suggestion.suggestion(Integer.toString(i)));
                return out;
            } catch (Exception e) {
                return List.of(Suggestion.suggestion(LATEST_ALIAS));
            }
        });
    }

    /**
     * Suggest 1..violationPages for the violation-page argument inside a session.
     * Relies on the already-parsed {@code session} argument in the context to
     * find the matching sessionId and count its violations.
     */
    private static SuggestionProvider<Sender> violationPageSuggestions() {
        return SuggestionProvider.blocking((ctx, in) -> {
            UUID uuid = resolveTargetUuid(ctx);
            if (uuid == null) return List.of();
            DataStoreLifecycle dsl = GrimAPI.INSTANCE.getDataStoreLifecycle();
            if (dsl == null || !dsl.isLoaded() || dsl.historyService() == null) return List.of();
            String sessionRaw = ctx.<String>getOrDefault("session", null);
            if (sessionRaw == null) return List.of();
            try {
                Integer ordinal = resolveSessionOrdinal(sessionRaw, uuid, dsl.historyService());
                if (ordinal == null) return List.of();
                SessionDetail detail;
                if (!(dsl.historyService() instanceof ac.grim.grimac.internal.storage.history.HistoryServiceImpl impl)) {
                    return List.of();
                }
                detail = impl.getSessionDetailByOrdinal(uuid, ordinal)
                        .toCompletableFuture().get(1, TimeUnit.SECONDS);
                if (detail == null) return List.of();
                int entriesPerPage = dsl.config().history().entriesPerPage();
                // Page unit depends on --detailed; without that info here, suggest
                // the larger of the two so we never under-offer. Detailed mode
                // paginates violations (larger count); grouped paginates buckets.
                int rows = Math.max(detail.violations().size(), detail.buckets().size());
                int maxPages = Math.max(1, (rows + entriesPerPage - 1) / Math.max(1, entriesPerPage));
                return rangeSuggestions(1, Math.min(maxPages, MAX_SUGGESTIONS));
            } catch (Exception e) {
                return List.of();
            }
        });
    }

    private static @Nullable UUID resolveTargetUuid(CommandContext<Sender> ctx) {
        String target = ctx.<String>getOrDefault("target", null);
        if (target == null) return null;
        OfflinePlatformPlayer p = GrimAPI.INSTANCE.getPlatformPlayerFactory().getOfflineFromName(target);
        return p == null ? null : p.getUniqueId();
    }

    private static List<Suggestion> rangeSuggestions(int fromInclusive, int toInclusive) {
        if (toInclusive < fromInclusive) return List.of();
        List<Suggestion> out = new ArrayList<>(toInclusive - fromInclusive + 1);
        for (int i = fromInclusive; i <= toInclusive; i++) out.add(Suggestion.suggestion(Integer.toString(i)));
        return out;
    }

    private Component message(String key, String fallback, Map<String, String> vars) {
        String raw = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse(key, fallback);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            raw = raw.replace("%" + e.getKey() + "%", e.getValue());
        }
        return MessageUtil.miniMessage(raw);
    }
}
