package ac.grim.grimac.checks.impl.chat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.common.client.WrapperCommonClientSettings;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;

@CheckData(name = "ChatD", description = "Chatting while chat is hidden", experimental = true)
public class ChatD extends Check implements PacketCheck {
    private boolean hidden;

    public ChatD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE
                || event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND_UNSIGNED
                || event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {
            if (hidden && flagAndAlert() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS || event.getPacketType() == PacketType.Configuration.Client.CLIENT_SETTINGS) {
            hidden = new WrapperPlayClientSettings(event).getChatVisibility() == WrapperCommonClientSettings.ChatVisibility.HIDDEN;
        }
    }
}
