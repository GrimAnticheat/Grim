package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

@CheckData(name = "BadPacketsO", stableKey = "grim.badpackets.invalid_keepalive", description = "Responded with a keepalive ID that was not sent by the server")
public class BadPacketsO extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("id={slong}");

    private final LinkedList<Long> keepalives = new LinkedList<>();

    public BadPacketsO(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerReceiveHandler(event -> {
            final long id = new WrapperPlayClientKeepAlive(event).getId();

            for (long keepalive : keepalives) {
                if (keepalive == id) {
                    // Found the ID, remove stuff until we get to it (to stop very slow memory leaks)
                    Long data;
                    do {
                        data = keepalives.poll();
                    } while (data != null && data != id);

                    return;
                }
            }

            if (flag(V.write(verbose()).slong(id)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.KEEP_ALIVE);

        registry.registerSendHandler(event -> keepalives.add(new WrapperPlayServerKeepAlive(event).getId()), PacketType.Play.Server.KEEP_ALIVE);
    }
}
