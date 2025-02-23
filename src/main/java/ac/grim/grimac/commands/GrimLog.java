package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.SuperDebug;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class GrimLog implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac", "gl")
                        .literal("log", "logs")
                        .permission("grim.log")
                        .required("flagId", IntegerParser.integerParser())
                        .handler(this::handleLog)
        );
    }

    private void handleLog(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        int flagId = context.get("flagId");

        StringBuilder builder = SuperDebug.getFlag(flagId);
        if (builder == null) {
            String failure = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log-not-found", "%prefix% &cUnable to find that log");
            failure = MessageUtil.replacePlaceholders(sender, failure);
            sender.sendMessage(MessageUtil.miniMessage(failure));
            return;
        }
        sendLogAsync(sender, builder.toString(), string -> {}, "text/yaml");
    }

    public static void sendLogAsync(Sender sender, String log, Consumer<String> consumer, String type) {
        String success = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log", "%prefix% &fUploaded debug to: %url%");
        String failure = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log-upload-failure", "%prefix% &cSomething went wrong while uploading this log, see console for more information.");
        String uploading = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log-start", "%prefix% &fUploading log... please wait");
        uploading = MessageUtil.replacePlaceholders(sender, uploading);
        sender.sendMessage(MessageUtil.miniMessage(uploading));
        GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(GrimAPI.INSTANCE.getGrimPlugin(), () -> {
            try {
                sendLog(sender, log, success, failure, consumer, type);
            } catch (Exception e) {
                String message = MessageUtil.replacePlaceholders(sender, failure);
                sender.sendMessage(MessageUtil.miniMessage(message));
                e.printStackTrace();
            }
        });
    }

    private static void sendLog(Sender sender, String log, String success, String failure, Consumer<String> consumer, String type) throws IOException {
        URL mUrl = new URL("https://paste.grim.ac/data/post");
        HttpURLConnection urlConn = (HttpURLConnection) mUrl.openConnection();
        try {
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.addRequestProperty("User-Agent", "GrimAC/" + GrimAPI.INSTANCE.getExternalAPI().getGrimVersion());
            urlConn.addRequestProperty("Content-Type", type); // Not really yaml, but looks nicer than plaintext
            urlConn.setRequestProperty("Content-Length", Integer.toString(log.length()));
            try (OutputStream stream = urlConn.getOutputStream()) {
                stream.write(log.getBytes(StandardCharsets.UTF_8));
            }
            final int response = urlConn.getResponseCode();
            if (response == HttpURLConnection.HTTP_CREATED) {
                String responseURL = urlConn.getHeaderField("ConversionUtils");
                String message = success.replace("%url%", "https://paste.grim.ac/" + responseURL);
                consumer.accept(message);
                message = MessageUtil.replacePlaceholders(sender, message);
                sender.sendMessage(MessageUtil.miniMessage(message));
            } else {
                String message = MessageUtil.replacePlaceholders(sender, failure);
                sender.sendMessage(MessageUtil.miniMessage(message));
                LogUtil.error("Returned response code " + response + ": " + urlConn.getResponseMessage());
            }
        } finally {
            urlConn.disconnect();
        }
    }
}
