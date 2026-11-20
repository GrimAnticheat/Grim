package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSelectBundleItem;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "CrashI", stableKey = "grim.crash.invalid_bundle_slot", description = "Sent a bundle item selection with an invalid negative slot index")
public class CrashI extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("selectedItemIndex={sint}");

    public CrashI(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            int selectedItemIndex = getSelectedItemIndex(new WrapperPlayClientSelectBundleItem(event));
            if (selectedItemIndex < -1) {
                flag(V.write(verbose()).sint(selectedItemIndex));
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.SELECT_BUNDLE_ITEM);
    }

    private static int getSelectedItemIndex(@NotNull WrapperPlayClientSelectBundleItem packet) {
        try {
            return packet.getSelectedItemIndex();
        } catch (IllegalArgumentException e) {
            // thanks packetevents!
            if (e.getMessage().startsWith("Invalid selectedItemIndex: ")) {
                return Integer.parseInt(e.getMessage().substring(27));
            } else {
                throw e;
            }
        }
    }
}
