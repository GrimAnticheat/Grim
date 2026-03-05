package ac.grim.grimac.utils.discord;

import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.Nullable;

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
     * @param externalResolver    PAPI resolver: key → resolved string, or key unchanged if unresolved (nullable)
     */
    public String render(@Nullable GrimPlayer player,
                         Map<String, String> statics,
                         Map<String, Function<GrimUser, String>> dynamics,
                         char backtickReplacement,
                         @Nullable Function<String, String> externalResolver) {
        StringBuilder sb = new StringBuilder(segments.length * 32);
        for (Segment seg : segments) {
            if (seg instanceof Literal) {
                sb.append(((Literal) seg).text);
            } else if (seg instanceof Placeholder) {
                Placeholder p = (Placeholder) seg;

                // Priority: static → dynamic → external (PAPI)
                String val = statics.get(p.key);

                if (val == null && player != null) {
                    Function<GrimUser, String> fn = dynamics.get(p.key);
                    if (fn != null) val = fn.apply(player);
                }

                if (val == null && externalResolver != null) {
                    String resolved = externalResolver.apply(p.key);
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
        if (mode == EscapeMode.CODE_SPAN) {
            return escapeCodeSpan(value, backtickReplacement);
        }
        return escapeMarkdown(value);
    }

    /** Full Discord Markdown escaping. Backslashes first to prevent double-escaping. */
    public static String escapeMarkdown(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '`' -> sb.append("\\`");
                case '*' -> sb.append("\\*");
                case '_' -> sb.append("\\_");
                case '~' -> sb.append("\\~");
                case '|' -> sb.append("\\|");
                case '[' -> sb.append("\\[");
                case ']' -> sb.append("\\]");
                case '(' -> sb.append("\\(");
                case ')' -> sb.append("\\)");
                case '\n' -> sb.append("\\n");
                case '#', '>', '-' -> {
                    // These are only special at the start of a line followed by a space
                    boolean lineStart = (i == 0) || (s.charAt(i - 1) == '\n');
                    boolean spaceAfter = (i + 1 < s.length()) && (s.charAt(i + 1) == ' ');
                    if (lineStart && spaceAfter) sb.append('\\');
                    sb.append(c);
                }
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
     * Advances Markdown context through literal text.
     * Handles: backslash escapes in NORMAL, single-` inline code, triple-` code blocks.
     *
     * Edge cases resolved:
     *   `text %p% text`         → INLINE_CODE (` opens)
     *   `a`%p%`b`              → NORMAL      (` opens, ` closes before %p%)
     *   \`%p%`                  → NORMAL      (` is backslash-escaped)
     */
    private static MarkdownContext advanceContext(MarkdownContext ctx, String text) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            switch (ctx) {
                case NORMAL -> {
                    if (c == '\\' && i + 1 < text.length()) {
                        i += 2;  // skip escaped char entirely
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
                }
                case INLINE_CODE -> {
                    // No escape sequences inside code spans — backtick always closes
                    if (c == '`') ctx = MarkdownContext.NORMAL;
                }
                case CODE_BLOCK -> {
                    if (c == '`'
                            && i + 2 < text.length()
                            && text.charAt(i + 1) == '`'
                            && text.charAt(i + 2) == '`') {
                        ctx = MarkdownContext.NORMAL;
                        i += 3;
                        continue;
                    }
                }
            }
            i++;
        }
        return ctx;
    }
}
