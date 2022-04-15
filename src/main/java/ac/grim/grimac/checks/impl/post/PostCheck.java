package ac.grim.grimac.checks.impl.post;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.lists.EvictingList;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Locale;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.*;

@CheckData(name = "Post")
public class PostCheck extends PacketCheck {
    private final ArrayDeque<PacketTypeCommon> post = new ArrayDeque<>();
    private boolean sentFlying = false;

    // EvictingList so we can play with max size
    // Increasing this may reduce falses due to varying latency, although the last 3 ticks seems fine in testing with clumsy
    private final EvictingList<Long> delayBetweenFlying = new EvictingList<>(3);
    // 1.9+ no idle packet handling
    private long lastFlying = 0;

    public PostCheck(GrimPlayer playerData) {
        super(playerData);
        delayBetweenFlying.add(0L);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // Don't count teleports or duplicates as movements
            if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate)
                return;
            post.clear();
            sentFlying = true;

            long time = System.currentTimeMillis();
            delayBetweenFlying.add(time - lastFlying);
            lastFlying = time;
        } else {
            PacketTypeCommon packetType = event.getPacketType();
            if (WINDOW_CONFIRMATION.equals(packetType) || PONG.equals(packetType)) {
                if (sentFlying && !post.isEmpty()) {
                    long max = Collections.max(delayBetweenFlying);
                    long timeSinceLastFlying = System.currentTimeMillis() - lastFlying;

                    // Okay, the user might be cheating, let's double check
                    // 1.8 clients have the idle packet, and this shouldn't false on 1.8 clients
                    if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)
                            // 20-80 ms range seems about right for filtering out idle movement.
                            // and lag spikes that can cause idle packet to be missed
                            //
                            // It can still false due to unlucky timings, but it's good enough.
                            //
                            // Low maximum means that there was a connection lag spike, all 3
                            // movements got bunched together.
                            //
                            // High maximum means the player isn't moving
                            //
                            || ((max > 20 && max < 80)
                            // We should also check if the player just started to stand still
                            // Around 25 ms is about normal for cheats, but a lagging player can go higher
                            && timeSinceLastFlying < 60)) {
                        if (flag()) {
                            alert("" + post.getFirst().toString().toLowerCase(Locale.ROOT).replace("_", " ") + " v" + player.getClientVersion().getReleaseName());
                        }
                    }
                }
                post.clear();
                sentFlying = false;
            } else if (PLAYER_ABILITIES.equals(packetType) || CHAT_MESSAGE.equals(packetType)
                    || CLOSE_WINDOW.equals(packetType) || ENTITY_ACTION.equals(packetType) || INTERACT_ENTITY.equals(packetType) || PLAYER_BLOCK_PLACEMENT.equals(packetType)
                    || USE_ITEM.equals(packetType) || PLAYER_DIGGING.equals(packetType)) {
                if (sentFlying) post.add(event.getPacketType());
            } else if (ANIMATION.equals(packetType) && player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
                // Handle ViaVersion being stupid and sending animations after flying packets for 1.8 players on 1.9+ servers
                // Is this to not false anticheat or what?  What the fuck viaversion.
                if (sentFlying) post.add(event.getPacketType());
            } else if (CLICK_WINDOW.equals(packetType) && player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) {
                // Why do 1.15+ players send the click window packet whenever? This doesn't make sense.
                if (sentFlying) post.add(event.getPacketType());
            }
        }
    }
}
