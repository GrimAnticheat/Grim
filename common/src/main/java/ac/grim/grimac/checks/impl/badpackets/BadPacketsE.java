package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsE", stableKey = "grim.badpackets.invalid_position", description = "Sent too many movement packets without updating position")
public class BadPacketsE extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("ticks={uint}");

    private int noReminderTicks;
    private final int maxNoReminderTicks = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) ? 20 : 19;

    public BadPacketsE(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerReceiveHandler(event -> noReminderTicks = 0,
                PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
                PacketType.Play.Client.PLAYER_POSITION,
                PacketType.Play.Client.STEER_VEHICLE // Exempt vehicles
        );

        registry.registerReceiveHandler(event -> {
            if (player.packetStateData.lastPacketWasTeleport) return;

            if (++noReminderTicks > maxNoReminderTicks) {
                flag(V.write(verbose()).uint(noReminderTicks));
            }
        }, PacketType.Play.Client.PLAYER_FLYING, PacketType.Play.Client.PLAYER_ROTATION);

        boolean isViaPleaseStopUsingProtocolHacksOnYourServer = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2)
                || PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2);
        if (isViaPleaseStopUsingProtocolHacksOnYourServer) {
            registry.registerReceiveHandler(event -> {
                if (player.inVehicle()) {
                    noReminderTicks = 0; // Exempt vehicles
                }
            });
        }
    }

    public void handleRespawn() {
        noReminderTicks = 0;
    }
}
