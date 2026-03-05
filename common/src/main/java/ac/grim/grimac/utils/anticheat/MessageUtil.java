package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.discord.CompiledDiscordTemplate;
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

    private static final Pattern UNIFIED_PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)%");

    @Contract("_, _, null, _ -> null; _, _, !null, _ -> !null")
    private @Nullable String replacePlaceholders(@Nullable GrimPlayer grimPlayer, @Nullable PlatformPlayer platformPlayer, @Nullable String string, boolean removeFormatting) {
        if (string == null) return null;

        // --- PHASE 1: FAST PATH ---
        // JVM Intrinsic: Scanning for a char is significantly faster than initializing a Regex Matcher.
        // If there is no '%', we can skip all allocation.
        if (string.indexOf('%') == -1) {
            // Since there are no % signs we can skip calling papi or our own replacement code
            return string;
        }

        final Matcher matcher = UNIFIED_PLACEHOLDER_PATTERN.matcher(string);

        // If '%' exists but doesn't form a valid simple placeholder, skip to the PAPI/Legacy handler.
        // This avoids allocating the StringBuilder below.
        if (!matcher.find()) {
            return GrimAPI.INSTANCE.getMessagePlaceHolderManager().replacePlaceholders(platformPlayer, string);
        }

        // Get references to the maps once, outside the loop.
        final Map<String, String> staticReplacements = GrimAPI.INSTANCE.getExternalAPI().getStaticReplacements();
        final Map<String, Function<GrimUser, String>> variableReplacements = GrimAPI.INSTANCE.getExternalAPI().getVariableReplacements();
        // 32 is a heuristic buffer. It roughly covers the expansion cost of one UUID (36 chars) vs one placeholder (6 chars).
        final StringBuilder sb = new StringBuilder(string.length() + 32);

        // --- PHASE 2: THE REPLACEMENT LOOP ---
        // Used do-while because the first `matcher.find()` was already called above.
        do {
            // The full placeholder, e.g., "%tps%" or "%prefix%"
            final String keyWithPercent = matcher.group(0);
            String value = null;

            // optimization: We check the static map first. This is a single, O(1) hash map lookup.
            String staticValue = staticReplacements.get(keyWithPercent);

            if (staticValue != null) {
                value = staticValue;
            } else if (grimPlayer != null) {
                // If it's not a static placeholder, check if it's a dynamic one.
                // This is a second, O(1) hash map lookup.
                final Function<GrimUser, String> func = variableReplacements.get(keyWithPercent);
                if (func != null) {
                    // LAZY EVALUATION: We only call the expensive function (like getTPS)
                    // if we actually found its placeholder in the string.
                    value = func.apply(grimPlayer);
                }
            }

            // If we found no replacement, `value` will be null.
            // In that case, we treat the placeholder as literal text by appending the original key.
            if (value == null) {
                value = keyWithPercent;
            // yes the check for `removeFormatting` is inside the loop, but it's a simple boolean
            // check and the cost is negligible compared to the string operations.
            // Do NOT escape - this is probably a PAPI key that must remain intact
            } else if (removeFormatting) {
                // Note: This assumes `escapeMarkdown` is reasonably fast.
                // If it's slow, there are further micro-optimizations, but this is the right place for it.
                value = CompiledDiscordTemplate.escapeMarkdown(value);
            }

            // `appendReplacement` efficiently appends the text between matches and our replacement value.
            // `Matcher.quoteReplacement` should handle any '$' or '\' in the replacement value.
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));

        } while (matcher.find());

        // Append the final part of the string after the last found placeholder.
        matcher.appendTail(sb);

        // Create the final string from our builder.
        String grimReplaced = sb.toString();

        return GrimAPI.INSTANCE.getMessagePlaceHolderManager().replacePlaceholders(platformPlayer, grimReplaced).replace(PLACEHOLDER_ESCAPE_CHAR, '%');
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
