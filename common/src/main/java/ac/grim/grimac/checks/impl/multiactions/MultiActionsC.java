package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

@CheckData(name = "MultiActionsC", stableKey = "grim.multiactions.inventory_click_while_moving", verboseVersion = 1, description = "Clicked in inventory while moving")
public class MultiActionsC extends Check implements PacketCheck {
    private static final String[] VERBOSE_SCHEMA = {"sprinting:bool", "sneaking:bool", "input:bool"};
    public static final VerboseSchema V = verboseSchema();

    public MultiActionsC(GrimPlayer player) {
        super(player);
    }

    public static VerboseSchema verboseSchema() {
        return VerboseSchema.of(VERBOSE_SCHEMA);
    }

    // TODO: move this to a better spot? not sure where to put this
    @Contract(pure = true)
    public static String getVerbose(@NotNull GrimPlayer player) {
        return getVerbose(isVerboseSprinting(player), isVerboseSneaking(player), isVerboseInput(player));
    }

    @Contract(pure = true)
    public static String getVerbose(boolean sprinting, boolean sneaking, boolean input) {
        StringJoiner verbose = new StringJoiner(", ");
        if (sprinting) {
            verbose.add("sprinting");
        }

        if (sneaking) {
            verbose.add("sneaking");
        }

        if (input) {
            verbose.add("input");
        }

        return verbose.toString();
    }

    @Contract(pure = true)
    public static boolean isVerboseSprinting(@NotNull GrimPlayer player) {
        return player.isSprinting && (!player.isSwimming || !player.clientClaimsLastOnGround);
    }

    @Contract(pure = true)
    public static boolean isVerboseSneaking(@NotNull GrimPlayer player) {
        return player.isSneaking && player.getClientVersion().isOlderThan(ClientVersion.V_1_15);
    }

    @Contract(pure = true)
    public static boolean isVerboseInput(@NotNull GrimPlayer player) {
        return player.supportsEndTick() && player.packetStateData.knownInput.moving();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;
        if (player.serverOpenedInventoryThisTick) return;

        boolean sprinting = isVerboseSprinting(player);
        boolean sneaking = isVerboseSneaking(player);
        boolean input = isVerboseInput(player);
        if (!sprinting && !sneaking && !input) return;

        if (flagAndAlert(V.write(verbose()).bool(sprinting).bool(sneaking).bool(input)) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
