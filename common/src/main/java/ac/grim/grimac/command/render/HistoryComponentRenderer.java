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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Converts {@link SessionSummary} / {@link SessionDetail} records returned by
 * {@link ac.grim.grimac.api.storage.history.HistoryService} into Adventure
 * {@link Component} trees for the plugin's command output. All presentation
 * text lives in {@code messages.yml} (see the {@code grim-history-*} keys),
 * so operators can recolour / reword the whole UI without touching code.
 * Hover tooltips are attached here directly from the raw data (verbose
 * strings, description one-liners, grouped check breakdowns).
 * <p>
 * The {@code grim-history-detail-entry} template accepts an optional
 * {@code %description%} variable — include it in your messages.yml when
 * you want the check's short description inlined in detailed-mode rows.
 * The default template omits it (to keep rows narrow); hover always
 * carries the description when the check has one declared.
 */
public final class HistoryComponentRenderer {

    private HistoryComponentRenderer() {}

    /**
     * Session-list view. {@code page} is 1-indexed; {@code maxPages} ≥ 1.
     * {@code ongoingSessionId} (optional) is the player's currently-active
     * sessionId from SessionTracker; the matching row shows "current" as its
     * duration.
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
        if (result.items().isEmpty()) {
            return List.of(parse(sender, cfg, "grim-history-no-sessions",
                    "%prefix% &7No session history for &f%player%&7.",
                    Map.of("player", playerDisplayName)));
        }
        List<Component> out = new ArrayList<>(result.items().size() + 1);
        // Emit both %max_pages% (new) and %maxPages% (pre-cutover spelling) so
        // operators upgrading from 1.x with a legacy grim-history-header that
        // still references %maxPages% see a correctly-rendered count.
        String maxPagesStr = Integer.toString(Math.max(1, maxPages));
        out.add(parse(sender, cfg, "grim-history-header",
                "%prefix% &bShowing session history for &f%player% &8[&f%page%&7/&f%max_pages%&8]",
                Map.of(
                        "player", playerDisplayName,
                        "page", Integer.toString(page),
                        "max_pages", maxPagesStr,
                        "maxPages", maxPagesStr)));
        for (SessionSummary s : result.items()) {
            boolean ongoing = ongoingSessionId != null && ongoingSessionId.equals(s.sessionId());
            out.add(renderSummaryLine(sender, cfg, s, ongoing));
        }
        return out;
    }

    private static Component renderSummaryLine(Sender sender, ConfigManager cfg, SessionSummary s, boolean ongoing) {
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
        Component line = parse(sender, cfg, "grim-history-session",
                "%prefix% &8[&b%grim_version%&8] &8[&b%server_name%&8] &8[&b%client_version%&8]"
                        + " &bSession &f%ordinal%&b duration &f%duration%&b with &c%violations%&b"
                        + " violations &8[&c%unique_checks%&8]%crashed_marker% &8(&7%timeago% ago&8)",
                Map.ofEntries(
                        Map.entry("grim_version", nullToUnknown(s.grimVersion())),
                        Map.entry("server_name", nullToUnknown(s.serverName())),
                        Map.entry("client_version", clientVersionDisplay(s.clientVersion())),
                        Map.entry("client_brand", nullToUnknown(s.clientBrand())),
                        Map.entry("ordinal", Integer.toString(s.sessionOrdinal())),
                        Map.entry("duration", durationText),
                        Map.entry("violations", Long.toString(s.violationCount())),
                        Map.entry("unique_checks", Integer.toString(s.uniqueCheckCount())),
                        Map.entry("crashed_marker", crashedMarker),
                        Map.entry("timeago", formatDuration(elapsedNow))));
        // Hover tooltip — click-hint plus raw session metadata that doesn't fit on the line.
        Component tooltip = Component.text()
                .append(Component.text("Session ", NamedTextColor.AQUA))
                .append(Component.text(s.sessionId().toString(), NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Click (when supported) or run ", NamedTextColor.GRAY))
                .append(Component.text("/grim history " + shortName(s.playerUuid()) + " session " + s.sessionOrdinal(),
                        NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("to view session details.", NamedTextColor.GRAY))
                .build();
        return line.hoverEvent(HoverEvent.showText(tooltip));
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

        Map<String, String> metaVars = Map.ofEntries(
                Map.entry("player", playerDisplayName),
                Map.entry("ordinal", Integer.toString(d.sessionOrdinal())),
                Map.entry("grim_version", nullToUnknown(d.grimVersion())),
                Map.entry("server_name", nullToUnknown(d.serverName())),
                Map.entry("client_version", clientVersionDisplay(d.clientVersion())),
                Map.entry("client_brand", nullToUnknown(d.clientBrand())),
                Map.entry("duration", durationText),
                Map.entry("timeago", formatDuration(elapsedNow)),
                Map.entry("violations", Integer.toString(d.violations().size())),
                Map.entry("unique_checks", Integer.toString(d.uniqueCheckCount())),
                Map.entry("bucket_size", formatDuration(d.bucketSizeMs())),
                Map.entry("page", Integer.toString(page)),
                Map.entry("max_pages", Integer.toString(maxPages)));
        out.add(parse(sender, cfg, "grim-history-detail-header",
                "%prefix% &bShowing &f%player%&b's session &f%ordinal%&b details:", metaVars));
        out.add(parse(sender, cfg, "grim-history-detail-meta1",
                "%prefix% &bGrim: &f%grim_version%&b, Server: &f%server_name%&b, Duration: &f%duration%&b, Date: &7%timeago% ago",
                metaVars));
        out.add(parse(sender, cfg, "grim-history-detail-meta2",
                "%prefix% &bClient: &f%client_version%&b, Brand: &f%client_brand%",
                metaVars));
        out.add(parse(sender, cfg, "grim-history-detail-violations-header",
                "%prefix% &bViolations: &8(%violations% total, %unique_checks% unique) &8[&f%page%&7/&f%max_pages%&8]",
                metaVars));

        if (d.violations().isEmpty()) {
            out.add(parse(sender, cfg, "grim-history-detail-empty", "%prefix% &7- (none)", Map.of()));
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
            checksList.append(cfg.getStringElse("grim-history-check-count",
                            "&f%check_name%&7 x&c%count%")
                    .replace("%check_name%", c.displayName())
                    .replace("%count%", Integer.toString(c.count())));
        }
        Component line = parse(sender, cfg, "grim-history-detail-group",
                "%prefix% &7- %checks_list% &8(&b%offset%&8)",
                Map.of(
                        "checks_list", checksList.toString(),
                        "offset", formatDuration(bucket.bucketStartOffsetMs())));
        // Hover: full breakdown of violations in this bucket, with verbose.
        Component tooltip = buildBucketHover(d, bucket, verbose);
        return line.hoverEvent(HoverEvent.showText(tooltip));
    }

    private static Component buildBucketHover(SessionDetail d, CheckBucket bucket, boolean verbose) {
        long bucketStart = bucket.bucketStartOffsetMs();
        long bucketEnd = bucketStart + d.bucketSizeMs();
        Component header = Component.text()
                .append(Component.text("Violations in ", NamedTextColor.AQUA))
                .append(Component.text(formatDuration(bucketStart) + "–" + formatDuration(bucketEnd),
                        NamedTextColor.WHITE))
                .append(Component.text(":", NamedTextColor.AQUA))
                .build();
        Component body = Component.empty();
        for (ViolationEntry v : d.violations()) {
            if (v.offsetFromSessionStartMs() < bucketStart || v.offsetFromSessionStartMs() >= bucketEnd) continue;
            Component line = Component.newline()
                    .append(Component.text("  " + formatDuration(v.offsetFromSessionStartMs()) + " ",
                            NamedTextColor.DARK_GRAY))
                    .append(Component.text(v.displayName(), NamedTextColor.AQUA));
            if (!v.description().isBlank()) {
                line = line.append(Component.text(" — " + v.description(), NamedTextColor.WHITE));
            }
            if (verbose && v.verbose() != null && !v.verbose().isBlank()) {
                line = line.append(Component.text(" — " + v.verbose(), NamedTextColor.GRAY));
            }
            body = body.append(line);
        }
        return header.append(body);
    }

    private static Component renderViolationLine(Sender sender, ConfigManager cfg, ViolationEntry v, boolean verbose) {
        String verboseText = v.verbose() == null ? "" : v.verbose();
        Component line = parse(sender, cfg, "grim-history-detail-entry",
                "%prefix% &7- &f%check% &8(&b%offset%&8)&7 %verbose%",
                Map.of(
                        "check", v.displayName(),
                        "description", v.description(),
                        "offset", formatDuration(v.offsetFromSessionStartMs()),
                        "vl", Double.toString(v.vl()),
                        "verbose", verbose ? verboseText : ""));
        // Hover carries the richer disambiguation — description on its own
        // line, then the raw verbose below. Shown regardless of the -v
        // flag, because operators scanning a dense list still want the
        // quick "what does this check mean" answer without re-running.
        boolean hasDescription = !v.description().isBlank();
        if (!verboseText.isBlank() || hasDescription) {
            var tooltip = Component.text()
                    .append(Component.text(v.displayName(), NamedTextColor.AQUA));
            if (hasDescription) {
                tooltip.append(Component.text(" — " + v.description(), NamedTextColor.WHITE));
            }
            tooltip.append(Component.newline())
                    .append(Component.text("@ " + formatDuration(v.offsetFromSessionStartMs()) + " — vl " + v.vl(),
                            NamedTextColor.DARK_GRAY));
            if (!verboseText.isBlank()) {
                tooltip.append(Component.newline())
                        .append(Component.text(verboseText, NamedTextColor.GRAY));
            }
            line = line.hoverEvent(HoverEvent.showText(tooltip.build()));
        }
        return line;
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

    private static Component parse(Sender sender, ConfigManager cfg, String key, String fallback,
                                   Map<String, String> vars) {
        String raw = cfg.getStringElse(key, fallback);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            raw = raw.replace("%" + e.getKey() + "%", e.getValue());
        }
        raw = MessageUtil.replacePlaceholders(sender, raw);
        return MessageUtil.miniMessage(raw);
    }

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

    private static String shortName(UUID u) {
        return u.toString().substring(0, 8);
    }
}
