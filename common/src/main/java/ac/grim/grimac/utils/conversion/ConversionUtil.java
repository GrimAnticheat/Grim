package ac.grim.grimac.utils.conversion;

import ac.grim.grimac.platform.api.player.GameMode;

public class ConversionUtil {
    public static GameMode fromPacketEventsGameMode(com.github.retrooper.packetevents.protocol.player.GameMode gameMode) {
        switch (gameMode) {
            case SURVIVAL:
                return GameMode.SURVIVAL;
            case CREATIVE:
                return GameMode.CREATIVE;
            case ADVENTURE:
                return GameMode.ADVENTURE;
            case SPECTATOR:
                return GameMode.SPECTATOR;
            default:
                throw new IllegalStateException();
        }
    }

    public static com.github.retrooper.packetevents.protocol.player.GameMode toPacketEventsGameMode(GameMode gameMode) {
        switch (gameMode) {
            case SURVIVAL:
                return com.github.retrooper.packetevents.protocol.player.GameMode.SURVIVAL;
            case CREATIVE:
                return com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE;
            case ADVENTURE:
                return com.github.retrooper.packetevents.protocol.player.GameMode.ADVENTURE;
            case SPECTATOR:
                return com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR;
            default:
                throw new IllegalStateException();
        }
    }
}
