package ac.grim.grimac.checks.impl.chat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "ChatA", stableKey = "grim.exploit.blank_tab_complete", description = "Sent a tab complete packet with no command or input text", experimental = true)
public class ChatA extends Check implements PacketListener {

    public ChatA(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            WrapperPlayClientTabComplete wrapper = new WrapperPlayClientTabComplete(event);
            String text = wrapper.getText();
            if (text.equals("/") || text.trim().isEmpty()) {
                if (flag() && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }, PacketType.Play.Client.TAB_COMPLETE);
    }
}
