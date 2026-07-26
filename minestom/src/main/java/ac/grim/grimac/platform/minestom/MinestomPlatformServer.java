package ac.grim.grimac.platform.minestom;

import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.minestom.sender.MinestomSenderFactory;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;

/**
 * Minestom implementation of Grim's {@link PlatformServer}.
 * <p>
 * TODO Phase 3: {@link #getTPS} returns the fixed target rate; a measured TPS needs tick-time
 * sampling. Minestom needs no outgoing plugin-channel registration, so that call is a no-op.
 */
public final class MinestomPlatformServer implements PlatformServer {

    private static final double MINESTOM_TARGET_TPS = 20.0D;

    private final MinestomSenderFactory senderFactory;

    public MinestomPlatformServer(MinestomSenderFactory senderFactory) {
        this.senderFactory = senderFactory;
    }

    @Override
    public String getPlatformImplementationString() {
        return "Minestom";
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        MinecraftServer.getCommandManager().execute((CommandSender) sender.getNativeSender(), command);
    }

    @Override
    public Sender getConsoleSender() {
        return senderFactory.wrap(MinecraftServer.getCommandManager().getConsoleSender());
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
        // Minestom does not require registering outgoing plugin-message channels.
    }

    @Override
    public double getTPS() {
        return MINESTOM_TARGET_TPS;
    }
}
