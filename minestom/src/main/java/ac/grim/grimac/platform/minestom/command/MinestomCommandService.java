package ac.grim.grimac.platform.minestom.command;

import ac.grim.grimac.platform.api.command.CommandService;

/**
 * Registers Grim's admin commands. TODO Phase 4: wire Grim's cloud commands to Minestom via
 * the monorepo's Lamp/{@code MinestomCommands} integration; a no-op keeps the platform bootable
 * (checks/alerts work without the admin command surface).
 */
public final class MinestomCommandService implements CommandService {

    @Override
    public void registerCommands() {
        // TODO Phase 4
    }
}
