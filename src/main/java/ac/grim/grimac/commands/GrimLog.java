package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.SuperDebug;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.command.CommandSender;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@CommandAlias("grim|grimac")
public class GrimLog extends BaseCommand {

    @Subcommand("log|logs")
    @CommandPermission("grim.log")
    @CommandAlias("gl")
    public void onLog(CommandSender sender, int flagId) {
        StringBuilder builder = SuperDebug.getFlag(flagId);

        if (builder == null) {
            String failure = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log-not-found", "%prefix% &cUnable to find that log");
            failure = MessageUtil.replacePlaceholders(sender, failure);
            MessageUtil.sendMessage(sender, MessageUtil.miniMessage(failure));
            return;
        }

        String success = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log", "%prefix% &fUploaded debug to: %url%");
        String failure = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log-upload-failure", "%prefix% &cSomething went wrong while uploading this log, see console for more information.");

        String uploading = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log-start", "%prefix% &fUploading log... please wait");
        uploading = MessageUtil.replacePlaceholders(sender, uploading);
        MessageUtil.sendMessage(sender, MessageUtil.miniMessage(uploading));

        FoliaScheduler.getAsyncScheduler().runNow(GrimAPI.INSTANCE.getPlugin(), (dummy) -> {
            try {
                URL mUrl = new URL("https://paste.grim.ac/data/post");
                HttpURLConnection urlConn = (HttpURLConnection) mUrl.openConnection();
                urlConn.setDoOutput(true);
                urlConn.setRequestMethod("POST");
                urlConn.addRequestProperty("User-Agent", "GrimAC/" + GrimAPI.INSTANCE.getExternalAPI().getGrimVersion());
                urlConn.addRequestProperty("Content-Type", "text/yaml"); // Not really yaml, but looks nicer than plaintext
                urlConn.setRequestProperty("Content-Length", Integer.toString(builder.length()));
                urlConn.getOutputStream().write(builder.toString().getBytes(StandardCharsets.UTF_8));

                urlConn.getOutputStream().close();

                int response = urlConn.getResponseCode();

                if (response == HttpURLConnection.HTTP_CREATED) {
                    String responseURL = urlConn.getHeaderField("Location");
                    String message = success.replace("%url%", "https://paste.grim.ac/" + responseURL);
                    message = MessageUtil.replacePlaceholders(sender, message);
                    MessageUtil.sendMessage(sender, MessageUtil.miniMessage(message));
                } else {
                    String message = MessageUtil.replacePlaceholders(sender, failure);
                    MessageUtil.sendMessage(sender, MessageUtil.miniMessage(message));
                    LogUtil.error("Returned response code " + response + ": " + urlConn.getResponseMessage());
                }

                urlConn.disconnect();
            } catch (Exception e) {
                String message = MessageUtil.replacePlaceholders(sender, failure);
                MessageUtil.sendMessage(sender, MessageUtil.miniMessage(message));
                e.printStackTrace();
            }
        });
    }
}
