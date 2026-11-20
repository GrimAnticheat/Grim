package ac.grim.grimac.checks.impl.chat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.common.client.WrapperCommonClientSettings;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "ChatD", stableKey = "grim.exploit.chat_while_hidden", description = "Chatting while chat is hidden")
public class ChatD extends Check implements PacketListener {
    private boolean hidden;

    public ChatD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            if (hidden && flag() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.CHAT_MESSAGE, PacketType.Play.Client.CHAT_COMMAND_UNSIGNED,
                PacketType.Play.Client.CHAT_COMMAND);

        registry.registerPreViaReceiveHandler(event -> {
            WrapperPlayClientSettings packet = new WrapperPlayClientSettings(event);
            hidden = packet.getChatVisibility() == WrapperCommonClientSettings.ChatVisibility.HIDDEN;
        }, PacketType.Play.Client.CLIENT_SETTINGS, PacketType.Configuration.Client.CLIENT_SETTINGS);
    }
}
