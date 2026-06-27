package ac.grim.grimac.command.render;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.storage.history.CheckBucket;
import ac.grim.grimac.api.storage.history.CheckCount;
import ac.grim.grimac.api.storage.history.SessionDetail;
import ac.grim.grimac.api.storage.history.SessionSummary;
import ac.grim.grimac.api.storage.history.ViolationEntry;
import ac.grim.grimac.api.storage.query.Page;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Renders history service records into command output components. */
@UtilityClass
public final class HistoryComponentRenderer {

    private static final String SESSION_ROW_FALLBACK = "<hover:show_text:'&bSession &7%session_uuid%"
            + "<newline>&7Click or run &e%detail_command%"
            + "<newline>&7to view session details.'>"
            + "&8[&b%grim_version%&8] &8[&b%server_name%&8] &8[&b%client_version%&8]"
            + " &bSession &f%ordinal%&b duration &f%duration%&b with &c%violations%&b"
            + " violations &8[&c%unique_checks%&8]%crashed_marker% &8(&7%timeago% ago&8)</hover>";

    private static final String GROUP_ROW_FALLBACK = "<hover:show_text:'&bViolations in "
            + "&f%bucket_start%–%bucket_end%&b:%bucket_hover_entries%'>"
            + "&7- %checks_list% &8(&b%offset%&8)</hover>";

    private record TemplateValue(@Nullable String raw, @Nullable Component component) {
        static TemplateValue raw(@Nullable String value) {
            return new TemplateValue(value == null ? "" : value, null);
        }

        static TemplateValue text(@Nullable String value) {
            return new TemplateValue(null, Component.text(value == null ? "" : value));
        }

        static TemplateValue component(@Nullable Component component) {
            return new TemplateValue(null, component == null ? Component.empty() : component);
        }
    }

    private record RenderedTemplate(Component component, String raw) {}

    /**
     * Session-list view. {@code page} is 1-indexed; {@code maxPages} ≥ 1.
     * {@code ongoingSessionId} (optional) is the player's currently-active
     * sessionId from SessionTracker; the matching row shows "current" as its
     * duration. Per-row click / copy-paste detail commands always use the
     * disambiguated {@code /grim history player <target>} form so a shared
     * or re-run command can't dead-end on a literal-collision player name.
     */
    public static @NotNull List<Component> renderSessionList(
            @NotNull Sender sender,
            @NotNull UUID player,
            @NotNull String playerDisplayName,
            int page,
            int maxPages,
            @NotNull Page<SessionSummary> result,
            @Nullable UUID ongoingSessionId) {
        ConfigManager cfg = GrimAPI.INSTANCE.getConfigManager().getConfig();
        String safePlayer = mmSafe(playerDisplayName);
        if (result.items().isEmpty()) {
            return List.of(parse(sender, cfg, "grim-history-no-sessions",
                    "%prefix% &7No session history for &f%player%&7.",
                    Map.of("player", TemplateValue.raw(safePlayer))).component());
        }
        List<Component> out = new ArrayList<>(result.items().size() + 1);
        // Emit both %max_pages% (new) and %maxPages% (pre-cutover spelling) so
        // operators upgrading from 1.x with a legacy grim-history-header that
        // still references %maxPages% see a correctly-rendered count.
        String maxPagesStr = Integer.toString(Math.max(1, maxPages));
        out.add(parse(sender, cfg, "grim-history-header",
                "%prefix% &bShowing session history for &f%player% &8[&f%page%&7/&f%max_pages%&8]",
                Map.of(
                        "player", TemplateValue.raw(safePlayer),
                        "page", TemplateValue.raw(Integer.toString(page)),
                        "max_pages", TemplateValue.raw(maxPagesStr),
                        "maxPages", TemplateValue.raw(maxPagesStr))).component());
        for (SessionSummary s : result.items()) {
            boolean ongoing = ongoingSessionId != null && ongoingSessionId.equals(s.sessionId());
            out.add(renderSummaryLine(sender, cfg, s, ongoing, playerDisplayName));
        }
        return out;
    }

    private static Component renderSummaryLine(Sender sender, ConfigManager cfg, SessionSummary s, boolean ongoing,
                                               String playerDisplayName) {
        long elapsedNow = Math.max(0, System.currentTimeMillis() - s.startedEpochMs());
        String durationText = ongoing
                ? "current"
                : formatDuration(s.durationMs());
        // Crashed-session marker: closed_at == last_activity means the
        // crash sweep stamped it (the disconnect path stamps closed_at =
        // now which is strictly later than the most recent heartbeat).
        // Skip the marker for ongoing sessions — they don't have closed_at
        // set yet so the comparison is irrelevant, and the "current"
        // duration tag already conveys that.
        String crashedMarker = (!ongoing && s.endedUnexpectedly())
                ? cfg.getStringElse("grim-history-crashed-marker", " &8(&ccrashed&8)")
                : "";
        String detailCommand = "/grim history player " + playerDisplayName + " session " + s.sessionOrdinal();
        RenderedTemplate rendered = parse(sender, cfg, "grim-history-session", SESSION_ROW_FALLBACK,
                Map.ofEntries(
                        // Only sanitize untrusted leaves; keep operator-configured fragments intact.
                        Map.entry("player", TemplateValue.raw(mmSafe(playerDisplayName))),
                        Map.entry("grim_version", TemplateValue.raw(nullToUnknown(s.grimVersion()))),
                        Map.entry("server_name", TemplateValue.raw(nullToUnknown(s.serverName()))),
                        Map.entry("client_version", TemplateValue.raw(clientVersionDisplay(s.clientVersion()))),
                        Map.entry("client_brand", TemplateValue.raw(mmSafe(nullToUnknown(s.clientBrand())))),
                        Map.entry("ordinal", TemplateValue.raw(Integer.toString(s.sessionOrdinal()))),
                        Map.entry("duration", TemplateValue.raw(durationText)),
                        Map.entry("violations", TemplateValue.raw(Long.toString(s.violationCount()))),
                        Map.entry("unique_checks", TemplateValue.raw(Integer.toString(s.uniqueCheckCount()))),
                        Map.entry("crashed_marker", TemplateValue.raw(crashedMarker)),
                        Map.entry("timeago", TemplateValue.raw(formatDuration(elapsedNow))),
                        Map.entry("session_uuid", TemplateValue.text(s.sessionId().toString())),
                        Map.entry("detail_command", TemplateValue.text(detailCommand))));
        Component line = rendered.component();
        if (!hasInlineHover(rendered.raw())) {
            line = line.hoverEvent(HoverEvent.showText(sessionHover(sender, detailCommand, s.sessionId().toString())));
        }
        return line.clickEvent(ClickEvent.runCommand(detailCommand));
    }

    /**
     * Session-detail view with violation pagination.
     *
     * @param pageArg    1-based violation page, or {@code null} to default to the
     *                   most-recent page (last page).
     * @param pageSize   violations-per-page; applies to raw rows in detailed mode
     *                   and to buckets in grouped mode.
     * @param isOngoing  {@code true} when this session is the player's currently
     *                   active one — drives the "current" duration marker.
     */
    public static @NotNull List<Component> renderSessionDetail(
            @NotNull Sender sender,
            @NotNull String playerDisplayName,
            @NotNull SessionDetail d,
            boolean detailed,
            boolean verbose,
            @Nullable Integer pageArg,
            int pageSize,
            boolean isOngoing) {
        ConfigManager cfg = GrimAPI.INSTANCE.getConfigManager().getConfig();
        List<Component> out = new ArrayList<>();
        int perPage = Math.max(1, pageSize);

        // Duration: ongoing sessions show just "current" — the %timeago% field
        // already carries the elapsed time since start. Historical sessions show
        // the stored (lastActivity - started) span.
        long elapsedNow = Math.max(0, System.currentTimeMillis() - d.startedEpochMs());
        String durationText = isOngoing
                ? "current"
                : formatDuration(d.durationMs());

        int totalRows = detailed ? d.violations().size() : d.buckets().size();
        int maxPages = Math.max(1, (totalRows + perPage - 1) / perPage);
        int page = pageArg == null ? maxPages : Math.max(1, Math.min(pageArg, maxPages));

        Map<String, TemplateValue> metaVars = Map.ofEntries(
                // Only sanitize untrusted leaves; keep operator-configured fragments intact.
                Map.entry("player", TemplateValue.raw(mmSafe(playerDisplayName))),
                Map.entry("ordinal", TemplateValue.raw(Integer.toString(d.sessionOrdinal()))),
                Map.entry("grim_version", TemplateValue.raw(nullToUnknown(d.grimVersion()))),
                Map.entry("server_name", TemplateValue.raw(nullToUnknown(d.serverName()))),
                Map.entry("client_version", TemplateValue.raw(clientVersionDisplay(d.clientVersion()))),
                Map.entry("client_brand", TemplateValue.raw(mmSafe(nullToUnknown(d.clientBrand())))),
                Map.entry("duration", TemplateValue.raw(durationText)),
                Map.entry("timeago", TemplateValue.raw(formatDuration(elapsedNow))),
                Map.entry("violations", TemplateValue.raw(Integer.toString(d.violations().size()))),
                Map.entry("unique_checks", TemplateValue.raw(Integer.toString(d.uniqueCheckCount()))),
                Map.entry("bucket_size", TemplateValue.raw(formatDuration(d.bucketSizeMs()))),
                Map.entry("page", TemplateValue.raw(Integer.toString(page))),
                Map.entry("max_pages", TemplateValue.raw(Integer.toString(maxPages))));
        out.add(parse(sender, cfg, "grim-history-detail-header",
                "%prefix% &bShowing &f%player%&b's session &f%ordinal%&b details:", metaVars).component());
        out.add(parse(sender, cfg, "grim-history-detail-meta1",
                "&bGrim: &f%grim_version%&b, Server: &f%server_name%&b, Duration: &f%duration%&b, Date: &7%timeago% ago",
                metaVars).component());
        out.add(parse(sender, cfg, "grim-history-detail-meta2",
                "&bClient: &f%client_version%&b, Brand: &f%client_brand%",
                metaVars).component());
        out.add(parse(sender, cfg, "grim-history-detail-violations-header",
                "&bViolations: &8(%violations% total, %unique_checks% unique) &8[&f%page%&7/&f%max_pages%&8]",
                metaVars).component());

        if (d.violations().isEmpty()) {
            out.add(parse(sender, cfg, "grim-history-detail-empty", "&7- (none)", Map.of()).component());
            return out;
        }

        int from = (page - 1) * perPage;
        int to = Math.min(from + perPage, totalRows);
        if (detailed) {
            for (int i = from; i < to; i++) out.add(renderViolationLine(sender, cfg, d.violations().get(i), verbose));
        } else {
            // Group violations in the same bucket. The %checks_list% is built by
            // joining per-check entries rendered via grim-history-check-count.
            for (int i = from; i < to; i++) out.add(renderBucketLine(sender, cfg, d.buckets().get(i), d, verbose));
        }
        return out;
    }

    private static Component renderBucketLine(Sender sender, ConfigManager cfg, CheckBucket bucket, SessionDetail d, boolean verbose) {
        StringBuilder checksList = new StringBuilder();
        boolean first = true;
        for (CheckCount c : bucket.checks()) {
            if (!first) checksList.append("&7, ");
            first = false;
            // Check names can be plugin-authored; render them as plain text.
            checksList.append(cfg.getStringElse("grim-history-check-count",
                            "&f%check_name%&7 x&c%count%")
                    .replace("%check_name%", mmSafe(c.displayName()))
                    .replace("%count%", Integer.toString(c.count())));
        }
        Component bucketHoverEntries = buildBucketHoverEntries(sender, cfg, d, bucket, verbose);
        String bucketStart = formatDuration(bucket.bucketStartOffsetMs());
        String bucketEnd = formatDuration(bucket.bucketStartOffsetMs() + d.bucketSizeMs());
        RenderedTemplate rendered = parse(sender, cfg, "grim-history-detail-group", GROUP_ROW_FALLBACK,
                Map.of(
                        "checks_list", TemplateValue.raw(checksList.toString()),
                        "offset", TemplateValue.raw(bucketStart),
                        "bucket_start", TemplateValue.raw(bucketStart),
                        "bucket_end", TemplateValue.raw(bucketEnd),
                        "bucket_hover_entries", TemplateValue.component(bucketHoverEntries)));
        Component line = rendered.component();
        if (!hasInlineHover(rendered.raw())) {
            line = line.hoverEvent(HoverEvent.showText(bucketHover(sender, cfg, bucketStart, bucketEnd, bucketHoverEntries)));
        }
        return line;
    }

    private static Component buildBucketHoverEntries(Sender sender, ConfigManager cfg, SessionDetail d, CheckBucket bucket, boolean verbose) {
        long bucketStart = bucket.bucketStartOffsetMs();
        long bucketEnd = bucketStart + d.bucketSizeMs();
        Component body = Component.empty();
        for (ViolationEntry v : d.violations()) {
            if (v.offsetFromSessionStartMs() < bucketStart || v.offsetFromSessionStartMs() >= bucketEnd) continue;
            Component description = v.description().isBlank()
                    ? Component.empty()
                    : parse(sender, cfg, "grim-history-hover-description", " — &f%description%",
                            Map.of("description", TemplateValue.text(v.description()))).component();
            String verboseText = v.verbose() == null ? "" : v.verbose();
            Component verboseComponent = verbose && !verboseText.isBlank()
                    ? parse(sender, cfg, "grim-history-detail-group-hover-verbose", " — &7%verbose%",
                            Map.of("verbose", TemplateValue.text(verboseText))).component()
                    : Component.empty();
            body = body.append(parse(sender, cfg, "grim-history-detail-group-hover-entry",
                    "<newline>&8  %offset% &b%check%%description%%verbose%",
                    Map.of(
                            "offset", TemplateValue.text(formatDuration(v.offsetFromSessionStartMs())),
                            "check", TemplateValue.text(v.displayName()),
                            "description", TemplateValue.component(description),
                            "verbose", TemplateValue.component(verboseComponent))).component());
        }
        return body;
    }

    private static Component bucketHover(Sender sender, ConfigManager cfg, String bucketStart, String bucketEnd,
                                         Component bucketHoverEntries) {
        return parse(sender, cfg, "grim-history-detail-group-hover",
                "&bViolations in &f%bucket_start%–%bucket_end%&b:%bucket_hover_entries%",
                Map.of(
                        "bucket_start", TemplateValue.text(bucketStart),
                        "bucket_end", TemplateValue.text(bucketEnd),
                        "bucket_hover_entries", TemplateValue.component(bucketHoverEntries))).component();
    }

    private static Component renderViolationLine(Sender sender, ConfigManager cfg, ViolationEntry v, boolean verbose) {
        String verboseText = v.verbose() == null ? "" : v.verbose();
        // Verbose/check metadata can include user or plugin text; render substitutions as plain text.
        RenderedTemplate rendered = parse(sender, cfg, "grim-history-detail-entry",
                "&7- &f%check% &8(&b%offset%&8)&7 %verbose%",
                Map.of(
                        "check", TemplateValue.raw(mmSafe(v.displayName())),
                        "description", TemplateValue.raw(mmSafe(v.description())),
                        "offset", TemplateValue.raw(formatDuration(v.offsetFromSessionStartMs())),
                        "vl", TemplateValue.raw(Double.toString(v.vl())),
                        "verbose", TemplateValue.raw(verbose ? mmSafe(verboseText) : "")));
        // Hover carries the richer disambiguation — description on its own
        // line, then the raw verbose below. Shown regardless of the -v
        // flag, because operators scanning a dense list still want the
        // quick "what does this check mean" answer without re-running.
        Component line = rendered.component();
        boolean hasDescription = !v.description().isBlank();
        if (!hasInlineHover(rendered.raw()) && (!verboseText.isBlank() || hasDescription)) {
            line = line.hoverEvent(HoverEvent.showText(violationHover(sender, cfg, v, verboseText, hasDescription)));
        }
        return line;
    }

    private static Component violationHover(Sender sender, ConfigManager cfg, ViolationEntry v,
                                            String verboseText, boolean hasDescription) {
        Component description = hasDescription
                ? parse(sender, cfg, "grim-history-hover-description", " — &f%description%",
                        Map.of("description", TemplateValue.text(v.description()))).component()
                : Component.empty();
        Component verbose = !verboseText.isBlank()
                ? parse(sender, cfg, "grim-history-detail-entry-hover-verbose", "<newline>&7%verbose%",
                        Map.of("verbose", TemplateValue.text(verboseText))).component()
                : Component.empty();
        return parse(sender, cfg, "grim-history-detail-entry-hover",
                "&b%check%%description%<newline>&8@ %offset% — vl %vl%%verbose%",
                Map.of(
                        "check", TemplateValue.text(v.displayName()),
                        "description", TemplateValue.component(description),
                        "offset", TemplateValue.text(formatDuration(v.offsetFromSessionStartMs())),
                        "vl", TemplateValue.text(Double.toString(v.vl())),
                        "verbose", TemplateValue.component(verbose))).component();
    }

    private static Component sessionHover(Sender sender, String detailCommand, String sessionId) {
        return parseRaw(sender,
                "&bSession &7%session_uuid%"
                        + "<newline>&7Click or run &e%detail_command%"
                        + "<newline>&7to view session details.",
                Map.of(
                        "session_uuid", TemplateValue.text(sessionId),
                        "detail_command", TemplateValue.text(detailCommand)));
    }

    /**
     * Filter a {@link SessionDetail} through a {@link ViolationEntry}
     * predicate. Drops non-matching violations AND re-aggregates buckets
     * from the survivors so non-detailed mode honours the filter too. The
     * unique-check-count is recomputed from the filtered violations; bucket
     * size and session metadata stay as-is.
     *
     * <p>Used by {@code /grim history --name <regex>} / {@code --match
     * <regex>} / {@code --grep <regex>} flag handling — pre-filtering at
     * the renderer keeps the rest of the rendering pipeline filter-agnostic.
     */
    public static @NotNull SessionDetail applyFilter(@NotNull SessionDetail d,
                                                     @NotNull Predicate<ViolationEntry> filter) {
        List<ViolationEntry> survivors = new ArrayList<>();
        for (ViolationEntry v : d.violations()) if (filter.test(v)) survivors.add(v);
        // Re-aggregate buckets keyed (bucketStart) → (displayName → count).
        // TreeMap orders buckets chronologically; LinkedHashMap on the inner
        // preserves first-seen check order so %checks_list% doesn't reshuffle
        // row-to-row. checkMeta caches the first ViolationEntry per
        // displayName so we can reconstruct CheckCount(checkId, stableKey,
        // displayName, description, count) without a separate lookup —
        // every violation with the same displayName shares those fields.
        TreeMap<Long, Map<String, int[]>> agg = new TreeMap<>();
        Map<String, ViolationEntry> checkMeta = new LinkedHashMap<>();
        long bucketSize = Math.max(1, d.bucketSizeMs());
        for (ViolationEntry v : survivors) {
            long bucketStart = (v.offsetFromSessionStartMs() / bucketSize) * bucketSize;
            agg.computeIfAbsent(bucketStart, k -> new LinkedHashMap<>())
                    .computeIfAbsent(v.displayName(), k -> new int[]{0})[0]++;
            checkMeta.putIfAbsent(v.displayName(), v);
        }
        List<CheckBucket> newBuckets = new ArrayList<>(agg.size());
        for (Map.Entry<Long, Map<String, int[]>> e : agg.entrySet()) {
            List<CheckCount> ccs = new ArrayList<>(e.getValue().size());
            for (Map.Entry<String, int[]> ce : e.getValue().entrySet()) {
                ViolationEntry meta = checkMeta.get(ce.getKey());
                ccs.add(new CheckCount(meta.checkId(), meta.stableKey(),
                        meta.displayName(), meta.description(), ce.getValue()[0]));
            }
            newBuckets.add(new CheckBucket(e.getKey(), ccs));
        }
        int uniqueChecks = checkMeta.size();
        return new SessionDetail(
                d.sessionId(), d.playerUuid(), d.sessionOrdinal(),
                d.startedEpochMs(), d.lastActivityEpochMs(),
                d.grimVersion(), d.serverName(), d.clientVersion(), d.clientBrand(),
                d.bucketSizeMs(), uniqueChecks, newBuckets, survivors);
    }

    // ---- helpers ----

    private static RenderedTemplate parse(Sender sender, ConfigManager cfg, String key, String fallback,
                                          Map<String, TemplateValue> vars) {
        String raw = cfg.getStringElse(key, fallback);
        return new RenderedTemplate(parseRaw(sender, raw, vars), raw);
    }

    private static Component parseRaw(Sender sender, String raw, Map<String, TemplateValue> vars) {
        TagResolver.Builder resolver = TagResolver.builder();
        for (Map.Entry<String, TemplateValue> e : vars.entrySet()) {
            TemplateValue value = e.getValue();
            if (value.component() == null) {
                raw = raw.replace("%" + e.getKey() + "%", value.raw());
            } else {
                raw = raw.replace("%" + e.getKey() + "%", "<" + e.getKey() + ">");
                resolver.resolver(Placeholder.component(e.getKey(), value.component()));
            }
        }
        raw = MessageUtil.replacePlaceholders(sender, raw);
        return MessageUtil.miniMessage(raw, resolver.build());
    }

    private static boolean hasInlineHover(String raw) {
        return raw.toLowerCase(Locale.ROOT).contains("<hover:");
    }

    /** Renders untrusted template values as plain text before MiniMessage parsing. */
    private static String mmSafe(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // Strip '%' first so '&%c' cannot become '&c' after placeholder removal.
        String stripped = LEGACY_FORMAT_PATTERN.matcher(
                raw.replace("%", "")).replaceAll("");
        // Escape backslash before '<' so the added escape isn't itself escaped.
        return stripped
                .replace("\\", "\\\\")
                .replace("<", "\\<");
    }

    // Canonical legacy alphabet only — leaves AT&T / R&D alone.
    // Covers §/& sigils, &#RRGGBB hex, and the Bedrock &x&R&R… interleave.
    private static final Pattern LEGACY_FORMAT_PATTERN = Pattern.compile(
            "[§&](?:[0-9a-fk-orxA-FK-ORX]|#[A-Fa-f0-9]{6}|x(?:[§&][A-Fa-f0-9]){6})");

    public static @NotNull String formatDuration(long ms) {
        if (ms < 0) ms = 0;
        long days = TimeUnit.MILLISECONDS.toDays(ms); ms -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(ms); ms -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms); ms -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    public static @NotNull String formatAbsolute(long epochMs) {
        return new Date(epochMs).toString();
    }

    private static @NotNull String nullToUnknown(@Nullable String s) {
        return s == null ? "unknown" : s;
    }

    static @NotNull String clientVersionDisplay(int pvn) {
        if (pvn <= 0) return "unknown";
        try {
            ClientVersion cv = ClientVersion.getById(pvn);
            if (cv == null || cv == ClientVersion.UNKNOWN) return "unknown";
            String name = cv.getReleaseName();
            return name == null ? ("pvn:" + pvn) : name;
        } catch (RuntimeException ignore) {
            return "pvn:" + pvn;
        }
    }

}
