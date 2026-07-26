package ac.grim.grimac.platform.minestom;

import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.sender.Sender;

/**
 * First Minestom implementation of Grim's {@link PlatformServer} SPI — the scaffold that
 * proves the additive {@code grim-minestom} module compiles against Grim {@code common}
 * (Phase 2, Task 2.0). The remaining {@code platform.api} interfaces (PlatformLoader,
 * PlatformPlayerFactory, the schedulers, sender, command, world, …) are implemented in the
 * follow-up Phase 2 tasks; methods that need those are stubbed with a clear TODO for now.
 */
public final class MinestomPlatformServer implements PlatformServer {

    @Override
    public String getPlatformImplementationString() {
        return "Minestom";
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        throw new UnsupportedOperationException("TODO Phase 2: dispatch via MinestomCommands/CommandManager");
    }

    @Override
    public Sender getConsoleSender() {
        throw new UnsupportedOperationException("TODO Phase 2: Minestom console Sender");
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
        throw new UnsupportedOperationException("TODO Phase 2: Minestom plugin messaging channel");
    }

    @Override
    public double getTPS() {
        // Minestom targets a fixed tick rate; a measured value comes with the scheduler work.
        return 20.0D;
    }
}
