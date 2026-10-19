package ac.grim.grimac.utils.message;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageNbtEncoderTest {
    private static NBTCompound encode(Component component, ClientVersion version) {
        return (NBTCompound) new MessageNbtEncoder(PacketWrapper.createDummyWrapper(version), false).encode(component);
    }

    @Test
    void translationArgumentsKeepNumbersAndRenderBooleansAsWords() {
        Component component = Component.translatable().key("test").arguments(
                TranslationArgument.numeric(42), TranslationArgument.bool(true)).build();
        var args = com.github.retrooper.packetevents.protocol.util.NbtCodecs.GENERIC_LIST.decode(
                encode(component, ClientVersion.V_1_21_6).getTagOrNull("with"), null);
        assertEquals(42, ((NBTInt) args.get(0)).getAsInt());
        assertEquals("true", ((NBTString) args.get(1)).getValue());
    }

    @Test
    void jsonItemDataDoesNotTruncateDecimalsOrLongs() {
        var data = GsonDataComponentValue.gsonDataComponentValue(new com.google.gson.Gson().fromJson(
                "{\"decimal\":1.5,\"large\":9223372036854775807}", com.google.gson.JsonElement.class));
        var input = Component.text("item").hoverEvent(HoverEvent.showItem(Key.key("minecraft:stone"), 1,
                Map.of(Key.key("minecraft:custom_data"), data)));
        var payload = encode(input, ClientVersion.V_1_21_6).getCompoundTagOrThrow("hover_event")
                .getCompoundTagOrThrow("components").getCompoundTagOrThrow("minecraft:custom_data");
        assertEquals(1.5, ((NBTDouble) payload.getTagOrNull("decimal")).getAsDouble());
        assertEquals(Long.MAX_VALUE, ((NBTLong) payload.getTagOrNull("large")).getAsLong());
    }

    @Test
    void eventSchemaChangesAt1215() {
        Component message = Component.text("click").clickEvent(ClickEvent.runCommand("/grim alerts"))
                .hoverEvent(Component.text("hover"));
        NBTCompound old = encode(message, ClientVersion.V_1_21_4);
        NBTCompound current = encode(message, ClientVersion.V_1_21_5);
        assertEquals("/grim alerts", old.getCompoundTagOrThrow("clickEvent").getStringTagValueOrThrow("value"));
        assertEquals("/grim alerts", current.getCompoundTagOrThrow("click_event").getStringTagValueOrThrow("command"));
        assertEquals("hover", old.getCompoundTagOrThrow("hoverEvent").getStringTagValueOrThrow("contents"));
        assertEquals("hover", current.getCompoundTagOrThrow("hover_event").getStringTagValueOrThrow("value"));
    }

    @Test
    void typedCustomPayloadSurvivesWithoutJsonConversion() {
        Component message = Component.text("custom").clickEvent(ClickEvent.custom(Key.key("grim:test"),
                BinaryTagHolder.binaryTagHolder("{b:1b,s:2s,i:3,l:9223372036854775807L,f:1.25f,d:2.5d,ba:[B;1b],ia:[I;2],la:[L;3L],list:[{x:1},{x:2}]}")));
        NBTCompound payload = encode(message, ClientVersion.V_1_21_6).getCompoundTagOrThrow("click_event").getCompoundTagOrThrow("payload");
        assertInstanceOf(NBTByte.class, payload.getTagOrNull("b"));
        assertInstanceOf(NBTShort.class, payload.getTagOrNull("s"));
        assertEquals(Long.MAX_VALUE, ((NBTLong) payload.getTagOrNull("l")).getAsLong());
        assertEquals(1.25f, ((NBTFloat) payload.getTagOrNull("f")).getAsFloat());
        assertInstanceOf(NBTDouble.class, payload.getTagOrNull("d"));
        assertInstanceOf(NBTByteArray.class, payload.getTagOrNull("ba"));
        assertInstanceOf(NBTIntArray.class, payload.getTagOrNull("ia"));
        assertInstanceOf(NBTLongArray.class, payload.getTagOrNull("la"));
        assertInstanceOf(NBTList.class, payload.getTagOrNull("list"));
    }

    @Test
    void itemDataAndEntityHoverArePreserved() {
        DataComponentValue.TagSerializable data = () -> BinaryTagHolder.binaryTagHolder("{decimal:1.5d,long:9223372036854775807L}");
        Component item = Component.text("item").hoverEvent(HoverEvent.showItem(Key.key("minecraft:stone"), 2,
                Map.of(Key.key("minecraft:custom_data"), data, Key.key("minecraft:custom_name"), DataComponentValue.removed())));
        NBTCompound hover = encode(item, ClientVersion.V_1_21_6).getCompoundTagOrThrow("hover_event");
        NBTCompound components = hover.getCompoundTagOrThrow("components");
        assertEquals(1.5, ((NBTDouble) components.getCompoundTagOrThrow("minecraft:custom_data").getTagOrNull("decimal")).getAsDouble());
        assertNotNull(components.getTagOrNull("!minecraft:custom_name"));
        Component entity = Component.text("entity").hoverEvent(HoverEvent.showEntity(Key.key("minecraft:pig"), new UUID(1, 2), Component.text("Pig")));
        hover = encode(entity, ClientVersion.V_1_21_6).getCompoundTagOrThrow("hover_event");
        assertEquals("minecraft:pig", hover.getStringTagValueOrThrow("id"));
        assertArrayEquals(new int[]{0, 1, 0, 2}, ((NBTIntArray) hover.getTagOrNull("uuid")).getValue());
    }

    @Test
    void legacyJsonHasLegacyHoversAndDownsampledColors() {
        Component item = Component.text("item").color(TextColor.color(0x12abef))
                .hoverEvent(HoverEvent.showItem(Key.key("minecraft:stone"), 2));
        var legacy = KyoriMessageSerializer.forDataVersion(0).serializeToTree(item).getAsJsonObject();
        assertTrue(legacy.getAsJsonObject("hoverEvent").has("value"));
        assertFalse(legacy.get("color").getAsString().startsWith("#"));
        var modern = KyoriMessageSerializer.forDataVersion(5000).serializeToTree(item).getAsJsonObject();
        assertTrue(modern.has("hover_event"));
        assertEquals("#12abef", modern.get("color").getAsString().toLowerCase(java.util.Locale.ROOT));
    }

    @Test
    void packetTrailersMatchProtocolBoundaries() {
        for (ServerVersion version : new ServerVersion[]{ServerVersion.V_1_8, ServerVersion.V_1_16,
                ServerVersion.V_1_19, ServerVersion.V_1_19_1, ServerVersion.V_1_20_3, ServerVersion.getLatest()}) {
            var buffer = Unpooled.buffer();
            try {
                SystemMessagePacket.writeTrailer(buffer, version);
                if (version.isNewerThanOrEquals(ServerVersion.V_1_19_1)) assertFalse(buffer.readBoolean());
                else {
                    assertEquals(1, buffer.readByte());
                    if (version.isNewerThanOrEquals(ServerVersion.V_1_16) && version.isOlderThan(ServerVersion.V_1_19)) {
                        assertEquals(0, buffer.readLong());
                        assertEquals(0, buffer.readLong());
                    }
                }
                assertFalse(buffer.isReadable());
            } finally {
                buffer.release();
            }
        }
    }
}
