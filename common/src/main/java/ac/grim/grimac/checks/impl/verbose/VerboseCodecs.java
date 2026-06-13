package ac.grim.grimac.checks.impl.verbose;

import ac.grim.grimac.api.storage.verbose.VerboseBuf;
import ac.grim.grimac.api.storage.verbose.VerboseRenderContext;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VerboseCodecs {
    public static final int PACKET_NONE = Integer.MIN_VALUE;
    public static final int PACKET_TRANSACTION = Integer.MIN_VALUE + 1;

    private static final int MC_XZ_BITS = 26;
    private static final int MC_XZ_MASK = (1 << MC_XZ_BITS) - 1;
    private static final List<StateType> STATE_TYPE_VALUES = List.copyOf(StateTypes.values());
    private static final Map<StateType, Integer> STATE_TYPE_ORDINALS = stateTypeOrdinals();

    private VerboseCodecs() {
    }

    public static int enumOrdinal(@Nullable Enum<?> value) {
        return value == null ? 0 : value.ordinal() + 1;
    }

    public static @NotNull VerboseBuf enumOrdinal(@NotNull VerboseBuf out, @Nullable Enum<?> value) {
        return out.vi(enumOrdinal(value));
    }

    public static @NotNull String enumName(int encoded, Enum<?> @NotNull [] values) {
        if (encoded == 0) return "null";
        int ordinal = encoded - 1;
        return ordinal >= 0 && ordinal < values.length ? values[ordinal].name() : "unknown(" + encoded + ")";
    }

    public static @NotNull String lowerEnumName(int encoded, Enum<?> @NotNull [] values) {
        return enumName(encoded, values).toLowerCase(Locale.ROOT);
    }

    public static @NotNull VerboseBuf mcBlockPos(@NotNull VerboseBuf out, @NotNull Vector3i pos) {
        return out.vl(packMcBlockXZ(pos.x, pos.z)).zz(pos.y);
    }

    public static @NotNull VerboseBuf nullableMcBlockPos(@NotNull VerboseBuf out, @Nullable Vector3i pos) {
        if (pos == null) {
            return out.bool(false).vl(0L).zz(0);
        }
        return out.bool(true).vl(packMcBlockXZ(pos.x, pos.z)).zz(pos.y);
    }

    public static @NotNull String rMcBlockPos(@NotNull VerboseBuf in) {
        long xz = in.rvl();
        int y = in.rzz();
        return formatMcBlockPos(unpackMcBlockX(xz), y, unpackMcBlockZ(xz));
    }

    public static @NotNull String rNullableMcBlockPos(@NotNull VerboseBuf in) {
        boolean present = in.rbool();
        long xz = in.rvl();
        int y = in.rzz();
        return present ? formatMcBlockPos(unpackMcBlockX(xz), y, unpackMcBlockZ(xz)) : "null";
    }

    public static @NotNull VerboseBuf cursor3f(@NotNull VerboseBuf out, @NotNull Vector3f cursor) {
        return out.f32(cursor.x).f32(cursor.y).f32(cursor.z);
    }

    public static @NotNull String rCursor3f(@NotNull VerboseBuf in) {
        return formatVector3f(in.rf32(), in.rf32(), in.rf32());
    }

    public static @NotNull String rCursor3fObject(@NotNull VerboseBuf in) {
        return formatVector3fObject(in.rf32(), in.rf32(), in.rf32());
    }

    public static int stateTypeOrdinal(@NotNull StateType type) {
        Integer ordinal = STATE_TYPE_ORDINALS.get(type);
        return ordinal == null ? 0 : ordinal + 1;
    }

    public static @NotNull String stateTypeName(int encoded) {
        if (encoded == 0) return "unknown(0)";
        int ordinal = encoded - 1;
        return ordinal >= 0 && ordinal < STATE_TYPE_VALUES.size()
                ? STATE_TYPE_VALUES.get(ordinal).getName()
                : "unknown(" + encoded + ")";
    }

    public static int itemTypeId(@NotNull ItemType type, @NotNull ClientVersion version) {
        return type.getId(version);
    }

    public static @NotNull String itemTypeName(@NotNull VerboseRenderContext ctx, int id) {
        if (id < 0) return "";
        ItemType type = ItemTypes.getById(ClientVersion.getById(ctx.clientVersionPvn()), id);
        return type == null ? "unknown(" + id + ")" : type.getName().getKey();
    }

    public static @NotNull VerboseBuf signedLong(@NotNull VerboseBuf out, long value) {
        return out.zz((int) (value >> 32)).zz((int) value);
    }

    public static long rSignedLong(@NotNull VerboseBuf in) {
        int high = in.rzz();
        int low = in.rzz();
        return ((long) high << 32) | (low & 0xFFFF_FFFFL);
    }

    public static int packetId(@NotNull PacketTypeCommon type, @NotNull ClientVersion version) {
        return type.getId(version);
    }

    public static @NotNull String packetName(@NotNull VerboseRenderContext ctx, int packetId) {
        if (packetId == PACKET_NONE) return "";
        if (packetId == PACKET_TRANSACTION) return "TRANSACTION";
        PacketTypeCommon type = PacketType.Play.Client.getById(ClientVersion.getById(ctx.clientVersionPvn()), packetId);
        return type == null ? "unknown(" + packetId + ")" : type.getName();
    }

    private static long packMcBlockXZ(int x, int z) {
        return ((long) (x & MC_XZ_MASK) << MC_XZ_BITS) | (z & MC_XZ_MASK);
    }

    private static int unpackMcBlockX(long xz) {
        return signExtend26((int) (xz >>> MC_XZ_BITS));
    }

    private static int unpackMcBlockZ(long xz) {
        return signExtend26((int) xz & MC_XZ_MASK);
    }

    private static int signExtend26(int value) {
        return (value << (Integer.SIZE - MC_XZ_BITS)) >> (Integer.SIZE - MC_XZ_BITS);
    }

    private static @NotNull Map<StateType, Integer> stateTypeOrdinals() {
        IdentityHashMap<StateType, Integer> ordinals = new IdentityHashMap<>(STATE_TYPE_VALUES.size());
        for (int i = 0; i < STATE_TYPE_VALUES.size(); i++) {
            ordinals.put(STATE_TYPE_VALUES.get(i), i);
        }
        return ordinals;
    }

    private static @NotNull String formatMcBlockPos(int x, int y, int z) {
        return x + ", " + y + ", " + z;
    }

    private static @NotNull String formatVector3f(float x, float y, float z) {
        return x + ", " + y + ", " + z;
    }

    private static @NotNull String formatVector3fObject(float x, float y, float z) {
        return "X: " + x + ", Y: " + y + ", Z: " + z;
    }
}
