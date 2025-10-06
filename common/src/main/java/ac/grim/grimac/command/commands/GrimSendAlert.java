package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.regex.Pattern;

public final class GrimSendAlert implements BuildableCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage(); // Strict MiniMessage parser
    // Pattern to find & followed by a valid color/formatting code char
    private static final Pattern LEGACY_COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
            manager.commandBuilder("grim", "grimac")
                   .literal(
                       "sendalert",
                       Description.of("Broadcast an alert (MiniMessage or legacy) to all staff"))
                   .permission("grim.sendalert")
                   .required("message", StringParser.greedyStringParser())
                   .handler(this::handleSendAlert)
        );
    }

    /**
     * Translates legacy '&' color codes to '§' color codes.
     */
    private String translateLegacyColors(String text) {
        if (text == null) {
            return null;
        }
        return LEGACY_COLOR_CODE_PATTERN.matcher(text).replaceAll("\u00A7$1");
    }

    private void handleSendAlert(@NonNull CommandContext<Sender> ctx) {
        String raw = ((String) ctx.get("message")).trim();

        // Strip optional wrapping quotes
        if (raw.length() > 1 &&
           ((raw.startsWith("\"") && raw.endsWith("\"")) ||
            (raw.startsWith("'")  && raw.endsWith("'")))) {
            raw = raw.substring(1, raw.length() - 1);
        }

        String replacedPlaceholders = MessageUtil.replacePlaceholders((Sender) null, raw);

        // Translate & to § for compatibility with MiniMessage's interpretation of legacy codes in attributes
        String translatedForMiniMessage = translateLegacyColors(replacedPlaceholders);

        Component finalComponent;

        try {
            // Attempt to parse with the STRICT MiniMessage parser
            Component miniMessageAttempt = MM.deserialize(translatedForMiniMessage);

            // If MM.deserialize didn't apply MiniMessage formatting (e.g., input was "§4Hello"),
            // it means the input was likely pure legacy or plain text.
            if (miniMessageAttempt.equals(Component.text(translatedForMiniMessage))) {
                // Fall back to MessageUtil.miniMessage for pure legacy & code handling,
                // using the original placeholder-replaced string (before & -> § translation).
                finalComponent = MessageUtil.miniMessage(replacedPlaceholders);
            } else {
                // MiniMessage formatting was successfully applied.
                finalComponent = miniMessageAttempt;
            }
        } catch (Exception e) {
            // Fallback for cases where strict MM.deserialize fails (e.g., malformed MiniMessage,
            // or complex mixed content that MessageUtil.miniMessage can handle).
            // Use the original placeholder-replaced string.
            finalComponent = MessageUtil.miniMessage(replacedPlaceholders);
        }

        GrimAPI.INSTANCE.getAlertManager().sendAlert(finalComponent, null);
    }
}
