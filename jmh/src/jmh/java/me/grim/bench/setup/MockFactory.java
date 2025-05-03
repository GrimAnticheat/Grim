package me.grim.bench.setup;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.latency.ILatencyUtils;
import ac.grim.grimac.utils.latency.LatencyUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates a "just-enough" GrimPlayer that can be used in unit & JMH
 * benchmarks without spinning up Bukkit or the full PacketEvents stack.
 *
 *  • GrimPlayer constructor is *not* executed (Objenesis).
 *  • Only the handful of fields actually touched by block-update logic
 *    are initialised.
 */
public final class MockFactory {

    private static final ObjenesisStd OBJ = new ObjenesisStd(true);

    private static ConcurrentHashMap<User, GrimPlayer> playerMap() {
        try {
            Object pdm = GrimAPI.INSTANCE.getPlayerDataManager();
            Field f = pdm.getClass().getDeclaredField("playerDataMap");
            f.setAccessible(true);
            return (ConcurrentHashMap<User, GrimPlayer>) f.get(pdm);
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    /* ----------------------------------------------------------- */
    public static GrimPlayer newMockPlayer() {

        /* 1) Allocate GrimPlayer without running its ctor */
        GrimPlayer p = OBJ.newInstance(GrimPlayer.class);

        /* 2) Create a super-light User implementation */
        DummyUser user = new DummyUser();

        /* 3) Fill the fields we need */
        set(p, "uuid", user.getUUID());
        set(p, "user", user);

        set(p, "x", 0d);
        set(p, "y", 64d);
        set(p, "z", 0d);
        set(p, "lastTransSent", 0L);
        set(p, "lastTransactionSent", new AtomicInteger());
        set(p, "lastTransactionReceived", new AtomicInteger());

        /* 4) Minimal world & latency helpers */
        CompensatedWorld world = new CompensatedWorld(p);
        set(p, "compensatedWorld", world);

        ILatencyUtils lat = new LatencyUtils(p);
        set(p, "latencyUtils", lat);

        return p;
    }

    public static void addMockPlayer(User user, GrimPlayer p) {
        playerMap().put(user, p);
    }

    /** Call once you are done with the player to keep tests isolated. */
    public static void releaseMockPlayer(GrimPlayer p) {
        playerMap().remove(p.user);
    }

    /* ===========================================================
       Dummy collaborators
       =========================================================== */

    /* -- a *real* subclass of User so most methods already work -- */
    private static final class DummyUser extends User {
        private static final Channel CHANNEL = new EmbeddedChannel();

        DummyUser() {
            super(CHANNEL, ConnectionState.PLAY,                         // pick any state you like
                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),                         // likewise
                    new UserProfile(UUID.randomUUID(), "MockUser"));
        }

        /* The production code you benchmark only calls getChannel(),
           getUUID() and getName(); everything else can stay as is.
           If you hit other methods, override them here.               */

        @Override public InetSocketAddress getAddress() {
            return new InetSocketAddress("127.0.0.1", 25565);
        }
    }

    /* ===========================================================
       Reflect helper that also works for private + final fields
       =========================================================== */
    public static void set(Object target, String field, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set " + field, e);
        }
    }

    private MockFactory() {}   // util class
}