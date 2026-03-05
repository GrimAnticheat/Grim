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

    sealed

    interface Segment permits Literal, Placeholder {
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
     * Resolves all known placeholders with context-appropriate escaping.
     * Unknown placeholders are left raw so PAPI (or similar) can handle them later.
     */
    public String render(@Nullable GrimPlayer player,
                         Map<String, String> staticReplacements,
                         Map<String, Function<GrimUser, String>> dynamicReplacements) {
        StringBuilder sb = new StringBuilder(segments.length * 24);
        for (Segment seg : segments) {
            if (seg instanceof Literal l) {
                sb.append(l.text());
            } else if (seg instanceof Placeholder p) {
                // Lazy, prioritized lookup — same semantics as the current code
                String val = staticReplacements.get(p.key());
                if (val == null && player != null) {
                    Function<GrimUser, String> fn = dynamicReplacements.get(p.key());
                    if (fn != null) val = fn.apply(player);
                }
                if (val != null) {
                    sb.append(escape(val, p.mode()));   // Grim value → escape
                } else {
                    sb.append(p.key());                  // unknown → leave raw for PAPI
                }
            }
        }
        return sb.toString();
    }

    // ──────────────────── ESCAPE ────────────────────
    private static String escape(String value, EscapeMode mode) {
        return switch (mode) {
            case FULL_MARKDOWN -> escapeMarkdown(value);
            case CODE_SPAN -> escapeCodeSpan(value);
            case NONE -> value;
        };
    }

    /**
     * Escapes all Discord Markdown metacharacters.
     * NOTE: Backslash MUST be escaped first to avoid double-escaping.
     */
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
     * Escapes content destined for inside a Discord code span.
     * <p>
     * Discord does NOT support backslash escaping inside code spans.
     * A backtick inside `...` unconditionally closes the span.
     * The only safe option is to substitute backticks with a
     * visually-similar Unicode character (modifier letter grave accent).
     */
    public static String escapeCodeSpan(String s) {
        if (s == null || s.isEmpty()) return s;
        // U+02CB MODIFIER LETTER GRAVE ACCENT — visually similar to backtick
        return s.replace('`', '\u02CB');
    }

    // ──────────────────── STATE MACHINE ────────────────────

    /**
     * Advances the Markdown context through a chunk of literal text.
     * Handles: single-backtick code spans, triple-backtick code blocks,
     * and backslash-escaping in NORMAL context.
     * 1. `text %placeholder% text`          → INLINE_CODE
     * 2. `a`%placeholder%`b`                → NORMAL       (first ` opens, second ` closes)
     * 3. \`%placeholder%`                   → NORMAL       (backtick is escaped)
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
