package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class MessageUtil {
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + '§' + "[0-9A-FK-ORX]");
    private final Pattern HEX_PATTERN = Pattern.compile("([&§]#[A-Fa-f0-9]{6})|([&§]x([&§][A-Fa-f0-9]){6})");
    private final char PLACEHOLDER_ESCAPE_CHAR = '\uFFFF'; // this specific character holds no significance

    public @NotNull String toUnlabledString(@Nullable Vector3i vec) {
        return vec == null ? "null" : vec.x + ", " + vec.y + ", " + vec.z;
    }

    public @NotNull String toUnlabledString(@Nullable Vector3f vec) {
        return vec == null ? "null" : vec.x + ", " + vec.y + ", " + vec.z;
    }

    @Contract("_, null, _ -> null; _, !null, _ -> !null")
    public @Nullable String replacePlaceholders(@Nullable GrimPlayer player, @Nullable String string, boolean removeFormatting) {
        return replacePlaceholders(player, player == null ? null : player.platformPlayer, string, removeFormatting);
    }

    @Contract("_, null -> null; _, !null -> !null")
    public @Nullable String replacePlaceholders(@Nullable GrimPlayer player, @Nullable String string) {
        return replacePlaceholders(player, player == null ? null : player.platformPlayer, string, false);
    }

    @Contract("_, null -> null; _, !null -> !null")
    public @Nullable String replacePlaceholders(@Nullable Sender sender, @Nullable String string) {
        return replacePlaceholders(sender != null ? sender.getPlatformPlayer() : null, string);
    }

    @Contract("_, null -> null; _, !null -> !null")
    public @Nullable String replacePlaceholders(@Nullable PlatformPlayer player, @Nullable String string) {
        return replacePlaceholders(player == null ? null : GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player.getUniqueId()), player, string, false);
    }

    // NOTE: This pattern intentionally matches standard placeholders (Alphanumeric + Underscore).
    // Complex PAPI placeholders containing symbols like '<' or '>' (e.g., %img_<id>%) will NOT match.
    // This is acceptable: complex placeholders are skipped by this loop and handled raw by PAPI later.
    private static final Pattern UNIFIED_PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)%");

    /**
     * Replaces placeholders using a hybrid performance strategy: Grim-internal lookups first, then PAPI.
     * <p>
     * <b>Strategy:</b>
     * <ol>
     *     <li><b>Fast Path:</b> Immediate return if no {@code %} is present.</li>
     *     <li><b>Grim Lookup:</b> Checks internal maps (O(1)). If found, the value is calculated and (optionally) filtered.</li>
     *     <li><b>PAPI Fallback:</b> If the placeholder is unknown to Grim, it is left <b>raw</b> (e.g., {@code %player_name%}).
     *     This allows the external PAPI hook to process it after this method returns.</li>
     * </ol>
     *
     * <b>Discord Formatting (when removeFormatting is true):</b>
     * <ul>
     *     <li><b>Standard:</b> Values are fully escaped (e.g., {@code _User_} -> {@code \_User\_}) to prevent accidental formatting.</li>
     *     <li><b>Code Blocks:</b> If the placeholder is wrapped in backticks (e.g., {@code `%player%`}),
     *     only backticks/backslashes are escaped. This preserves the raw data look.</li>
     *     <li><b>Caveat:</b> PAPI results are <b>NOT</b> filtered. If a PAPI placeholder returns an underscore, it may cause formatting issues.</li>
     * </ul>
     *
     * @param grimPlayer       The Grim user instance (nullable).
     * @param platformPlayer   The Platform player instance (nullable).
     * @param string           The raw message string.
     * @param removeFormatting Whether to escape Markdown characters (used for Discord).
     * @return The processed string with Grim placeholders filled and PAPI placeholders pending.
     */
    @Contract("_, _, null, _ -> null; _, _, !null, _ -> !null")
    private @Nullable String replacePlaceholders(@Nullable GrimPlayer grimPlayer, @Nullable PlatformPlayer platformPlayer, @Nullable String string, boolean removeFormatting) {
        if (string == null) return null;

        // --- PHASE 1: FAST PATH ---
        // JVM Intrinsic: Scanning for a char is significantly faster than initializing a Regex Matcher.
        // If there is no '%', we can skip all allocation.
        if (string.indexOf('%') == -1) {
            return string;
        }

        final Matcher matcher = UNIFIED_PLACEHOLDER_PATTERN.matcher(string);

        // If '%' exists but doesn't form a valid simple placeholder, skip to the PAPI/Legacy handler.
        // This avoids allocating the StringBuilder below.
        if (!matcher.find()) {
            return GrimAPI.INSTANCE.getMessagePlaceHolderManager().replacePlaceholders(platformPlayer, string);
        }

        final Map<String, String> staticReplacements = GrimAPI.INSTANCE.getExternalAPI().getStaticReplacements();
        final Map<String, Function<GrimUser, String>> variableReplacements = GrimAPI.INSTANCE.getExternalAPI().getVariableReplacements();

        // 32 is a heuristic buffer. It roughly covers the expansion cost of one UUID (36 chars) vs one placeholder (6 chars).
        final StringBuilder sb = new StringBuilder(string.length() + 32);

        // --- PHASE 2: THE REPLACEMENT LOOP ---
        // Used do-while because the first `matcher.find()` was already called above.
        do {
            final String keyWithPercent = matcher.group(0);
            String value = staticReplacements.get(keyWithPercent);

            // Lazy Evaluation: Only check dynamic placeholders if static failed.
            // Only run the function (e.g. TPS calc) if the placeholder is actually present.
            if (value == null && grimPlayer != null) {
                final Function<GrimUser, String> func = variableReplacements.get(keyWithPercent);
                if (func != null) {
                    value = func.apply(grimPlayer);
                }
            }

            // --- PHASE 3: HANDLING & FORMATTING ---
            if (value != null) {
                // CASE A: Grim Placeholder Found
                if (removeFormatting) {
                    // Check if the user wrapped this placeholder in code blocks (e.g. ` %player% `)
                    boolean insideCodeBlock = false;
                    if (matcher.start() > 0 && matcher.end() < string.length()) {
                        char charBefore = string.charAt(matcher.start() - 1);
                        char charAfter = string.charAt(matcher.end());
                        if (charBefore == '`' && charAfter == '`') {
                            insideCodeBlock = true;
                        }
                    }

                    if (insideCodeBlock) {
                        // Context: Inside Code Block (`...`).
                        // Action: Escape backticks/backslashes to prevent breaking out. Leave underscores/markdown raw.
                        value = value.replace("\\", "\\\\").replace("`", "\\`");
                    } else {
                        // Context: Standard Text.
                        // Action: Aggressively escape all Markdown characters.
                        value = filterDiscordText(value);
                    }
                }
            } else {
                // CASE B: Unknown Placeholder (Likely PAPI)
                // Action: Restore the original key (e.g. "%player_name%") so PAPI can process it later.
                // CRITICAL: Do NOT run filterDiscordText() here. It would escape the '%' or '_' in the key,
                // breaking PAPI syntax (e.g., "%player\_name%" is invalid PAPI).
                value = keyWithPercent;
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));

        } while (matcher.find());

        matcher.appendTail(sb);

        String grimReplaced = sb.toString();

        // Final Pass: Hand off to platform-specific handler (PAPI)
        return GrimAPI.INSTANCE.getMessagePlaceHolderManager().replacePlaceholders(platformPlayer, grimReplaced).replace(PLACEHOLDER_ESCAPE_CHAR, '%');
    }

    public static String filterDiscordText(String message) {
        if (message == null || message.isBlank()) return message;
        final StringBuilder sb = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); ++i) {
            final char c = message.charAt(i);
            // Escape a newline
            if (c == '\n') {
                sb.append("\\n");
            }  // Escape Markdown special characters
            else if (c == '`' || c == '*' || c == '_' || c == '~' || c == '|') {
                sb.append('\\').append(c);
            } else {
                // Escape "# ", "> ", etc
                if (c == '#' || c == '>' || c == '-') {
                    // check if there's a space next
                    if (((i + 1 < message.length()) && (message.charAt(i + 1) == ' '))
                            && ((i == 0) || (message.charAt(i - 1) == '\n'))) {
                        sb.append("\\").append(c);
                    } else {
                        sb.append(c);
                    }
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public @NotNull Component replacePlaceholders(@NotNull GrimPlayer player, @NotNull Component component) {
        // Replacement config that forces any placeholder replacement to be pure text
        final TextReplacementConfig safeReplacement = TextReplacementConfig.builder()
                .match("%[a-zA-Z0-9_]+%") // Match placeholders
                .replacement(placeholder -> Component.text(replacePlaceholders(player, placeholder.content())))
                .build();
        return component.replaceText(safeReplacement);
    }

    public @NotNull Component miniMessage(@NotNull String string) {
        string = string.replace("%prefix%", GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("prefix", "&bGrim &8»"));

        // hex codes
        Matcher matcher = HEX_PATTERN.matcher(string);
        StringBuilder sb = new StringBuilder(string.length());

        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(0).replaceAll("[&§#x]", "") + ">");
        }

        string = matcher.appendTail(sb).toString();

        // MiniMessage doesn't like legacy formatting codes
        string = translateAlternateColorCodes('&', string)
                .replace("§0", "<!b><!i><!u><!st><!obf><black>")
                .replace("§1", "<!b><!i><!u><!st><!obf><dark_blue>")
                .replace("§2", "<!b><!i><!u><!st><!obf><dark_green>")
                .replace("§3", "<!b><!i><!u><!st><!obf><dark_aqua>")
                .replace("§4", "<!b><!i><!u><!st><!obf><dark_red>")
                .replace("§5", "<!b><!i><!u><!st><!obf><dark_purple>")
                .replace("§6", "<!b><!i><!u><!st><!obf><gold>")
                .replace("§7", "<!b><!i><!u><!st><!obf><gray>")
                .replace("§8", "<!b><!i><!u><!st><!obf><dark_gray>")
                .replace("§9", "<!b><!i><!u><!st><!obf><blue>")
                .replace("§a", "<!b><!i><!u><!st><!obf><green>")
                .replace("§b", "<!b><!i><!u><!st><!obf><aqua>")
                .replace("§c", "<!b><!i><!u><!st><!obf><red>")
                .replace("§d", "<!b><!i><!u><!st><!obf><light_purple>")
                .replace("§e", "<!b><!i><!u><!st><!obf><yellow>")
                .replace("§f", "<!b><!i><!u><!st><!obf><white>")
                .replace("§r", "<reset>")
                .replace("§k", "<obfuscated>")
                .replace("§l", "<bold>")
                .replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>")
                .replace("§o", "<italic>");

        return MiniMessage.miniMessage().deserialize(string).compact();
    }

    public Component getParsedComponent(Sender sender, String key, String fallbackText) {
        String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse(key, fallbackText);
        message = MessageUtil.replacePlaceholders(sender, message);
        return MessageUtil.miniMessage(message);
    }

    @Contract("_, _ -> new")
    public static @NotNull String translateAlternateColorCodes(char altColorChar, @NotNull String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for (int i = 0; i < b.length - 1; ++i) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = 167;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    @Contract("!null -> !null; null -> null")
    public static @Nullable String stripColor(@Nullable String input) {
        return input == null ? null : STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }
}
