package ac.grim.grimac.utils.data.webhook.discord;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record CompiledDiscordTemplate(Segment[] segments) {

    /**
     * Markdown context as determined by a state-machine scan of the template.
     * Used only at compile time.
     */
    private enum MarkdownContext {
        NORMAL,
        INLINE_CODE,
        CODE_BLOCK
    }

    /**
     * Escape strategy assigned to each placeholder at compile time.
     */
    public enum EscapeMode {
        /**
         * Full Markdown escaping: \, `, *, _, ~, |, [], (), line-start chars, newlines
         */
        FULL_MARKDOWN,
        /**
         * Inside a code span: only backtick substitution (backslash escaping doesn't work in Discord code spans)
         */
        CODE_SPAN,
        /**
         * No escaping (placeholder left raw for PAPI)
         */
        NONE
    }

    // Identical to the existing pattern
    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_]+)%");

    sealed interface Segment permits Literal, Placeholder {
    }

    record Literal(String text) implements Segment {
    }

    record Placeholder(String key, EscapeMode mode) implements Segment {
    }

    // ──────────────────── COMPILE (once per config reload) ────────────────────
    public static CompiledDiscordTemplate compile(String template) {
        List<Segment> parts = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(template);
        MarkdownContext ctx = MarkdownContext.NORMAL;
        int lastEnd = 0;

        while (m.find()) {
            String gap = template.substring(lastEnd, m.start());
            if (!gap.isEmpty()) parts.add(new Literal(gap));

            // Advance the context through all the literal text before this placeholder
            ctx = advanceContext(ctx, gap);

            EscapeMode mode = switch (ctx) {
                case NORMAL -> EscapeMode.FULL_MARKDOWN;
                case INLINE_CODE,
                     CODE_BLOCK -> EscapeMode.CODE_SPAN;
            };
            parts.add(new Placeholder(m.group(0), mode));
            lastEnd = m.end();
        }

        if (lastEnd < template.length()) {
            parts.add(new Literal(template.substring(lastEnd)));
        }
        return new CompiledDiscordTemplate(parts.toArray(Segment[]::new));
    }

    // ──────────────────── RENDER (once per alert) ────────────────────
    /**
     * @param player              Grim player (nullable)
     * @param statics             Static replacements (may include per-alert overrides like %check%)
     * @param dynamics            Dynamic replacements (lazy functions like %tps%)
     * @param backtickReplacement Char to substitute for backticks inside code spans (loaded from config)
     */
    public String render(@NotNull GrimPlayer player,
                         @NotNull Map<String, String> statics,
                         @NotNull Map<String, Function<GrimUser, String>> dynamics,
                         char backtickReplacement) {
        StringBuilder sb = new StringBuilder(segments.length * 32);
        for (Segment seg : segments) {
            if (seg instanceof Literal l) {
                sb.append(l.text);
            } else if (seg instanceof Placeholder p) {
                // Priority: static → dynamic → external (PAPI)
                String val = statics.get(p.key);

                if (val == null) {
                    Function<GrimUser, String> fn = dynamics.get(p.key);
                    if (fn != null) val = fn.apply(player);
                }

                if (val == null) {
                    String resolved = GrimAPI.INSTANCE.getMessagePlaceHolderManager()
                            .replacePlaceholders(player.platformPlayer, p.key);
                    if (!resolved.equals(p.key)) val = resolved;
                }

                if (val != null) {
                    sb.append(escape(val, p.mode, backtickReplacement));
                } else {
                    sb.append(p.key); // truly unresolved — leave raw
                }
            }
        }
        return sb.toString();
    }

    private static String escape(String value, EscapeMode mode, char backtickReplacement) {
        if (mode == EscapeMode.NONE) return value;
        if (mode == EscapeMode.CODE_SPAN) return escapeCodeSpan(value, backtickReplacement);
        return escapeMarkdown(value);
    }

    /**
     * Full Discord Markdown escaping for untrusted data values.
     * <p>
     * This method treats input as <b>raw data</b>, not pre-formatted markdown.
     * A literal backslash in a player name is data and must render as a visible backslash.
     * This means "already escaped" input like {@code \*} correctly becomes {@code \\*}
     * (rendering as {@code \*}), which is the intended behavior — identical to SQL parameterization.
     * <p>
     * All escaped characters ({@code \X}) render identically to their unescaped form ({@code X})
     * in Discord's CommonMark variant, so escaping is invisible to end users.
     */
    public static String escapeMarkdown(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                // Backslash MUST be first — prevents our own escape backslashes
                // from being re-escaped if the input already contains backslashes.
                case '\\' -> sb.append("\\\\");
                // Inline code spans (`text`)
                case '`' -> sb.append("\\`");
                // Bold (**text**) and italic (*text*)
                case '*' -> sb.append("\\*");
                // Underlined  (__text__) and italic (_text_)
                case '_' -> sb.append("\\_");
                // Strikethrough (~~text~~)
                case '~' -> sb.append("\\~");
                // Spoiler tags (||text||)
                case '|' -> sb.append("\\|");
                // Link [text](url) and image ![alt](url) syntax
                case '[' -> sb.append("\\[");
                case ']' -> sb.append("\\]");
                case '(' -> sb.append("\\(");
                case ')' -> sb.append("\\)");
                // Auto-linking (https://...) and emoji (:name:)
                case ':' -> sb.append("\\:");
                // Timestamps (<t:...>), mentions (<@id>), custom emoji (<:n:id>),
                // embed suppression (<url>)
                case '<' -> sb.append("\\<");
                // Headers (#, ##, ###), block quotes (>, >>>).
                // Always escaped — \# and \> render identically to # and >.
                case '#' -> sb.append("\\#");
                case '>' -> sb.append("\\>");
                // Unordered lists (- item) and subtext (-# text)
                case '-' -> sb.append("\\-");
                // Ordered lists (1. item). \. renders identically to .
                case '.' -> sb.append("\\.");
                // Newlines in injected values would break embed layout and enable
                // line-start syntax injection (headers, quotes, lists).
                // Replaced with literal "\n" text. Template newlines are unaffected
                // (they pass through as Literal segments, not through this method).
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Code span escaping: Discord ignores backslash escapes inside code spans,
     * so backticks are replaced with a visually-similar substitute character.
     * If replacement == '`', this is a no-op (user disabled the feature).
     */
    public static String escapeCodeSpan(String s, char replacement) {
        if (s == null || s.isEmpty() || replacement == '`') return s;
        return s.replace('`', replacement);
    }

    // ──────────────────── STATE MACHINE ────────────────────

    /**
     * Advances Markdown context through literal template text.
     * Handles: backslash escapes (NORMAL only), single-` inline code, triple-` code blocks.
     * <p>
     * Edge cases resolved:
     * <pre>
     *   `text %p% text`  → INLINE_CODE  (` opens before placeholder)
     *   `a`%p%`b`        → NORMAL       (` opens then ` closes before placeholder)
     *   \`%p%`           → NORMAL       (backtick is backslash-escaped)
     * </pre>
     * <b>Known limitation:</b> Double-backtick code spans ({@code `` text ``}) are not tracked.
     */
    private static MarkdownContext advanceContext(MarkdownContext ctx, String text) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (ctx == MarkdownContext.NORMAL) {
                if (c == '\\' && i + 1 < text.length()) {
                    i += 2; // skip escaped char entirely
                    continue;
                }
                if (c == '`') {
                    if (i + 2 < text.length()
                            && text.charAt(i + 1) == '`'
                            && text.charAt(i + 2) == '`') {
                        ctx = MarkdownContext.CODE_BLOCK;
                        i += 3;
                        continue;
                    }
                    ctx = MarkdownContext.INLINE_CODE;
                }
            } else if (ctx == MarkdownContext.INLINE_CODE) {
                if (c == '`') ctx = MarkdownContext.NORMAL;
            } else { // CODE_BLOCK
                if (c == '`'
                        && i + 2 < text.length()
                        && text.charAt(i + 1) == '`'
                        && text.charAt(i + 2) == '`') {
                    ctx = MarkdownContext.NORMAL;
                    i += 3;
                    continue;
                }
            }
            i++;
        }
        return ctx;
    }
}
