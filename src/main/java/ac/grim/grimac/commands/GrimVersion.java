package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.LogUtil;
import co.aikar.commands.annotation.CommandAlias;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@CommandAlias("grim|grimac")
public class GrimVersion implements BuildableCommand {

    private static long lastCheck;
    private static final AtomicReference<Component> updateMessage = new AtomicReference<>();

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("version")
                        .permission("grim.version")
                        .handler(this::handleVersion)
        );
    }

    private void handleVersion(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        checkForUpdatesAsync(sender);
    }

    public static void checkForUpdatesAsync(Sender sender) {
        String current = GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        sender.sendMessage(Component.text()
                .append(Component.text("Grim Version: ").color(NamedTextColor.GRAY))
                .append(Component.text(current).color(NamedTextColor.AQUA))
                .build());
        // use cached message if last check was less than 1 minute ago
        final long now = System.currentTimeMillis();
        if (now - lastCheck < 60000) {
            Component message = updateMessage.get();
            if (message != null) sender.sendMessage(message);
            return;
        }
        lastCheck = now;
        GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(GrimAPI.INSTANCE.getGrimPlugin(), () -> checkForUpdates(sender));
    }

    // Using UserAgent format recommended by https://docs.modrinth.com/api/
    @SuppressWarnings("deprecation")
    private static void checkForUpdates(Sender sender) {
        String current = GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/project/LJNGWSvH/version"))
                    .GET()
                    .header("User-Agent", "GrimAnticheat/Grim/" + GrimAPI.INSTANCE.getExternalAPI().getGrimVersion())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.of(5, ChronoUnit.SECONDS))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Component msg = updateMessage.get();
                sender.sendMessage(Objects.requireNonNullElseGet(msg, () -> Component.text()
                        .append(Component.text("Failed to check latest version.").color(NamedTextColor.RED))
                        .build()));
                LogUtil.error("Failed to check latest GrimAC version. Response code: " + response.statusCode());
                return;
            }
            // Using old JsonParser method, as old versions of Gson don't include the static one
            JsonObject object = new JsonParser().parse(response.body()).getAsJsonArray().get(0).getAsJsonObject();
            String latest = object.get("version_number").getAsString();
            Status status = compareVersions(current, latest);
            Component msg = switch (status) {
                case AHEAD ->
                        Component.text("You are using a development version of GrimAC").color(NamedTextColor.LIGHT_PURPLE);
                case UPDATED ->
                        Component.text("You are using the latest version of GrimAC").color(NamedTextColor.GREEN);
                case OUTDATED -> Component.text()
                        .append(Component.text("New GrimAC version found!").color(NamedTextColor.AQUA))
                        .append(Component.text(" Version ").color(NamedTextColor.GRAY))
                        .append(Component.text(latest).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                        .append(Component.text(" is available to be downloaded here: ").color(NamedTextColor.GRAY))
                        .append(Component.text("https://modrinth.com/plugin/grimac").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl("https://modrinth.com/plugin/grimac")))
                        .build();
            };
            updateMessage.set(msg);
            sender.sendMessage(msg);
        } catch (Exception ignored) {
            sender.sendMessage(Component.text("Failed to check latest version.").color(NamedTextColor.RED));
            LogUtil.error("Failed to check latest GrimAC version.", ignored);
        }
    }

    private enum Status {
        AHEAD,
        UPDATED,
        OUTDATED
    }

    private static Status compareVersions(String local, String latest) {
        if (local.equals(latest)) return Status.UPDATED;
        String[] localParts = local.split("\\.");
        String[] latestParts = latest.split("\\.");
        int length = Math.max(localParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int localPart = i < localParts.length ? Integer.parseInt(localParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            if (localPart < latestPart) {
                return Status.OUTDATED;
            } else if (localPart > latestPart) {
                return Status.AHEAD;
            }
        }
        return Status.UPDATED;
    }

}
