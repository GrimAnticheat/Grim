package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;

@CheckData(name = "CrashH", stableKey = "grim.crash.invalid_tab_complete", verboseVersion = 1, description = "Sent a tab complete request with invalid or excessive length")
public class CrashH extends Check implements PacketCheck {
    public static final VerboseSchema V = VerboseSchema.of("lengthLimit:bool", "length:zz");

    public CrashH(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            WrapperPlayClientTabComplete wrapper = new WrapperPlayClientTabComplete(event);
            String text = wrapper.getText();
            final int length = text.length();
            // general length limit
            if (length > (!player.canUseGameMasterBlocks() ? 256 : 32500)) {
                if (shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
                flagAndAlert(V.write(verbose()).bool(true).zz(length));
                return;
            }
            // paper's patch
            final int index;
            if (length > 64 && ((index = text.indexOf(' ')) == -1 || index >= 64)) {
                if (shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
                flagAndAlert(V.write(verbose()).bool(false).zz(length));
            }
        }
    }
}