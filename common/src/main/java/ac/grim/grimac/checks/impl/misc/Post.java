package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.lists.EvictingQueue;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.*;

@CheckData(name = "Post", stableKey = "grim.post.invalid_order", description = "Sent packets out of the expected movement tick order")
public class Post extends Check implements PacketListener, PostPredictionListener {
    private static final Verbose V = Verbose.of("{str}");

    private final ArrayDeque<PacketTypeCommon> post = new ArrayDeque<>();
    // Due to 1.9+ missing the idle packet, we must queue flags
    // 1.8 clients will have the same logic for simplicity, although it's not needed
    private final List<String> flags = new EvictingQueue<>(10);
    private boolean sentFlying = false;
    private int isExemptFromSwingingCheck = Integer.MIN_VALUE;

    public Post(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (flags.isEmpty()) {
            return;
        }

        // Okay, the user might be cheating, let's double check
        // 1.8 clients have the idle packet, and this shouldn't false on 1.8 clients
        // 1.9+ clients have predictions, which will determine if hidden tick skipping occurred
        if (player.isTickingReliablyFor(3)) {
            for (String flag : flags) {
                flag(V.write(verbose()).str(flag));
            }
        }

        flags.clear();
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerReceiveHandler(event -> {
            // Don't count teleports or duplicates as movements
            if (!isTickPacket(event.getPacketType())) return;
            post.clear();
            sentFlying = true;
        }, TICK_PACKET_CANDIDATES);

        registry.registerReceiveHandler(event -> {
            if (!player.packetStateData.lastTransactionPacketWasValid) return;
            if (sentFlying && !post.isEmpty()) {
                flags.add(post.getFirst().toString().toLowerCase(Locale.ROOT).replace("_", " ") + " v" + player.getClientVersion().getReleaseName());
            }
            post.clear();
            sentFlying = false;
        }, TRANSACTION_PACKETS);

        registry.registerReceiveHandler(this::addPost,
                PLAYER_ABILITIES,
                INTERACT_ENTITY,
                PLAYER_BLOCK_PLACEMENT,
                ATTACK,
                SPECTATE_ENTITY,
                PUNCH,
                USE_ITEM,
                PLAYER_DIGGING);

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
            registry.registerReceiveHandler(this::addPost, HELD_ITEM_CHANGE);
        }

        // Why do 1.13+ players send the click window packet whenever? This doesn't make sense.
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_13)) {
            registry.registerReceiveHandler(this::addPost, CLICK_WINDOW);
        }

        boolean noViaRewindDelayedSwing = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                || PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8);
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_13) // 1.13 clicking inventory causes weird animations
                && noViaRewindDelayedSwing) {
            registry.registerReceiveHandler(event -> {
                if (isExemptFromSwingingCheck < player.lastTransactionReceived.get()) {
                    addPost(event);
                }
            }, ANIMATION);

            // only matters if the above is registered
            registry.registerSendHandler(event -> {
                WrapperPlayServerEntityAnimation animation = new WrapperPlayServerEntityAnimation(event);
                if (animation.getEntityId() == player.entityID) {
                    if (animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM ||
                            animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND) {
                        isExemptFromSwingingCheck = player.lastTransactionSent.get();
                    }
                }
            }, PacketType.Play.Server.ENTITY_ANIMATION);
        }

        registry.registerReceiveHandler(event -> {
            WrapperPlayClientEntityAction.Action action = new WrapperPlayClientEntityAction(event).getAction();
            // ViaRewind sends START_FALL_FLYING packets async for 1.8 clients on 1.9+ servers
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)
                    && action == WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA)
                return;

            // https://github.com/GrimAnticheat/Grim/issues/824
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_3) && player.inVehicle())
                return;

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) && action == WrapperPlayClientEntityAction.Action.LEAVE_BED)
                return;

            addPost(event);
        }, ENTITY_ACTION);
    }

    private void addPost(@NotNull PacketReceiveEvent event) {
        if (!sentFlying) return;
        post.add(event.getPacketType());
    }
}
