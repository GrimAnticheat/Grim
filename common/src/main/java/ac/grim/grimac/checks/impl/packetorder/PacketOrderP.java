package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import it.unimi.dsi.fastutil.ints.IntArrayList;

@CheckData(name = "PacketOrderP", stableKey = "grim.packetorder.transaction_response_order", description = "Responded to chunk batch packets in an invalid transaction order", experimental = true)
public class PacketOrderP extends Check implements PacketCheck {
    private static final Verbose V = Verbose.of("[invalid response|skipped response, type={packet}]");

    public PacketOrderP(final GrimPlayer player) {
        super(player);
    }

    private byte trimTimer; // let the list shrink eventually
    private final IntArrayList transactions = new IntArrayList(0);

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHUNK_BATCH_ACK) {
            if (!transactions.rem(player.getLastTransactionReceived())) {
                flag(V.write(verbose()).bool(true).sint(VerboseCodecs.PACKET_NONE));
            }
        } else if (!isAsync(event.getPacketType()) && !isTransaction(event.getPacketType())) {
            if (transactions.rem(player.getLastTransactionReceived())) {
                int packetId = VerboseCodecs.packet(event.getPacketType(), player.getClientVersion());
                flag(V.write(verbose()).bool(false).sint(packetId));
            }
        }
    }

    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CHUNK_BATCH_END) {
            boolean sendingBundlePacket = player.packetStateData.sendingBundlePacket;
            if (!sendingBundlePacket) player.user.sendPacket(new WrapperPlayServerBundle());

            player.sendTransaction();
            int transaction = player.getLastTransactionSent();
            transactions.add(transaction);
            if (++trimTimer == 0) transactions.trim();
            player.addRealTimeTaskNext(() -> {
                if (transactions.rem(transaction)) {
                    flag(V.write(verbose()).bool(false).sint(VerboseCodecs.PACKET_TRANSACTION));
                }
            });

            if (!sendingBundlePacket) {
                event.getTasksAfterSend().add(() -> player.user.sendPacket(new WrapperPlayServerBundle()));
            }
        }
    }
}
