package ac.grim.grimac.checks.impl.post;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.*;

@CheckData(name = "Post")
public class PostCheck extends Check implements PacketCheck {
    private final ArrayDeque<PacketTypeCommon> post = new ArrayDeque<>();
    // Due to 1.9+ missing the idle packet, we must queue flags
    // 1.8 clients will have the same logic for simplicity, although it's not needed
    private final List<String> flags = new ArrayList<>();
    private boolean sentFlying = false;
    private int isExemptFromSwingingCheck = Integer.MIN_VALUE;

    public PostCheck(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_ANIMATION) {
            WrapperPlayServerEntityAnimation animation = new WrapperPlayServerEntityAnimation(event);
            if (animation.getEntityId() == player.entityID) {
                if (animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM ||
                        animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND) {
                    isExemptFromSwingingCheck = player.lastTransactionSent.get();
                }
            }
        }
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // Don't count teleports or duplicates as movements
            if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
                return;
            }

            if (!flags.isEmpty()) {
                // Okay, the user might be cheating, let's double check
                // 1.8 clients have the idle packet, and this shouldn't false on 1.8 clients
                // 1.9+ clients have predictions, which will determine if hidden tick skipping occurred
                if (player.isTickingReliablyFor(3)) {
                    for (String flag : flags) {
                        flagAndAlert(flag);
                    }
                }

                flags.clear();
            }

            post.clear();
            sentFlying = true;
        } else {
            // 1.13+ clients can click inventory outside tick loop, so we can't post check those two packets on 1.13+
            PacketTypeCommon packetType = event.getPacketType();
            if (WINDOW_CONFIRMATION.equals(packetType) || PONG.equals(packetType)) {
                if (sentFlying && !post.isEmpty()) {
                    flags.add(post.getFirst().toString().toLowerCase(Locale.ROOT).replace("_", " ") + " v" + player.getClientVersion().getReleaseName());
                }
                post.clear();
                sentFlying = false;
            } else if (PLAYER_ABILITIES.equals(packetType)
                    || INTERACT_ENTITY.equals(packetType) || PLAYER_BLOCK_PLACEMENT.equals(packetType)
                    || USE_ITEM.equals(packetType) || PLAYER_DIGGING.equals(packetType)) {
                if (sentFlying) post.add(event.getPacketType());
            } else if (CLICK_WINDOW.equals(packetType) && player.getClientVersion().isOlderThan(ClientVersion.V_1_13)) {
                // Why do 1.13+ players send the click window packet whenever? This doesn't make sense.
                if (sentFlying) post.add(event.getPacketType());
            } else if (ANIMATION.equals(packetType)
                    && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) // ViaVersion delays animations for 1.8 clients
                    || PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8)) // when on 1.9+ servers
                    && player.getClientVersion().isOlderThan(ClientVersion.V_1_13) // 1.13 clicking inventory causes weird animations
                    && isExemptFromSwingingCheck < player.lastTransactionReceived.get()) { // Exempt when the server sends animations because viaversion
                if (sentFlying) post.add(event.getPacketType());
            } else if (ENTITY_ACTION.equals(packetType) // ViaRewind sends START_FALL_FLYING packets async for 1.8 clients on 1.9+ servers
                    && ((player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) // ViaRewind doesn't 1.9 players
                    || PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8)))) { // No elytras
                // https://github.com/GrimAnticheat/Grim/issues/824
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_3) && player.compensatedEntities.getSelf().getRiding() != null) {
                    return;
                }
                if (sentFlying) post.add(event.getPacketType());
            }
        }
    }
}
