package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTickingState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;

import java.util.Deque;
import java.util.LinkedList;

@CheckData(name = "BadPacketsZ", description = "Did not send movement packets", setback = 1, decay = 0.001)
public class BadPacketsZ extends Check implements PostPredictionCheck {
    private long lastTransSent = 0;
    private long lastTransReceived = 0;
    private Deque<Short> transactionsToConsider = new LinkedList<>();
    private int ticksSinceMovement = 0;

    private long nanosPerTick = (long)50e6;

    public BadPacketsZ(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        long currentTime = System.nanoTime();
        short id = getTransactionID(event);

        if (id <= 0 && currentTime - lastTransSent >= nanosPerTick) {
            lastTransSent = currentTime;
            transactionsToConsider.add(id);
            if (transactionsToConsider.size() > 20) transactionsToConsider.removeFirst();
        }

        if (event.getPacketType() == PacketType.Play.Server.TICKING_STATE) {
            WrapperPlayServerTickingState tickingState = new WrapperPlayServerTickingState(event);
            nanosPerTick = (long)(1e9 / Math.min(20, tickingState.getTickRate()));
        }
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        long currentTime = System.nanoTime();
        short id = getTransactionID(event);

        if (transactionsToConsider.contains(id)) {
            transactionsToConsider.remove(id);
            // We've added a rate limit on the server side, so it is impossible for the client to respond faster
            // unless they had a lag spike and is ticking faster to catch up with the server.
            if (currentTime - lastTransReceived >= nanosPerTick) {
                ticksSinceMovement++;
                lastTransReceived = currentTime;
            }
        }
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // A player will only send movement packets once every second (20 ticks) during 0.03
        if (player.skippedTickInActualMovement || player.likelyKB != null) {
            ticksSinceMovement -= 19;
        }

        if (ticksSinceMovement > 1) {
            flagAndAlertWithSetback("skipped: " + (ticksSinceMovement - 1));
        } else {
            reward();
        }

        ticksSinceMovement = 0;
    }

    public short getTransactionID(PacketReceiveEvent event) {
        short id = 1; // Bogus value for when the packet isn't a transaction

        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            WrapperPlayClientWindowConfirmation transaction = new WrapperPlayClientWindowConfirmation(event);
            id = transaction.getActionId();
        }

        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            WrapperPlayClientPong pong = new WrapperPlayClientPong(event);

            int longID = pong.getId();
            if (longID == (short)longID) {
                id = (short)longID;
            }
        }

        return id;
    }

    public short getTransactionID(PacketSendEvent event) {
        short id = 1;

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            WrapperPlayServerWindowConfirmation transaction = new WrapperPlayServerWindowConfirmation(event);
            id = transaction.getActionId();
        }

        if (event.getPacketType() == PacketType.Play.Server.PING) {
            WrapperPlayServerPing pong = new WrapperPlayServerPing(event);

            int longID = pong.getId();
            if (longID == (short)longID) {
                id = (short)longID;
            }
        }

        return id;
    }
}
