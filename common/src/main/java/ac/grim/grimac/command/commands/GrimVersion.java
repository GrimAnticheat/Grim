package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class GrimVersion implements BuildableCommand {

    private static final AtomicReference<Component> updateMessage = new AtomicReference<>();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static long lastCheck;

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
        try {
            //
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CommonGrimArguments.API_URL.value() + "updates"))
                    .GET()
                    .header("User-Agent", "GrimAC/" + GrimAPI.INSTANCE.getExternalAPI().getGrimVersion())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.of(5, ChronoUnit.SECONDS))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            final int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                Component msg = updateMessage.get();
                sender.sendMessage(Objects.requireNonNullElseGet(msg, () -> Component.text()
                        .append(MessageUtil.miniMessage("%prefix%"))
                        .append(Component.text(" Failed to check latest GrimAC version. Update server responded with status code: ")
                                .color(NamedTextColor.YELLOW))
                        .append(Component.text(statusCode)
                                .color(getColorForStatusCode(statusCode))
                                .decorate(TextDecoration.BOLD))
                        .build()));
                return;
            }
            // Using old JsonParser method, as old versions of Gson don't include the static one
            JsonObject object = new JsonParser().parse(response.body()).getAsJsonObject();
            String downloadPage = getJsonString(object, "download_page", "Unknown");
            String latest = getJsonString(object, "latest_version", "Unknown");
            @Nullable String warning = getJsonString(object, "warning", null);
            // allow status to be overridden if provided
            Status status;
            if (object.has("status")) {
                status = Status.getStatus(object.get("status").getAsString());
            } else {
                status = Status.SemVer.getVersionStatus(GrimAPI.INSTANCE.getExternalAPI().getGrimVersion(), latest);
            }
            //
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
                        .append(Component.text(downloadPage).color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl(downloadPage)))
                        .build();
                case UNKNOWN ->
                        Component.text("You are using an unknown GrimAC version.").color(NamedTextColor.RED);
            };
            // in case of a critical exploit that requires attention, allow us to provide a warning
            if (warning != null && !warning.isBlank()) {
                msg = msg.append(Component.text().append(Component.text(warning).color(NamedTextColor.RED)).build());
            }
            updateMessage.set(msg);
            sender.sendMessage(msg);
        } catch (Exception e) {
            sender.sendMessage(Component.text("Failed to check latest version.").color(NamedTextColor.RED));
            LogUtil.error("Failed to check latest GrimAC version.", e);
        }
    }

    private static String getJsonString(JsonObject object, String key, String defaultValue) {
        return object.has(key) ? object.get(key).getAsString() : defaultValue;
    }

    private static NamedTextColor getColorForStatusCode(int code) {
        if (code >= 500) { // Server Errors (e.g., 500, 503)
            return NamedTextColor.RED;
        } else if (code >= 400) { // Client Errors (e.g., 403, 404)
            return NamedTextColor.RED;
        } else if (code >= 300) { // Redirection (e.g., 301, 302)
            return NamedTextColor.YELLOW;
        } else if (code >= 200) { // Success (e.g., 200, 201)
            return NamedTextColor.GREEN;
        }
        return NamedTextColor.GRAY; // Default for 1xx codes or others
    }

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("version")
                        .permission("grim.version")
                        .handler(this::handleVersion)
        );
    }

    private void handleVersion(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        checkForUpdatesAsync(sender);
    }


    @AllArgsConstructor
    private enum Status {
        AHEAD("ahead"),
        UPDATED("updated"),
        OUTDATED("outdated"),
        UNKNOWN("unknown");

        private final String id;

        public static Status getStatus(String id) {
            for (Status status : Status.values()) {
                if (status.id.equals(id)) return status;
            }
            return UNKNOWN;
        }

        private static class SemVer {

            public static Status getVersionStatus(String current, String latest) {
                try {
                    var cmp = compareSemver(current, latest);
                    if (cmp == 0) {
                        return Status.UPDATED;
                    }
                    if (cmp < 0) {
                        return Status.OUTDATED;
                    }
                    return Status.AHEAD;
                } catch (Exception ignored) {}
                return Status.UNKNOWN;
            }

            public static String normalizeCoreVersion(String version) {
                String trimmed = version.trim();
                String[] dashParts = trimmed.split("-");
                String[] plusParts = dashParts[0].split("\\+");
                return plusParts[0];
            }

            public static int[] parseVersion(String version) {
                String core = normalizeCoreVersion(version);
                if (core.isEmpty()) return null;
                String[] parts = core.split("\\.");
                if (parts.length < 1) return null;

                int major = parseInt(parts[0]);
                int minor = parts.length > 1 ? parseInt(parts[1]) : 0;
                int patch = parts.length > 2 ? parseInt(parts[2]) : 0;

                if (major < 0 || minor < 0 || patch < 0) {
                    return null;
                }

                return new int[] { major, minor, patch };
            }

            private static int parseInt(String str) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }

            public static int compareSemver(String a, String b) {
                int[] pa = parseVersion(a);
                int[] pb = parseVersion(b);
                if (pa == null || pb == null) return 0;

                for (int i = 0; i < 3; i++) {
                    if (pa[i] < pb[i]) return -1;
                    if (pa[i] > pb[i]) return 1;
                }
                return 0;
            }
        }
    }
}
