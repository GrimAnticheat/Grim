package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSelectBundleItem;

@CheckData(name = "CrashI", stableKey = "grim.crash.invalid_bundle_slot", description = "Sent a bundle item selection with an invalid negative slot index")
public class CrashI extends Check implements PacketCheck {
    private static final Verbose V = Verbose.of("selectedItemIndex={sint}");

    public CrashI(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.SELECT_BUNDLE_ITEM) {
            int selectedItemIndex;
            try {
                selectedItemIndex = new WrapperPlayClientSelectBundleItem(event).getSelectedItemIndex();
            } catch (IllegalArgumentException e) {
                // thanks packetevents!
                if (e.getMessage().startsWith("Invalid selectedItemIndex: ")) {
                    selectedItemIndex = Integer.parseInt(e.getMessage().substring(27));
                } else {
                    throw e;
                }
            }

            if (selectedItemIndex < -1) {
                flag(V.write(verbose()).sint(selectedItemIndex));
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}