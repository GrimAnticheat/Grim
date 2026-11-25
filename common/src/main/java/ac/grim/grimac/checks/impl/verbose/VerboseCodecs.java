package ac.grim.grimac.checks.impl.verbose;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.api.storage.verbose.VerboseTags;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Grim's verbose template tags plus the write-side value encoders that pair
 * with them. Registered once; checks declare templates referencing these tag
 * names and write values through the matching {@code VerboseCodecs.x(...)}
 * encoder.
 */
public final class VerboseCodecs {
    /** {@code {packet}} sentinel for "no packet". */
    public static final int PACKET_NONE = Integer.MIN_VALUE;
    /** {@code {packet}} sentinel for transaction/pong. */
    public static final int PACKET_TRANSACTION = Integer.MIN_VALUE + 1;

    static {
        VerboseTags.registerEnum("face", BlockFace.values());
        VerboseTags.registerEnum("digging", DiggingAction.values());
        VerboseTags.registerEnumLower("digging_lower", DiggingAction.values());
        VerboseTags.registerEnum("clicktype", WindowClickType.values());
        VerboseTags.registerEnumLower("clicktype_lower", WindowClickType.values());
        VerboseTags.registerEnum("entityaction", Action.values());
        VerboseTags.registerEnum("hand", InteractionHand.values());
        VerboseTags.register("block", List.of(VerboseSchema.TypeTag.ZZ),
                (in, ctx, out, fmt) -> out.append(blockName(ctx.clientVersionPvn(), in.rzz())));
        VerboseTags.register("item", List.of(VerboseSchema.TypeTag.ZZ),
                (in, ctx, out, fmt) -> out.append(itemTypeName(ctx.clientVersionPvn(), in.rzz())));
        VerboseTags.register("packet", List.of(VerboseSchema.TypeTag.ZZ),
                (in, ctx, out, fmt) -> out.append(packetName(ctx.clientVersionPvn(), in.rzz())));
        VerboseTags.register("entity", List.of(VerboseSchema.TypeTag.VI),
                (in, ctx, out, fmt) -> out.append(entityTypeName(ctx.clientVersionPvn(), in.rvi())));
        VerboseTags.register("offset", List.of(VerboseSchema.TypeTag.F64),
                (in, ctx, out, fmt) -> out.append(OffsetHandler.humanFormattedOffset(in.rf64())));
        VerboseTags.register("stdnum", List.of(VerboseSchema.TypeTag.F64),
                (in, ctx, out, fmt) -> out.append(formatNumberStandard(in.rf64())));
    }

    private VerboseCodecs() {
    }

    /**
     * Force tag registration. Call before templates parse and before history
     * rows render; class initialization performs the actual work once.
     */
    public static void ensureRegistered() {
        // Static initializer has run by the time this returns.
    }

    /** Null-safe enum encoding for the {@code registerEnum} family of tags. */
    public static int enumId(@Nullable Enum<?> value) {
        return VerboseTags.enumId(value);
    }

    /**
     * Encoder for {@code {block}}: the protocol-defined per-version block id,
     * so any product following the MC protocol decodes the same name.
     * {@code -1} when the block has no id in the player's version
     * (server-only states render as {@code unknown}).
     */
    public static int block(@NotNull StateType type, @NotNull ClientVersion version) {
        try {
            StateType.Mapped mapped = type.getMapped();
            return mapped == null ? -1 : mapped.getId(version);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** Encoder for {@code {item}}. */
    public static int item(@NotNull ItemType type, @NotNull ClientVersion version) {
        return type.getId(version);
    }

    /** Encoder for {@code {packet}}; see {@link #PACKET_NONE} / {@link #PACKET_TRANSACTION}. */
    public static int packet(@NotNull PacketTypeCommon type, @NotNull ClientVersion version) {
        return type.getId(version);
    }

    /** Encoder for {@code {entity}}. */
    public static int entity(@NotNull EntityType type, @NotNull ClientVersion version) {
        return type.getId(version);
    }

    private static @NotNull String blockName(int clientVersionPvn, int id) {
        if (id < 0) return "unknown";
        StateType type = StateTypes.getById(ClientVersion.getById(clientVersionPvn), id);
        return type == null ? "unknown(" + id + ")" : type.getName();
    }

    private static @NotNull String itemTypeName(int clientVersionPvn, int id) {
        if (id < 0) return "";
        ItemType type = ItemTypes.getById(ClientVersion.getById(clientVersionPvn), id);
        return type == null ? "unknown(" + id + ")" : type.getName().getKey();
    }

    private static @NotNull String packetName(int clientVersionPvn, int packetId) {
        if (packetId == PACKET_NONE) return "";
        if (packetId == PACKET_TRANSACTION) return "TRANSACTION";
        PacketTypeCommon type = PacketType.Play.Client.getById(ClientVersion.getById(clientVersionPvn), packetId);
        return type == null ? "unknown(" + packetId + ")" : type.getName();
    }

    private static @NotNull String entityTypeName(int clientVersionPvn, int entityId) {
        EntityType entityType = EntityTypes.getById(ClientVersion.getById(clientVersionPvn), entityId);
        return entityType == null ? "unknown" : entityType.getName().getKey();
    }

    private static @NotNull String formatNumberStandard(double value) {
        double abs = Math.abs(value);
        if (abs < 1e-7) return "0";
        String formatted;
        if (abs < 0.001) {
            formatted = String.format("%.4E", value);
            return formatted.replace("E-0", "E-");
        }
        formatted = String.format("%6f", value);
        return formatted.replace("0.", ".");
    }

}
