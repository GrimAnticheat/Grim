package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsU;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;

@CheckData(name = "CrashI")
public class CrashI extends Check implements PacketCheck {
    private static final Gson GSON = new GsonBuilder().create();
    private static final int COLOR_CHAR = '\u00a7';

    private final boolean isLegacyClient = player.getClientVersion().isOlderThan(ClientVersion.V_1_9);
    private final boolean isLegacyServer = PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9);

    public CrashI(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isLegacyServer && event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
            WrapperPlayClientUpdateSign sign = new WrapperPlayClientUpdateSign(event);
            String[] lines = sign.getTextLines();
            boolean flaggedColorSign = false;
            try {
                for (int i = 0; i < lines.length; ++i) {
                    JsonElement json = GSON.fromJson(lines[i], JsonElement.class);
                    String line = null;
                    boolean invalidLine = false;

                    // 1.8 clients seems always simple string like "abc"
                    if (isLegacyClient && json.isJsonPrimitive()) {
                        JsonPrimitive jsonPrimitive = (JsonPrimitive) json;
                        if (jsonPrimitive.isString())
                            line = jsonPrimitive.getAsString();
                        else
                            invalidLine = true;
                    }
                    // 1.9+ and Via likely {"text":"abc"}
                    // Via handle this bad, allows 1.9+ clients use sign crasher
                    // I don't care about 1.9+ clients falses before Via fix json
                    else if (!isLegacyClient && json.isJsonObject()) {
                        for (Map.Entry<String, JsonElement> member : ((JsonObject) json).entrySet()) {
                            String key = member.getKey();
                            if (key.equals("text")) {
                                JsonElement valueElement = member.getValue();
                                if (valueElement.isJsonPrimitive()) {
                                    JsonPrimitive valuePrimitive = (JsonPrimitive) valueElement;
                                    if (valuePrimitive.isString()) {
                                        line = valuePrimitive.getAsString();
                                        continue;
                                    }
                                }
                            }
                            line = null;
                            break;
                        }
                        if (line == null) {
                            invalidLine = true;
                        }
                    } else {
                        invalidLine = true;
                    }

                    if (invalidLine) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        flagAndAlert("custom json");
                        break;
                    }
                    if (!flaggedColorSign && line.indexOf(COLOR_CHAR) != -1) {
                        player.checkManager.getPacketCheck(BadPacketsU.class).handleColorSign(event);
                        if (event.isCancelled())
                            break;
                        flaggedColorSign = true;
                    }
                }
            }
            // Will some 384 length strings make stack overflow?
            catch (StackOverflowError | Exception e) {
                if (!event.isCancelled()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
                flagAndAlert("failed to get json");
            }
        }

    }

}
